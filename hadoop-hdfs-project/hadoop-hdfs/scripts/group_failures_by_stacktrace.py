#!/usr/bin/env python3
"""
Script to group test failures and errors by their normalized stack traces.

This script analyzes test logs and groups failures/errors that have the same
root cause by normalizing their stack traces (removing line numbers and
test-specific frames).
"""

import os
import re
import sys
from collections import defaultdict


def remove_ansi_codes(text):
    """Remove ANSI escape codes from text."""
    ansi_escape = re.compile(r'\x1B\[[0-9;]*[a-zA-Z]')
    ansi_simple = re.compile(r'\x1B\[m')
    text = ansi_escape.sub('', text)
    text = ansi_simple.sub('', text)
    return text


def remove_line_numbers(line):
    """
    Remove line numbers from stack trace lines.

    Examples:
        '(DFSInputStream.java:414)' -> '(DFSInputStream.java)'
        'at Class.method(File.java:123)' -> 'at Class.method(File.java)'
    """
    # Remove :number) patterns like (File.java:123)
    line = re.sub(r':(\d+)\)', ')', line)
    # Remove :number from other contexts
    line = re.sub(r':(\d+)\s*$', '', line)
    return line


def normalize_stacktrace(stacktrace_lines, test_class_name):
    """
    Normalize a stack trace by:
    1. Removing ANSI codes
    2. Removing line numbers
    3. Removing variable data (IDs, addresses, ports, etc.)
    4. Truncating at the test method frame

    Args:
        stacktrace_lines: List of stack trace lines
        test_class_name: Name of the test class (e.g., 'TestBlockRecovery2_RestartInjected')

    Returns:
        Normalized stack trace as a string
    """
    normalized = []

    for line in stacktrace_lines:
        # Remove ANSI codes first
        clean_line = remove_ansi_codes(line)

        # Check if this line contains the test class name in a stack frame
        # This indicates we've reached the test method in the stack trace
        if '\tat ' in clean_line and test_class_name in clean_line:
            # Stop here - don't include this line or anything after
            break

        # Skip empty lines at the start
        if not normalized and not clean_line.strip():
            continue

        # Remove line numbers
        clean_line = remove_line_numbers(clean_line)

        # Remove dynamic IDs, addresses, and other variable data from exception messages
        # Examples:
        # - BP-1772494331-127.0.1.1-1764570987883 (block pool IDs)
        # - blk_1073741825_1001 (block IDs)
        # - 127.0.0.1:43191 (IP addresses and ports)
        # - DS-b51f4220-85f2-4f7f-9a0f-7aa9c17f8f54 (datanode storage IDs)

        # Remove block pool IDs
        clean_line = re.sub(r'BP-\d+-[\d.:]+-\d+', 'BP-***', clean_line)

        # Remove block IDs
        clean_line = re.sub(r'blk_\d+_\d+', 'blk_***', clean_line)

        # Remove IP:port combinations
        clean_line = re.sub(r'\d+\.\d+\.\d+\.\d+:\d+', '***.***.***:***', clean_line)

        # Remove datanode storage IDs
        clean_line = re.sub(r'DS-[0-9a-f-]+', 'DS-***', clean_line)

        # Remove UUIDs in general
        clean_line = re.sub(r'[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}',
                           '***-***-***-***-***', clean_line)

        # Remove standalone large numbers that might be timestamps or IDs
        clean_line = re.sub(r'\b\d{10,}\b', '***', clean_line)

        normalized.append(clean_line.strip())

    # Remove trailing empty lines
    while normalized and not normalized[-1]:
        normalized.pop()

    return '\n'.join(normalized)


def extract_failures_errors(log_file_path):
    """
    Extract failure and error information from a test log file.

    Returns:
        list: List of dicts containing failure/error information
    """
    test_class_name = os.path.basename(log_file_path).replace('.log', '')

    failures_errors = []

    try:
        with open(log_file_path, 'r', encoding='utf-8', errors='ignore') as f:
            lines = f.readlines()

        i = 0
        while i < len(lines):
            line = lines[i]
            clean_line = remove_ansi_codes(line)

            # Look for ERROR or FAILURE markers for individual tests
            # Format: [ERROR] testMethodName(...) Time elapsed: X s  <<< ERROR!
            # or:     [ERROR] testMethodName(...) Time elapsed: X s  <<< FAILURE!
            if ('<<< ERROR!' in clean_line or '<<< FAILURE!' in clean_line) and '\t' not in clean_line:
                # Extract test method name
                test_method_match = re.search(r'(\w+)\([^)]*\)\s+Time elapsed', clean_line)
                test_method = test_method_match.group(1) if test_method_match else 'Unknown'

                error_type = 'ERROR' if '<<< ERROR!' in clean_line else 'FAILURE'

                # Collect the stack trace
                stacktrace = []
                i += 1

                # Read until we hit another test result line or the end of stack trace
                while i < len(lines):
                    next_line = lines[i]
                    clean_next = remove_ansi_codes(next_line)

                    # Stop if we hit another ERROR/INFO marker that's not part of the stack
                    if (clean_next.strip().startswith('[ERROR]') or
                        clean_next.strip().startswith('[INFO]')):
                        # Check if it's a new test result or just part of stack
                        if '<<< ERROR!' in clean_next or '<<< FAILURE!' in clean_next or 'Results:' in clean_next:
                            break
                        # If it's an error summary line (short), also break
                        if '[ERROR]' in clean_next and 'Tests run:' not in clean_next and len(clean_next.strip()) < 100:
                            # Check if this looks like a summary line
                            if re.match(r'\[ERROR\]\s+\w+\.\w+:\d+', clean_next.strip()):
                                break

                    # Stop at blank lines that indicate end of stack trace
                    if not clean_next.strip():
                        # Peek ahead to see if there's more stack trace
                        if i + 1 < len(lines):
                            peek = remove_ansi_codes(lines[i + 1]).strip()
                            if peek and (peek.startswith('at ') or
                                       peek.startswith('Caused by:') or
                                       peek.startswith('...')):
                                # Continue collecting
                                stacktrace.append(next_line.rstrip())
                                i += 1
                                continue
                        # Otherwise, end of stack trace
                        break

                    stacktrace.append(next_line.rstrip())
                    i += 1

                if stacktrace:
                    failures_errors.append({
                        'test_class': test_class_name,
                        'test_method': test_method,
                        'type': error_type,
                        'stacktrace': stacktrace
                    })

                continue

            i += 1

    except Exception as e:
        print(f"Error parsing {log_file_path}: {e}", file=sys.stderr)
        return []

    return failures_errors


def analyze_failures_errors(log_dir):
    """
    Analyze all failures and errors in the log directory.

    Returns:
        tuple: (grouped_failures, grouped_errors)
    """
    # Get all .log files
    log_files = [
        f for f in os.listdir(log_dir)
        if f.endswith('.log') and f not in ['summary.txt', 'test_list.txt']
    ]

    # Group by normalized stack trace
    grouped_failures = defaultdict(list)
    grouped_errors = defaultdict(list)

    total_failures = 0
    total_errors = 0

    for log_file in sorted(log_files):
        log_path = os.path.join(log_dir, log_file)
        failures_errors = extract_failures_errors(log_path)

        for item in failures_errors:
            # Normalize the stack trace
            normalized = normalize_stacktrace(item['stacktrace'], item['test_class'])

            test_info = {
                'test_class': item['test_class'],
                'test_method': item['test_method']
            }

            if item['type'] == 'FAILURE':
                grouped_failures[normalized].append(test_info)
                total_failures += 1
            else:  # ERROR
                grouped_errors[normalized].append(test_info)
                total_errors += 1

    return grouped_failures, grouped_errors, total_failures, total_errors


def print_grouped_results(grouped_failures, grouped_errors, total_failures, total_errors):
    """Print the grouped results."""

    print("=" * 80)
    print("FAILURES GROUPED BY ROOT CAUSE")
    print("=" * 80)
    print()
    print(f"Total failures: {total_failures}")
    print(f"Unique failure signatures: {len(grouped_failures)}")
    print()

    if not grouped_failures:
        print("No failures found.")
    else:
        for idx, (stacktrace, tests) in enumerate(
            sorted(grouped_failures.items(), key=lambda x: len(x[1]), reverse=True), 1):
            print(f"Failure Group #{idx} - {len(tests)} occurrence(s)")
            print("-" * 80)
            print("Affected tests:")
            for test in sorted(tests, key=lambda x: (x['test_class'], x['test_method'])):
                print(f"  - {test['test_class']}.{test['test_method']}")
            print()
            print("Normalized stack trace:")
            if stacktrace:
                for line in stacktrace.split('\n')[:30]:  # Limit to first 30 lines
                    print(f"  {line}")
                if stacktrace.count('\n') > 30:
                    print(f"  ... ({stacktrace.count('\n') - 30} more lines)")
            else:
                print("  (Empty stack trace)")
            print()
            print("=" * 80)
            print()

    print()
    print("=" * 80)
    print("ERRORS GROUPED BY ROOT CAUSE")
    print("=" * 80)
    print()
    print(f"Total errors: {total_errors}")
    print(f"Unique error signatures: {len(grouped_errors)}")
    print()

    if not grouped_errors:
        print("No errors found.")
    else:
        for idx, (stacktrace, tests) in enumerate(
            sorted(grouped_errors.items(), key=lambda x: len(x[1]), reverse=True), 1):
            print(f"Error Group #{idx} - {len(tests)} occurrence(s)")
            print("-" * 80)
            print("Affected tests:")
            for test in sorted(tests, key=lambda x: (x['test_class'], x['test_method'])):
                print(f"  - {test['test_class']}.{test['test_method']}")
            print()
            print("Normalized stack trace:")
            if stacktrace:
                for line in stacktrace.split('\n')[:30]:  # Limit to first 30 lines
                    print(f"  {line}")
                if stacktrace.count('\n') > 30:
                    print(f"  ... ({stacktrace.count('\n') - 30} more lines)")
            else:
                print("  (Empty stack trace)")
            print()
            print("=" * 80)
            print()


def save_grouped_report(output_file, grouped_failures, grouped_errors, total_failures, total_errors):
    """Save the grouped results to a file."""

    with open(output_file, 'w') as f:
        f.write("=" * 80 + "\n")
        f.write("FAILURES GROUPED BY ROOT CAUSE\n")
        f.write("=" * 80 + "\n\n")
        f.write(f"Total failures: {total_failures}\n")
        f.write(f"Unique failure signatures: {len(grouped_failures)}\n\n")

        if not grouped_failures:
            f.write("No failures found.\n")
        else:
            for idx, (stacktrace, tests) in enumerate(
                sorted(grouped_failures.items(), key=lambda x: len(x[1]), reverse=True), 1):
                f.write(f"Failure Group #{idx} - {len(tests)} occurrence(s)\n")
                f.write("-" * 80 + "\n")
                f.write("Affected tests:\n")
                for test in sorted(tests, key=lambda x: (x['test_class'], x['test_method'])):
                    f.write(f"  - {test['test_class']}.{test['test_method']}\n")
                f.write("\n")
                f.write("Normalized stack trace:\n")
                f.write(stacktrace if stacktrace else "(Empty stack trace)")
                f.write("\n\n")
                f.write("=" * 80 + "\n\n")

        f.write("\n")
        f.write("=" * 80 + "\n")
        f.write("ERRORS GROUPED BY ROOT CAUSE\n")
        f.write("=" * 80 + "\n\n")
        f.write(f"Total errors: {total_errors}\n")
        f.write(f"Unique error signatures: {len(grouped_errors)}\n\n")

        if not grouped_errors:
            f.write("No errors found.\n")
        else:
            for idx, (stacktrace, tests) in enumerate(
                sorted(grouped_errors.items(), key=lambda x: len(x[1]), reverse=True), 1):
                f.write(f"Error Group #{idx} - {len(tests)} occurrence(s)\n")
                f.write("-" * 80 + "\n")
                f.write("Affected tests:\n")
                for test in sorted(tests, key=lambda x: (x['test_class'], x['test_method'])):
                    f.write(f"  - {test['test_class']}.{test['test_method']}\n")
                f.write("\n")
                f.write("Normalized stack trace:\n")
                f.write(stacktrace if stacktrace else "(Empty stack trace)")
                f.write("\n\n")
                f.write("=" * 80 + "\n\n")


def main():
    if len(sys.argv) < 2:
        print("Usage: python group_failures_by_stacktrace.py <log_directory> [--save]")
        sys.exit(1)

    log_dir = sys.argv[1]
    save_report = '--save' in sys.argv

    if not os.path.isdir(log_dir):
        print(f"Error: {log_dir} is not a valid directory")
        sys.exit(1)

    print(f"Analyzing failures and errors in: {log_dir}")
    print("Extracting and normalizing stack traces...")
    print()

    grouped_failures, grouped_errors, total_failures, total_errors = analyze_failures_errors(log_dir)

    print_grouped_results(grouped_failures, grouped_errors, total_failures, total_errors)

    if save_report:
        output_file = os.path.join(log_dir, 'grouped_failures_errors_report.txt')
        save_grouped_report(output_file, grouped_failures, grouped_errors, total_failures, total_errors)
        print(f"Detailed report saved to: {output_file}")


if __name__ == '__main__':
    main()
