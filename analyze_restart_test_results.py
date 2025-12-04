#!/usr/bin/env python3
"""
Analyze YARN restart test results from surefire XML reports.

This script:
1. Parses all surefire XML test reports
2. Extracts test method results (pass/fail/error)
3. Gathers all failures and errors
4. Groups failures by normalized stack traces
"""

import os
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path
import json
import re


class TestResult:
    """Represents a single test execution result."""

    def __init__(self, test_class, test_method, status, time, failure_msg=None,
                 failure_type=None, stack_trace=None, test_dir=None):
        self.test_class = test_class
        self.test_method = test_method
        self.status = status  # 'PASS', 'FAIL', 'ERROR', 'SKIPPED'
        self.time = time
        self.failure_msg = failure_msg
        self.failure_type = failure_type
        self.stack_trace = stack_trace
        self.test_dir = test_dir

        # Parse test directory for restart config
        self.position = None
        self.target = None
        self.mode = None
        if test_dir:
            self._parse_test_dir(test_dir)

    def _parse_test_dir(self, test_dir):
        """Extract restart configuration from test directory name."""
        # Format: NNN-TestClass_testMethod__position_X__target_Y__mode_Z_
        parts = test_dir.split('__')
        for part in parts:
            if part.startswith('position_'):
                self.position = part.replace('position_', '').rstrip('_')
            elif part.startswith('target_'):
                self.target = part.replace('target_', '').rstrip('_')
            elif part.startswith('mode_'):
                self.mode = part.replace('mode_', '').rstrip('_')

    def to_dict(self):
        """Convert to dictionary for JSON serialization."""
        return {
            'testClass': self.test_class,
            'testMethod': self.test_method,
            'status': self.status,
            'time': self.time,
            'position': self.position,
            'target': self.target,
            'mode': self.mode,
            'failureMessage': self.failure_msg,
            'failureType': self.failure_type,
            'stackTrace': self.stack_trace,
            'testDirectory': self.test_dir
        }


def normalize_stack_trace(stack_trace):
    """
    Normalize a stack trace for grouping similar failures.

    Rules:
    1. Remove all line numbers (e.g., File.java:123 -> File.java)
    2. Truncate after the first occurrence of the test method name
    """
    if not stack_trace:
        return ""

    lines = []
    for line in stack_trace.strip().split('\n'):
        # Remove line numbers from stack trace
        # Match patterns like .java:123) or .java:123
        line = re.sub(r'\.java:\d+\)', '.java)', line)
        line = re.sub(r'\.java:\d+', '.java', line)

        # Also remove line numbers from other file types
        line = re.sub(r'\.scala:\d+', '.scala', line)
        line = re.sub(r'\.groovy:\d+', '.groovy', line)

        lines.append(line)

    normalized = '\n'.join(lines)

    # Truncate after test method appears in stack trace
    # This removes framework-specific parts that are the same for all tests
    # Look for common test runner patterns
    truncation_patterns = [
        r'at org\.junit\.runners\.ParentRunner',
        r'at org\.junit\.runners\.BlockJUnit4ClassRunner',
        r'at org\.apache\.maven\.surefire',
        r'at sun\.reflect\.NativeMethodAccessorImpl\.invoke0\(Native Method\)',
    ]

    for pattern in truncation_patterns:
        match = re.search(pattern, normalized)
        if match:
            # Include the line where we found the pattern, then stop
            pos = match.start()
            # Find the end of this line
            next_newline = normalized.find('\n', pos)
            if next_newline != -1:
                normalized = normalized[:next_newline]
            break

    return normalized.strip()


def parse_surefire_xml(xml_file):
    """Parse a surefire XML report and extract test results."""
    try:
        tree = ET.parse(xml_file)
        root = tree.getroot()

        results = []

        # Get test directory from file path
        test_dir = xml_file.parent.parent.name

        for testcase in root.findall('.//testcase'):
            test_class = testcase.get('classname')
            test_method = testcase.get('name')
            time = float(testcase.get('time', 0))

            # Check for failure
            failure = testcase.find('failure')
            error = testcase.find('error')
            skipped = testcase.find('skipped')

            if failure is not None:
                status = 'FAIL'
                failure_msg = failure.get('message', '')
                failure_type = failure.get('type', '')
                stack_trace = failure.text or ''

                result = TestResult(
                    test_class=test_class,
                    test_method=test_method,
                    status=status,
                    time=time,
                    failure_msg=failure_msg,
                    failure_type=failure_type,
                    stack_trace=stack_trace,
                    test_dir=test_dir
                )
                results.append(result)

            elif error is not None:
                status = 'ERROR'
                failure_msg = error.get('message', '')
                failure_type = error.get('type', '')
                stack_trace = error.text or ''

                result = TestResult(
                    test_class=test_class,
                    test_method=test_method,
                    status=status,
                    time=time,
                    failure_msg=failure_msg,
                    failure_type=failure_type,
                    stack_trace=stack_trace,
                    test_dir=test_dir
                )
                results.append(result)

            elif skipped is not None:
                status = 'SKIPPED'
                result = TestResult(
                    test_class=test_class,
                    test_method=test_method,
                    status=status,
                    time=time,
                    test_dir=test_dir
                )
                results.append(result)

            else:
                status = 'PASS'
                result = TestResult(
                    test_class=test_class,
                    test_method=test_method,
                    status=status,
                    time=time,
                    test_dir=test_dir
                )
                results.append(result)

        return results

    except Exception as e:
        print(f"Error parsing {xml_file}: {e}", file=sys.stderr)
        return []


def analyze_test_results(test_output_dir):
    """Analyze all test results from the output directory."""
    output_path = Path(test_output_dir)

    if not output_path.exists():
        print(f"Error: Directory not found: {test_output_dir}", file=sys.stderr)
        return None

    all_results = []

    # Find all TEST-*.xml files
    xml_files = list(output_path.glob('**/TEST-*.xml'))

    print(f"Found {len(xml_files)} surefire XML reports")

    for xml_file in xml_files:
        results = parse_surefire_xml(xml_file)
        all_results.extend(results)

    print(f"Parsed {len(all_results)} test results")

    # Categorize results
    passed = [r for r in all_results if r.status == 'PASS']
    failed = [r for r in all_results if r.status == 'FAIL']
    errored = [r for r in all_results if r.status == 'ERROR']
    skipped = [r for r in all_results if r.status == 'SKIPPED']

    print(f"  PASS: {len(passed)}")
    print(f"  FAIL: {len(failed)}")
    print(f"  ERROR: {len(errored)}")
    print(f"  SKIPPED: {len(skipped)}")

    return {
        'all_results': all_results,
        'passed': passed,
        'failed': failed,
        'errored': errored,
        'skipped': skipped
    }


def group_failures_by_stacktrace(failures_and_errors):
    """Group failures and errors by normalized stack trace."""
    groups = defaultdict(list)

    for result in failures_and_errors:
        normalized = normalize_stack_trace(result.stack_trace)

        # Use normalized stack trace + failure type as key
        key = f"{result.failure_type}:::{normalized}"
        groups[key].append(result)

    return groups


def generate_report(analysis, output_file):
    """Generate a detailed report of test results."""

    with open(output_file, 'w') as f:
        f.write("=" * 80 + "\n")
        f.write("YARN RESTART TEST ANALYSIS REPORT\n")
        f.write("=" * 80 + "\n\n")

        # Summary
        total = len(analysis['all_results'])
        passed = len(analysis['passed'])
        failed = len(analysis['failed'])
        errored = len(analysis['errored'])
        skipped = len(analysis['skipped'])

        f.write("SUMMARY\n")
        f.write("-" * 80 + "\n")
        f.write(f"Total Tests:    {total}\n")
        f.write(f"Passed:         {passed} ({100*passed/total:.1f}%)\n")
        f.write(f"Failed:         {failed} ({100*failed/total if total > 0 else 0:.1f}%)\n")
        f.write(f"Errored:        {errored} ({100*errored/total if total > 0 else 0:.1f}%)\n")
        f.write(f"Skipped:        {skipped} ({100*skipped/total if total > 0 else 0:.1f}%)\n")
        f.write("\n")

        # Group failures by stack trace
        failures_and_errors = analysis['failed'] + analysis['errored']

        if not failures_and_errors:
            f.write("No failures or errors found!\n")
            return

        f.write("=" * 80 + "\n")
        f.write("FAILURES AND ERRORS GROUPED BY STACK TRACE\n")
        f.write("=" * 80 + "\n\n")

        groups = group_failures_by_stacktrace(failures_and_errors)

        f.write(f"Found {len(groups)} unique failure patterns\n\n")

        for i, (key, results) in enumerate(sorted(groups.items(),
                                                   key=lambda x: len(x[1]),
                                                   reverse=True), 1):
            failure_type, normalized_trace = key.split(':::', 1)

            f.write(f"\n{'='*80}\n")
            f.write(f"FAILURE GROUP #{i} ({len(results)} occurrences)\n")
            f.write(f"{'='*80}\n\n")

            f.write(f"Failure Type: {failure_type}\n\n")

            # Show first failure message
            f.write(f"Failure Message:\n")
            f.write(f"{results[0].failure_msg}\n\n")

            f.write(f"Normalized Stack Trace:\n")
            f.write("-" * 80 + "\n")
            f.write(normalized_trace)
            f.write("\n" + "-" * 80 + "\n\n")

            f.write(f"Affected Tests ({len(results)}):\n")
            f.write("-" * 80 + "\n")

            for result in results:
                f.write(f"  - {result.test_class}.{result.test_method}\n")
                f.write(f"    Position: {result.position}, Target: {result.target}, Mode: {result.mode}\n")
                f.write(f"    Directory: {result.test_dir}\n")

            f.write("\n")

        # List all failures with full details
        f.write("\n" + "=" * 80 + "\n")
        f.write("ALL FAILURES AND ERRORS (Detailed)\n")
        f.write("=" * 80 + "\n\n")

        for i, result in enumerate(failures_and_errors, 1):
            f.write(f"\n{'-'*80}\n")
            f.write(f"FAILURE #{i}\n")
            f.write(f"{'-'*80}\n\n")
            f.write(f"Test: {result.test_class}.{result.test_method}\n")
            f.write(f"Status: {result.status}\n")
            f.write(f"Restart Config: position={result.position}, target={result.target}, mode={result.mode}\n")
            f.write(f"Directory: {result.test_dir}\n")
            f.write(f"Failure Type: {result.failure_type}\n")
            f.write(f"Failure Message: {result.failure_msg}\n\n")
            f.write(f"Stack Trace:\n")
            f.write(result.stack_trace)
            f.write("\n\n")


def generate_json_report(analysis, output_file):
    """Generate a JSON report of test results."""

    report = {
        'summary': {
            'total': len(analysis['all_results']),
            'passed': len(analysis['passed']),
            'failed': len(analysis['failed']),
            'errored': len(analysis['errored']),
            'skipped': len(analysis['skipped']),
        },
        'results': [r.to_dict() for r in analysis['all_results']],
        'failureGroups': []
    }

    # Group failures
    failures_and_errors = analysis['failed'] + analysis['errored']
    groups = group_failures_by_stacktrace(failures_and_errors)

    for key, results in sorted(groups.items(), key=lambda x: len(x[1]), reverse=True):
        failure_type, normalized_trace = key.split(':::', 1)

        group = {
            'failureType': failure_type,
            'count': len(results),
            'normalizedStackTrace': normalized_trace,
            'failureMessage': results[0].failure_msg,
            'affectedTests': [
                {
                    'testClass': r.test_class,
                    'testMethod': r.test_method,
                    'position': r.position,
                    'target': r.target,
                    'mode': r.mode,
                    'directory': r.test_dir
                }
                for r in results
            ]
        }
        report['failureGroups'].append(group)

    with open(output_file, 'w') as f:
        json.dump(report, f, indent=2)


def main():
    if len(sys.argv) < 2:
        print("Usage: python analyze_restart_test_results.py <test_output_dir>")
        print("Example: python analyze_restart_test_results.py restart-test-output-20251203-143743")
        sys.exit(1)

    test_output_dir = sys.argv[1]

    print(f"Analyzing test results from: {test_output_dir}\n")

    # Analyze results
    analysis = analyze_test_results(test_output_dir)

    if analysis is None:
        sys.exit(1)

    # Generate text report
    report_file = f"{test_output_dir}_analysis_report.txt"
    print(f"\nGenerating text report: {report_file}")
    generate_report(analysis, report_file)

    # Generate JSON report
    json_file = f"{test_output_dir}_analysis_report.json"
    print(f"Generating JSON report: {json_file}")
    generate_json_report(analysis, json_file)

    print(f"\nDone! Reports generated:")
    print(f"  - {report_file}")
    print(f"  - {json_file}")


if __name__ == '__main__':
    main()
