#!/usr/bin/env python3
"""
Script to analyze Maven Surefire test logs and generate statistics.
"""

import os
import re
import sys
from collections import defaultdict


def remove_ansi_codes(text):
    """Remove ANSI escape codes from text."""
    # Pattern to match ANSI escape sequences
    # This handles ESC[ followed by any number of parameter bytes and a final byte
    ansi_escape = re.compile(r'\x1B\[[0-9;]*[a-zA-Z]')
    # Also match simpler format
    ansi_simple = re.compile(r'\x1B\[m')
    text = ansi_escape.sub('', text)
    text = ansi_simple.sub('', text)
    return text


def parse_test_log(log_file_path):
    """
    Parse a single test log file and extract test results.

    Returns:
        dict: Dictionary with test statistics or None if parsing failed
    """
    test_name = os.path.basename(log_file_path).replace('.log', '')

    # Pattern to match test result lines
    # Matches both [INFO] and [ERROR] lines with test counts
    pattern = r'\[(INFO|ERROR)\]\s+Tests run:\s+(\d+),\s+Failures:\s+(\d+),\s+Errors:\s+(\d+),\s+Skipped:\s+(\d+)'

    results = None

    try:
        with open(log_file_path, 'r', encoding='utf-8', errors='ignore') as f:
            in_results_section = False
            for line in f:
                # Remove ANSI color codes
                clean_line = remove_ansi_codes(line)

                # Look for the "Results:" section which has the final summary
                if 'Results:' in clean_line:
                    in_results_section = True
                    continue

                # If we're in the results section, capture the first match
                if in_results_section:
                    match = re.search(pattern, clean_line)
                    if match:
                        level, tests_run, failures, errors, skipped = match.groups()
                        results = {
                            'test_name': test_name,
                            'tests_run': int(tests_run),
                            'failures': int(failures),
                            'errors': int(errors),
                            'skipped': int(skipped),
                            'status': level  # INFO or ERROR
                        }
                        break

            # If we didn't find results in the Results section, try to find any match
            if results is None:
                f.seek(0)
                matches = []
                for line in f:
                    clean_line = remove_ansi_codes(line)
                    match = re.search(pattern, clean_line)
                    if match and 'Time elapsed' in clean_line:
                        level, tests_run, failures, errors, skipped = match.groups()
                        matches.append({
                            'test_name': test_name,
                            'tests_run': int(tests_run),
                            'failures': int(failures),
                            'errors': int(errors),
                            'skipped': int(skipped),
                            'status': level
                        })

                # Take the last match (usually the most complete one)
                if matches:
                    results = matches[-1]

    except Exception as e:
        print(f"Error parsing {log_file_path}: {e}", file=sys.stderr)
        return None

    return results


def analyze_test_logs(log_dir):
    """
    Analyze all test logs in the given directory.

    Returns:
        tuple: (aggregated_stats, test_details)
    """
    aggregated = {
        'total_tests': 0,
        'total_failures': 0,
        'total_errors': 0,
        'total_skipped': 0,
        'total_passed': 0,
        'test_classes': 0
    }

    test_details = []
    failed_tests = []
    error_tests = []
    skipped_tests = []

    # Get all .log files except summary.txt
    log_files = [
        f for f in os.listdir(log_dir)
        if f.endswith('.log') and f not in ['summary.txt', 'test_list.txt']
    ]

    for log_file in sorted(log_files):
        log_path = os.path.join(log_dir, log_file)
        result = parse_test_log(log_path)

        if result:
            aggregated['total_tests'] += result['tests_run']
            aggregated['total_failures'] += result['failures']
            aggregated['total_errors'] += result['errors']
            aggregated['total_skipped'] += result['skipped']
            aggregated['test_classes'] += 1

            # Calculate passed tests for this class
            passed = result['tests_run'] - result['failures'] - result['errors'] - result['skipped']
            result['passed'] = passed
            aggregated['total_passed'] += passed

            test_details.append(result)

            # Track problematic tests
            if result['failures'] > 0:
                failed_tests.append(result)
            if result['errors'] > 0:
                error_tests.append(result)
            if result['skipped'] > 0:
                skipped_tests.append(result)

    return aggregated, test_details, failed_tests, error_tests, skipped_tests


def print_summary(aggregated, test_details, failed_tests, error_tests, skipped_tests):
    """Print a formatted summary of the test results."""

    print("=" * 80)
    print("TEST RESULTS SUMMARY")
    print("=" * 80)
    print()

    print(f"Total Test Classes:  {aggregated['test_classes']}")
    print(f"Total Test Methods:  {aggregated['total_tests']}")
    print()

    if aggregated['total_tests'] == 0:
        print("No test results found!")
        return

    print(f"  Passed:   {aggregated['total_passed']:4d} ({aggregated['total_passed']/aggregated['total_tests']*100:.1f}%)")
    print(f"  Failed:   {aggregated['total_failures']:4d} ({aggregated['total_failures']/aggregated['total_tests']*100:.1f}%)")
    print(f"  Errors:   {aggregated['total_errors']:4d} ({aggregated['total_errors']/aggregated['total_tests']*100:.1f}%)")
    print(f"  Skipped:  {aggregated['total_skipped']:4d} ({aggregated['total_skipped']/aggregated['total_tests']*100:.1f}%)")
    print()

    # Print failed tests if any
    if failed_tests:
        print("=" * 80)
        print(f"FAILED TESTS ({len(failed_tests)} test classes)")
        print("=" * 80)
        for test in failed_tests:
            print(f"  {test['test_name']}")
            print(f"    Tests: {test['tests_run']}, Failures: {test['failures']}")
        print()

    # Print error tests if any
    if error_tests:
        print("=" * 80)
        print(f"ERROR TESTS ({len(error_tests)} test classes)")
        print("=" * 80)
        for test in error_tests:
            print(f"  {test['test_name']}")
            print(f"    Tests: {test['tests_run']}, Errors: {test['errors']}")
        print()

    # Print skipped tests summary
    if skipped_tests:
        print("=" * 80)
        print(f"SKIPPED TESTS ({len(skipped_tests)} test classes with skipped tests)")
        print("=" * 80)
        for test in skipped_tests:
            print(f"  {test['test_name']}")
            print(f"    Tests: {test['tests_run']}, Skipped: {test['skipped']}")
        print()


def save_detailed_report(output_file, aggregated, test_details, failed_tests, error_tests, skipped_tests):
    """Save a detailed report to a file."""

    with open(output_file, 'w') as f:
        f.write("=" * 80 + "\n")
        f.write("DETAILED TEST RESULTS REPORT\n")
        f.write("=" * 80 + "\n\n")

        f.write(f"Total Test Classes:  {aggregated['test_classes']}\n")
        f.write(f"Total Test Methods:  {aggregated['total_tests']}\n\n")

        f.write(f"  Passed:   {aggregated['total_passed']:4d} ({aggregated['total_passed']/aggregated['total_tests']*100:.1f}%)\n")
        f.write(f"  Failed:   {aggregated['total_failures']:4d} ({aggregated['total_failures']/aggregated['total_tests']*100:.1f}%)\n")
        f.write(f"  Errors:   {aggregated['total_errors']:4d} ({aggregated['total_errors']/aggregated['total_tests']*100:.1f}%)\n")
        f.write(f"  Skipped:  {aggregated['total_skipped']:4d} ({aggregated['total_skipped']/aggregated['total_tests']*100:.1f}%)\n\n")

        # Detailed results for all test classes
        f.write("=" * 80 + "\n")
        f.write("ALL TEST CLASSES\n")
        f.write("=" * 80 + "\n\n")

        for test in sorted(test_details, key=lambda x: x['test_name']):
            status = "PASS"
            if test['failures'] > 0:
                status = "FAIL"
            elif test['errors'] > 0:
                status = "ERROR"
            elif test['skipped'] == test['tests_run']:
                status = "SKIPPED"
            elif test['skipped'] > 0:
                status = "PARTIAL"

            f.write(f"[{status:7s}] {test['test_name']}\n")
            f.write(f"           Tests: {test['tests_run']}, Passed: {test['passed']}, "
                   f"Failed: {test['failures']}, Errors: {test['errors']}, Skipped: {test['skipped']}\n\n")


def main():
    if len(sys.argv) < 2:
        print("Usage: python analyze_test_results.py <log_directory> [--detailed]")
        sys.exit(1)

    log_dir = sys.argv[1]
    save_detailed = '--detailed' in sys.argv

    if not os.path.isdir(log_dir):
        print(f"Error: {log_dir} is not a valid directory")
        sys.exit(1)

    print(f"Analyzing test logs in: {log_dir}\n")

    aggregated, test_details, failed_tests, error_tests, skipped_tests = analyze_test_logs(log_dir)

    print_summary(aggregated, test_details, failed_tests, error_tests, skipped_tests)

    if save_detailed:
        output_file = os.path.join(log_dir, 'detailed_test_report.txt')
        save_detailed_report(output_file, aggregated, test_details, failed_tests, error_tests, skipped_tests)
        print(f"Detailed report saved to: {output_file}")


if __name__ == '__main__':
    main()
