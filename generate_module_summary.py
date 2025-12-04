#!/usr/bin/env python3
"""
Generate a summary of test results by module.
"""

import json
import sys
from pathlib import Path
from collections import defaultdict


def generate_module_summary(json_report_file):
    """Generate a summary of test results by module."""

    # Read JSON report
    with open(json_report_file, 'r') as f:
        report = json.load(f)

    # Group by module (from test directory)
    module_stats = defaultdict(lambda: {
        'total': 0,
        'passed': 0,
        'failed': 0,
        'errored': 0,
        'skipped': 0
    })

    for result in report['results']:
        # Extract module from test directory
        test_dir = result.get('testDirectory', '')
        if test_dir:
            # Module name is first part after number prefix (e.g., hadoop-yarn-client)
            # Format: 001-org.apache.hadoop.yarn.client.TestClass_testMethod__...
            # We need to extract the module from the path structure
            # Easier to extract from test class package
            test_class = result['testClass']
            if 'hadoop-yarn-client' in test_dir:
                module = 'hadoop-yarn-client'
            elif 'hadoop-yarn-applications-distributedshell' in test_dir:
                module = 'hadoop-yarn-applications-distributedshell'
            elif 'hadoop-yarn-applications-unmanaged-am-launcher' in test_dir:
                module = 'hadoop-yarn-applications-unmanaged-am-launcher'
            elif 'hadoop-yarn-services-core' in test_dir:
                module = 'hadoop-yarn-services-core'
            elif 'hadoop-yarn-services-api' in test_dir:
                module = 'hadoop-yarn-services-api'
            elif 'hadoop-yarn-server-tests' in test_dir:
                module = 'hadoop-yarn-server-tests'
            else:
                # Fallback: use package name
                parts = test_class.split('.')
                if len(parts) >= 4:
                    module = '.'.join(parts[:4])
                else:
                    module = 'unknown'
        else:
            module = 'unknown'

        module_stats[module]['total'] += 1

        status = result['status']
        if status == 'PASS':
            module_stats[module]['passed'] += 1
        elif status == 'FAIL':
            module_stats[module]['failed'] += 1
        elif status == 'ERROR':
            module_stats[module]['errored'] += 1
        elif status == 'SKIPPED':
            module_stats[module]['skipped'] += 1

    # Print summary
    print("=" * 100)
    print("MODULE-LEVEL TEST SUMMARY")
    print("=" * 100)
    print()

    # Sort modules by name
    for module in sorted(module_stats.keys()):
        stats = module_stats[module]
        total = stats['total']
        passed = stats['passed']
        failed = stats['failed']
        errored = stats['errored']
        skipped = stats['skipped']

        pass_rate = (passed / total * 100) if total > 0 else 0

        print(f"Module: {module}")
        print(f"  Total:   {total}")
        print(f"  Passed:  {passed} ({pass_rate:.1f}%)")
        print(f"  Failed:  {failed}")
        print(f"  Errored: {errored}")
        print(f"  Skipped: {skipped}")
        print()

    # Overall summary
    print("=" * 100)
    print("OVERALL SUMMARY")
    print("=" * 100)
    print()

    total_tests = sum(s['total'] for s in module_stats.values())
    total_passed = sum(s['passed'] for s in module_stats.values())
    total_failed = sum(s['failed'] for s in module_stats.values())
    total_errored = sum(s['errored'] for s in module_stats.values())
    total_skipped = sum(s['skipped'] for s in module_stats.values())

    overall_pass_rate = (total_passed / total_tests * 100) if total_tests > 0 else 0

    print(f"Total Tests:   {total_tests}")
    print(f"Passed:        {total_passed} ({overall_pass_rate:.1f}%)")
    print(f"Failed:        {total_failed}")
    print(f"Errored:       {total_errored}")
    print(f"Skipped:       {total_skipped}")
    print()

    # Top failure patterns
    print("=" * 100)
    print("TOP 10 FAILURE PATTERNS")
    print("=" * 100)
    print()

    for i, group in enumerate(report['failureGroups'][:10], 1):
        print(f"{i}. [{group['count']} occurrences] {group['failureType']}")
        print(f"   {group['failureMessage'][:100]}...")
        print()


def main():
    if len(sys.argv) < 2:
        print("Usage: python generate_module_summary.py <json_report_file>")
        print("Example: python generate_module_summary.py restart-test-output-20251203-143743_analysis_report.json")
        sys.exit(1)

    json_report_file = sys.argv[1]

    if not Path(json_report_file).exists():
        print(f"Error: File not found: {json_report_file}")
        sys.exit(1)

    generate_module_summary(json_report_file)


if __name__ == '__main__':
    main()
