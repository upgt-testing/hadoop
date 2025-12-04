#!/usr/bin/env python3
"""
Analyze YARN Restart Test Results

This script:
1. Parses all JSON test reports from restart test output
2. Extracts all test method results
3. Gathers all failures/errors
4. Groups failures by normalized stacktrace
5. Generates comprehensive summary reports
"""

import json
import os
import re
from collections import defaultdict
from pathlib import Path
from typing import Dict, List, Tuple, Optional


class StackTraceNormalizer:
    """Normalize stack traces for grouping similar failures"""

    @staticmethod
    def normalize(stacktrace: str, test_method: str) -> str:
        """
        Normalize a stack trace by:
        1. Removing all line numbers
        2. Removing all parts after the test method name appears

        Args:
            stacktrace: The original stack trace
            test_method: The test method name to look for

        Returns:
            Normalized stack trace
        """
        if not stacktrace:
            return ""

        lines = stacktrace.split('\n')
        normalized_lines = []

        for line in lines:
            # Remove line numbers (e.g., :123, .java:45)
            # Pattern matches things like "File.java:123" or ":123"
            line_normalized = re.sub(r':\d+\)', ')', line)  # Remove :123)
            line_normalized = re.sub(r'\.java:\d+', '.java', line_normalized)  # Remove .java:123
            line_normalized = re.sub(r':\d+', '', line_normalized)  # Remove remaining :123

            normalized_lines.append(line_normalized)

            # Stop after we see the test method name
            if test_method in line:
                break

        return '\n'.join(normalized_lines)

    @staticmethod
    def extract_exception_type(stacktrace: str) -> str:
        """Extract the exception type from a stacktrace"""
        if not stacktrace:
            return "Unknown"

        lines = stacktrace.split('\n')
        if lines:
            # First line usually contains the exception type
            first_line = lines[0].strip()
            # Extract exception class name
            match = re.match(r'([a-zA-Z0-9_.]+(?:Exception|Error|Throwable))', first_line)
            if match:
                return match.group(1)

        return "Unknown"


class TestResult:
    """Represents a single test execution result"""

    def __init__(self, data: dict, module: str):
        self.module = module
        self.test_class = data.get('testClass', 'Unknown')
        self.test_method = data.get('testMethod', 'Unknown')
        self.position = data.get('position', None)
        self.target = data.get('target', None)
        self.mode = data.get('mode', None)
        self.passed = data.get('passed', True)
        self.duration_ms = data.get('durationMs', 0)
        self.error_message = data.get('errorMessage', None)
        self.stack_trace = data.get('stackTrace', None)

    def is_baseline(self) -> bool:
        """Check if this is a baseline test (no restart)"""
        return self.position is None and self.target is None and self.mode is None

    def get_scenario_key(self) -> str:
        """Get a unique key for this test scenario"""
        if self.is_baseline():
            return f"{self.test_class}.{self.test_method} [BASELINE]"
        else:
            return f"{self.test_class}.{self.test_method} [pos={self.position}, target={self.target}, mode={self.mode}]"

    def get_test_method_key(self) -> str:
        """Get a key identifying the test method (without scenario details)"""
        return f"{self.test_class}.{self.test_method}"

    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization"""
        return {
            'module': self.module,
            'testClass': self.test_class,
            'testMethod': self.test_method,
            'position': self.position,
            'target': self.target,
            'mode': self.mode,
            'passed': self.passed,
            'durationMs': self.duration_ms,
            'errorMessage': self.error_message,
            'stackTrace': self.stack_trace,
            'scenarioKey': self.get_scenario_key(),
            'isBaseline': self.is_baseline()
        }


class FailureGroup:
    """Groups test failures by normalized stack trace"""

    def __init__(self, normalized_stacktrace: str, exception_type: str):
        self.normalized_stacktrace = normalized_stacktrace
        self.exception_type = exception_type
        self.failures: List[TestResult] = []

    def add_failure(self, result: TestResult):
        """Add a failure to this group"""
        self.failures.append(result)

    def get_affected_test_methods(self) -> set:
        """Get set of unique test methods affected by this failure"""
        return {f.get_test_method_key() for f in self.failures}

    def get_affected_modules(self) -> set:
        """Get set of unique modules affected by this failure"""
        return {f.module for f in self.failures}

    def to_dict(self) -> dict:
        """Convert to dictionary for JSON serialization"""
        return {
            'exceptionType': self.exception_type,
            'normalizedStackTrace': self.normalized_stacktrace,
            'failureCount': len(self.failures),
            'affectedTestMethods': sorted(list(self.get_affected_test_methods())),
            'affectedModules': sorted(list(self.get_affected_modules())),
            'failures': [f.to_dict() for f in self.failures]
        }


class TestAnalyzer:
    """Main analyzer for test results"""

    def __init__(self, output_dir: str):
        self.output_dir = Path(output_dir)
        self.all_results: List[TestResult] = []
        self.failures: List[TestResult] = []
        self.failure_groups: Dict[str, FailureGroup] = {}
        self.module_summaries: Dict[str, dict] = {}

    def load_all_reports(self):
        """Load all JSON reports from the output directory"""
        print(f"Loading test reports from: {self.output_dir}")

        # Find all module directories
        for module_dir in self.output_dir.iterdir():
            if not module_dir.is_dir():
                continue

            module_name = module_dir.name
            report_file = module_dir / "restart-test-report.json"

            if not report_file.exists():
                print(f"  ⚠ No report found for module: {module_name}")
                continue

            print(f"  Loading: {module_name}")
            self._load_module_report(module_name, report_file)

        print(f"\nLoaded {len(self.all_results)} total test executions")
        print(f"Found {len(self.failures)} failures")

    def _load_module_report(self, module_name: str, report_file: Path):
        """Load a single module's test report"""
        with open(report_file, 'r') as f:
            data = json.load(f)

        # Store module summary
        self.module_summaries[module_name] = data.get('summary', {})

        # Parse all test results
        results = data.get('results', [])
        for result_data in results:
            result = TestResult(result_data, module_name)
            self.all_results.append(result)

            if not result.passed:
                self.failures.append(result)

    def group_failures_by_stacktrace(self):
        """Group failures by normalized stack trace"""
        print("\nGrouping failures by normalized stack trace...")

        normalizer = StackTraceNormalizer()

        for failure in self.failures:
            # Normalize the stack trace
            normalized = normalizer.normalize(
                failure.stack_trace or "",
                failure.test_method
            )

            # Extract exception type
            exception_type = normalizer.extract_exception_type(
                failure.stack_trace or failure.error_message or ""
            )

            # Group by normalized stacktrace
            if normalized not in self.failure_groups:
                self.failure_groups[normalized] = FailureGroup(normalized, exception_type)

            self.failure_groups[normalized].add_failure(failure)

        print(f"Grouped into {len(self.failure_groups)} unique failure patterns")

    def generate_summary(self) -> dict:
        """Generate overall summary statistics"""
        total_tests = len(self.all_results)
        passed_tests = sum(1 for r in self.all_results if r.passed)
        failed_tests = len(self.failures)

        # Count unique test methods
        unique_test_methods = len(set(r.get_test_method_key() for r in self.all_results))

        # Calculate total duration
        total_duration_ms = sum(r.duration_ms for r in self.all_results)

        return {
            'totalExecutions': total_tests,
            'passedExecutions': passed_tests,
            'failedExecutions': failed_tests,
            'passRate': (passed_tests / total_tests * 100) if total_tests > 0 else 0,
            'uniqueTestMethods': unique_test_methods,
            'totalDurationMs': total_duration_ms,
            'totalDurationMinutes': round(total_duration_ms / 60000, 2),
            'modulesAnalyzed': len(self.module_summaries),
            'uniqueFailurePatterns': len(self.failure_groups)
        }

    def save_results(self, output_file: str):
        """Save analysis results to JSON file"""
        output_path = Path(output_file)

        analysis = {
            'summary': self.generate_summary(),
            'moduleSummaries': self.module_summaries,
            'failureGroups': [
                group.to_dict()
                for group in sorted(
                    self.failure_groups.values(),
                    key=lambda g: len(g.failures),
                    reverse=True
                )
            ],
            'allFailures': [f.to_dict() for f in self.failures],
            'allResults': [r.to_dict() for r in self.all_results]
        }

        with open(output_path, 'w') as f:
            json.dump(analysis, f, indent=2)

        print(f"\n✓ Saved detailed analysis to: {output_path}")

    def print_summary_report(self):
        """Print a human-readable summary report"""
        summary = self.generate_summary()

        print("\n" + "=" * 80)
        print("YARN RESTART TEST ANALYSIS SUMMARY")
        print("=" * 80)
        print(f"\nModules Analyzed: {summary['modulesAnalyzed']}")
        print(f"Unique Test Methods: {summary['uniqueTestMethods']}")
        print(f"Total Test Executions: {summary['totalExecutions']}")
        print(f"  ✓ Passed: {summary['passedExecutions']}")
        print(f"  ✗ Failed: {summary['failedExecutions']}")
        print(f"Pass Rate: {summary['passRate']:.2f}%")
        print(f"Total Duration: {summary['totalDurationMinutes']:.2f} minutes")

        print("\n" + "-" * 80)
        print("MODULE BREAKDOWN")
        print("-" * 80)
        for module_name, module_summary in sorted(self.module_summaries.items()):
            total = module_summary.get('totalTests', 0)
            passed = module_summary.get('passed', 0)
            failed = module_summary.get('failed', 0)
            pass_rate = module_summary.get('passRate', 0)

            status = "✓" if failed == 0 else "✗"
            print(f"{status} {module_name}")
            print(f"    Total: {total}, Passed: {passed}, Failed: {failed}, "
                  f"Pass Rate: {pass_rate:.1f}%")

        if self.failure_groups:
            print("\n" + "-" * 80)
            print("FAILURE GROUPS (by normalized stack trace)")
            print("-" * 80)

            for i, group in enumerate(sorted(
                self.failure_groups.values(),
                key=lambda g: len(g.failures),
                reverse=True
            ), 1):
                print(f"\nGroup #{i}: {group.exception_type}")
                print(f"  Failure Count: {len(group.failures)}")
                print(f"  Affected Test Methods: {len(group.get_affected_test_methods())}")
                print(f"  Affected Modules: {', '.join(sorted(group.get_affected_modules()))}")
                print(f"  Test Methods:")
                for method in sorted(group.get_affected_test_methods()):
                    print(f"    - {method}")

                # Show normalized stacktrace (truncated)
                if group.normalized_stacktrace:
                    lines = group.normalized_stacktrace.split('\n')
                    preview = '\n'.join(lines[:5])
                    print(f"  Stack Trace Preview:")
                    for line in lines[:5]:
                        print(f"    {line}")
                    if len(lines) > 5:
                        print(f"    ... ({len(lines) - 5} more lines)")
        else:
            print("\n" + "-" * 80)
            print("✓ NO FAILURES DETECTED - All tests passed!")
            print("-" * 80)

        print("\n" + "=" * 80)


def main():
    """Main entry point"""
    import sys

    # Default to the most recent output directory
    if len(sys.argv) > 1:
        output_dir = sys.argv[1]
    else:
        # Find the most recent restart-test-output directory
        current_dir = Path.cwd()
        output_dirs = sorted(current_dir.glob("restart-test-output-*"), reverse=True)

        if not output_dirs:
            print("Error: No restart-test-output-* directories found")
            print("Usage: python analyze_restart_tests.py [output_directory]")
            sys.exit(1)

        output_dir = str(output_dirs[0])

    # Create analyzer
    analyzer = TestAnalyzer(output_dir)

    # Load and analyze
    analyzer.load_all_reports()
    analyzer.group_failures_by_stacktrace()

    # Generate reports
    analyzer.print_summary_report()

    # Save detailed results
    output_file = Path(output_dir) / "ANALYSIS.json"
    analyzer.save_results(str(output_file))

    print(f"\n✓ Analysis complete!")
    print(f"  Detailed results: {output_file}")


if __name__ == "__main__":
    main()
