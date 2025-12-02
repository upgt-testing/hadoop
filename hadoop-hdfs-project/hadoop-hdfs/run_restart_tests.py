#!/usr/bin/env python3
"""
Script to find and run all *_RestartInjected.java test files using Maven Surefire.
Logs are saved to a timestamped directory.
"""

import os
import subprocess
import sys
from datetime import datetime
from pathlib import Path


def find_restart_test_files(base_dir):
    """Find all *_RestartInjected.java test files."""
    test_files = []
    for root, dirs, files in os.walk(base_dir):
        for file in files:
            if file.endswith('_RestartInjected.java'):
                test_files.append(os.path.join(root, file))
    return sorted(test_files)


def extract_test_class_name(file_path):
    """Extract the fully qualified test class name from the file path."""
    # Get the file name without extension
    file_name = os.path.basename(file_path)
    class_name = file_name.replace('.java', '')

    # Extract package name from file
    with open(file_path, 'r') as f:
        for line in f:
            line = line.strip()
            if line.startswith('package '):
                package = line.replace('package ', '').replace(';', '').strip()
                return f"{package}.{class_name}"

    # Fallback: just return class name
    return class_name


def create_log_directory():
    """Create a timestamped log directory."""
    timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')
    log_dir = f"restart-test-log-{timestamp}"
    os.makedirs(log_dir, exist_ok=True)
    return log_dir


def run_maven_test(test_class_name, log_dir, test_number, total_tests):
    """Run a single test using Maven Surefire and capture the output."""
    log_file = os.path.join(log_dir, f"{test_class_name.split('.')[-1]}.log")

    print(f"\n[{test_number}/{total_tests}] Running test: {test_class_name}")
    print(f"  Log file: {log_file}")

    # Maven command
    cmd = ['mvn', 'surefire:test', f'-Dtest={test_class_name}']

    try:
        with open(log_file, 'w') as log:
            # Write header to log file
            log.write(f"{'='*80}\n")
            log.write(f"Test: {test_class_name}\n")
            log.write(f"Command: {' '.join(cmd)}\n")
            log.write(f"Start time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            log.write(f"{'='*80}\n\n")
            log.flush()

            # Run the test
            process = subprocess.Popen(
                cmd,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                universal_newlines=True,
                bufsize=1
            )

            # Stream output to both console and log file
            for line in process.stdout:
                print(line, end='')
                log.write(line)
                log.flush()

            process.wait()

            # Write footer to log file
            log.write(f"\n{'='*80}\n")
            log.write(f"End time: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            log.write(f"Exit code: {process.returncode}\n")
            log.write(f"{'='*80}\n")

            if process.returncode == 0:
                print(f"  ✓ Test passed")
            else:
                print(f"  ✗ Test failed (exit code: {process.returncode})")

            return process.returncode

    except Exception as e:
        error_msg = f"Error running test {test_class_name}: {str(e)}\n"
        print(f"  ✗ {error_msg}")
        with open(log_file, 'a') as log:
            log.write(f"\n{error_msg}")
        return -1


def main():
    """Main function to orchestrate test execution."""
    print("="*80)
    print("Restart Test Runner")
    print("="*80)

    # Find the base directory (src/test/java)
    current_dir = os.path.dirname(os.path.abspath(__file__))
    base_test_dir = os.path.join(current_dir, 'src', 'test', 'java')

    if not os.path.exists(base_test_dir):
        print(f"Error: Test directory not found: {base_test_dir}")
        sys.exit(1)

    print(f"\nSearching for *_RestartInjected.java files in: {base_test_dir}")

    # Find all test files
    test_files = find_restart_test_files(base_test_dir)

    if not test_files:
        print("No *_RestartInjected.java test files found!")
        sys.exit(1)

    print(f"Found {len(test_files)} test file(s)\n")

    # Create log directory
    log_dir = create_log_directory()
    print(f"Log directory: {log_dir}\n")

    # Extract test class names
    test_classes = []
    for test_file in test_files:
        class_name = extract_test_class_name(test_file)
        test_classes.append(class_name)

    # Write test list to log directory
    test_list_file = os.path.join(log_dir, 'test_list.txt')
    with open(test_list_file, 'w') as f:
        f.write(f"Total tests: {len(test_classes)}\n")
        f.write(f"{'='*80}\n\n")
        for i, test_class in enumerate(test_classes, 1):
            f.write(f"{i}. {test_class}\n")

    print(f"Test list saved to: {test_list_file}")

    # Run all tests
    results = []
    passed = 0
    failed = 0

    for i, test_class in enumerate(test_classes, 1):
        exit_code = run_maven_test(test_class, log_dir, i, len(test_classes))
        results.append((test_class, exit_code))

        if exit_code == 0:
            passed += 1
        else:
            failed += 1

    # Print summary
    print("\n" + "="*80)
    print("Test Execution Summary")
    print("="*80)
    print(f"Total tests: {len(test_classes)}")
    print(f"Passed: {passed}")
    print(f"Failed: {failed}")
    print(f"\nLogs saved in: {log_dir}")

    # Write summary to file
    summary_file = os.path.join(log_dir, 'summary.txt')
    with open(summary_file, 'w') as f:
        f.write(f"Test Execution Summary\n")
        f.write(f"{'='*80}\n")
        f.write(f"Total tests: {len(test_classes)}\n")
        f.write(f"Passed: {passed}\n")
        f.write(f"Failed: {failed}\n")
        f.write(f"{'='*80}\n\n")

        for test_class, exit_code in results:
            status = "PASS" if exit_code == 0 else "FAIL"
            f.write(f"[{status}] {test_class}\n")

    print(f"Summary saved to: {summary_file}")
    print("="*80)

    # Exit with error code if any tests failed
    sys.exit(0 if failed == 0 else 1)


if __name__ == '__main__':
    main()
