#!/usr/bin/env python3
"""
Generate a CSV summary of test failures grouped by stack trace.
"""

import csv
import json
import sys
from pathlib import Path


def generate_csv_summary(json_report_file, csv_output_file):
    """Generate a CSV file summarizing failure groups."""

    # Read JSON report
    with open(json_report_file, 'r') as f:
        report = json.load(f)

    # Open CSV file for writing
    with open(csv_output_file, 'w', newline='') as csvfile:
        fieldnames = [
            'Group #',
            'Occurrences',
            'Failure Type',
            'Failure Message',
            'Test Classes Affected',
            'Test Methods Affected',
            'Normalized Stack Trace (first 200 chars)'
        ]

        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()

        for i, group in enumerate(report['failureGroups'], 1):
            # Get unique test classes and methods
            test_classes = set()
            test_methods = set()

            for test in group['affectedTests']:
                test_classes.add(test['testClass'])
                test_methods.add(f"{test['testClass']}.{test['testMethod']}")

            # Truncate stack trace for CSV
            stack_trace_preview = group['normalizedStackTrace'][:200]
            if len(group['normalizedStackTrace']) > 200:
                stack_trace_preview += "..."

            row = {
                'Group #': i,
                'Occurrences': group['count'],
                'Failure Type': group['failureType'],
                'Failure Message': group['failureMessage'],
                'Test Classes Affected': ', '.join(sorted(test_classes)),
                'Test Methods Affected': '; '.join(sorted(test_methods)),
                'Normalized Stack Trace (first 200 chars)': stack_trace_preview
            }

            writer.writerow(row)

    print(f"CSV summary written to: {csv_output_file}")


def generate_detailed_csv(json_report_file, csv_output_file):
    """Generate a detailed CSV with one row per test result."""

    # Read JSON report
    with open(json_report_file, 'r') as f:
        report = json.load(f)

    # Open CSV file for writing
    with open(csv_output_file, 'w', newline='') as csvfile:
        fieldnames = [
            'Test Class',
            'Test Method',
            'Status',
            'Time (s)',
            'Position',
            'Target',
            'Mode',
            'Failure Type',
            'Failure Message',
            'Test Directory'
        ]

        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()

        for result in report['results']:
            row = {
                'Test Class': result['testClass'],
                'Test Method': result['testMethod'],
                'Status': result['status'],
                'Time (s)': result['time'],
                'Position': result.get('position', ''),
                'Target': result.get('target', ''),
                'Mode': result.get('mode', ''),
                'Failure Type': result.get('failureType', ''),
                'Failure Message': result.get('failureMessage', ''),
                'Test Directory': result.get('testDirectory', '')
            }

            writer.writerow(row)

    print(f"Detailed CSV written to: {csv_output_file}")


def main():
    if len(sys.argv) < 2:
        print("Usage: python generate_failure_summary.py <json_report_file>")
        print("Example: python generate_failure_summary.py restart-test-output-20251203-143743_analysis_report.json")
        sys.exit(1)

    json_report_file = sys.argv[1]

    if not Path(json_report_file).exists():
        print(f"Error: File not found: {json_report_file}")
        sys.exit(1)

    # Generate summary CSV
    base_name = json_report_file.replace('_analysis_report.json', '')
    summary_csv = f"{base_name}_failure_groups.csv"
    generate_csv_summary(json_report_file, summary_csv)

    # Generate detailed CSV
    detailed_csv = f"{base_name}_all_results.csv"
    generate_detailed_csv(json_report_file, detailed_csv)

    print("\nDone! Generated files:")
    print(f"  - {summary_csv} (Failure groups summary)")
    print(f"  - {detailed_csv} (All test results)")


if __name__ == '__main__':
    main()
