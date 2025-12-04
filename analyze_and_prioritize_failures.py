#!/usr/bin/env python3
"""
Analyze and prioritize YARN restart test failures by likelihood of being real bugs.
"""

import re
from collections import defaultdict
from dataclasses import dataclass
from typing import List, Tuple

@dataclass
class FailureGroup:
    group_num: int
    count: int
    failure_type: str
    failure_message: str
    stack_trace: str
    affected_tests: List[dict]

    def is_framework_issue(self) -> bool:
        """Check if this is a testing framework issue (low priority)"""
        framework_indicators = [
            "No adapter registered or discovered",
            "org.restarttest.core.RestartException",
            "org.restarttest.api.",
            "AdapterRegistry.getAdapterOrDiscover",
            "UnsatisfiedLinkError"  # Native library issues are environment problems
        ]

        for indicator in framework_indicators:
            if indicator in self.failure_message or indicator in self.stack_trace or indicator in self.failure_type:
                return True
        return False

    def priority_score(self) -> Tuple[int, int]:
        """
        Return priority score (category, -count).
        Lower number = higher priority.
        Category: 1=high, 2=medium, 3=low
        """
        # Framework issues = low priority
        if self.is_framework_issue():
            return (3, -self.count)

        # Real bugs = high priority
        high_priority_indicators = [
            "IndexOutOfBoundsException",
            "AssertionError",
            "NullPointerException",
            "ConcurrentModificationException",
            "IllegalStateException",
        ]

        for indicator in high_priority_indicators:
            if indicator in self.failure_type:
                return (1, -self.count)

        # Timeouts could be real bugs (race conditions) = high/medium priority
        if "TestTimedOutException" in self.failure_type or "TimeoutException" in self.failure_type:
            return (1, -self.count)

        # Connection/network issues during restart = medium priority
        connection_indicators = [
            "ConnectException",
            "SocketException",
            "ConnectionRefusedException",
            "EOFException",
        ]

        for indicator in connection_indicators:
            if indicator in self.failure_type or indicator in self.failure_message:
                return (2, -self.count)

        # Everything else = medium priority
        return (2, -self.count)


def parse_failure_report(filepath: str) -> List[FailureGroup]:
    """Parse the analysis report and extract failure groups"""
    with open(filepath, 'r') as f:
        content = f.read()

    groups = []

    # Split by failure groups
    pattern = r'={80}\nFAILURE GROUP #(\d+) \((\d+) occurrences?\)\n={80}'
    splits = re.split(pattern, content)

    # Skip the header (index 0)
    for i in range(1, len(splits), 3):
        if i+2 >= len(splits):
            break

        group_num = int(splits[i])
        count = int(splits[i+1])
        group_content = splits[i+2]

        # Extract failure type
        type_match = re.search(r'Failure Type:\s*(.+)', group_content)
        failure_type = type_match.group(1).strip() if type_match else "Unknown"

        # Extract failure message
        msg_match = re.search(r'Failure Message:\s*(.+?)(?=\n\nNormalized Stack Trace:)', group_content, re.DOTALL)
        failure_message = msg_match.group(1).strip() if msg_match else ""

        # Extract stack trace
        stack_match = re.search(r'Normalized Stack Trace:\n-+\n(.+?)\n-+', group_content, re.DOTALL)
        stack_trace = stack_match.group(1).strip() if stack_match else ""

        # Extract affected tests
        tests_match = re.search(r'Affected Tests \((\d+)\):\n-+\n(.+?)(?=\n\n\n|$)', group_content, re.DOTALL)
        affected_tests = []

        if tests_match:
            tests_content = tests_match.group(2)
            # Parse each test entry
            test_blocks = re.split(r'  - ', tests_content)[1:]  # Skip empty first element

            for block in test_blocks:
                lines = block.strip().split('\n')
                if not lines:
                    continue

                test_name = lines[0].strip()
                position = None
                target = None
                mode = None
                directory = None

                for line in lines[1:]:
                    line = line.strip()
                    if line.startswith('Position:'):
                        # Extract Position, Target, and Mode from the same line
                        pos_match = re.search(r'Position:\s*([^,]+)', line)
                        tgt_match = re.search(r'Target:\s*([^,]+)', line)
                        mode_match = re.search(r'Mode:\s*(.+)', line)

                        position = pos_match.group(1).strip() if pos_match else None
                        target = tgt_match.group(1).strip() if tgt_match else None
                        mode = mode_match.group(1).strip() if mode_match else None
                    elif line.startswith('Directory:'):
                        directory = re.search(r'Directory:\s*(.+)', line).group(1).strip()

                affected_tests.append({
                    'test_name': test_name,
                    'position': position,
                    'target': target,
                    'mode': mode,
                    'directory': directory
                })

        groups.append(FailureGroup(
            group_num=group_num,
            count=count,
            failure_type=failure_type,
            failure_message=failure_message,
            stack_trace=stack_trace,
            affected_tests=affected_tests
        ))

    return groups


def group_similar_failures(groups: List[FailureGroup]) -> List[List[FailureGroup]]:
    """Group similar failures together"""
    # Group by failure type and similar messages
    similarity_groups = defaultdict(list)

    for group in groups:
        # Create a key based on failure type and first line of stack trace
        first_stack_line = group.stack_trace.split('\n')[0] if group.stack_trace else ""
        key = f"{group.failure_type}|{first_stack_line}"
        similarity_groups[key].append(group)

    return list(similarity_groups.values())


def generate_markdown_report(grouped_failures: List[List[FailureGroup]], output_file: str):
    """Generate prioritized markdown report"""

    # Flatten and sort by priority
    all_groups = []
    for similar_groups in grouped_failures:
        all_groups.extend(similar_groups)

    all_groups.sort(key=lambda g: g.priority_score())

    with open(output_file, 'w') as f:
        f.write("# YARN Restart Test Failure Inspection List\n\n")
        f.write("Failures prioritized by likelihood of being real bugs (highest priority first).\n\n")

        # Add summary of high priority bugs
        high_priority_groups = [g for g in all_groups if g.priority_score()[0] == 1]
        if high_priority_groups:
            f.write("## Executive Summary\n\n")
            f.write(f"**Total Failure Groups:** {len(all_groups)}\n")
            f.write(f"**High Priority (Real Bugs):** {len(high_priority_groups)} groups\n\n")

            f.write("### Top Issues (by occurrence count):\n\n")
            for i, group in enumerate(high_priority_groups[:5], 1):
                # Extract test class name for brevity
                first_test = group.affected_tests[0]['test_name'] if group.affected_tests else "Unknown"
                test_class = first_test.rsplit('.', 1)[0] if '.' in first_test else first_test
                f.write(f"{i}. **{group.failure_type}** ({group.count} occurrences)\n")
                msg_preview = group.failure_message.split('\n')[0][:100]
                f.write(f"   - {msg_preview}\n")
                f.write(f"   - Test class: `{test_class}`\n\n")

            f.write("---\n\n")

        f.write("**Legend:**\n")
        f.write("- 🔴 HIGH PRIORITY: Likely real bugs (exceptions, assertion failures, etc.)\n")
        f.write("- 🟡 MEDIUM PRIORITY: Unclear failures, may need investigation\n")
        f.write("- ⚪ LOW PRIORITY: Testing framework issues\n\n")
        f.write("---\n\n")

        current_category = None
        group_counter = 1

        for group in all_groups:
            priority_cat, _ = group.priority_score()

            # Add category header
            if priority_cat != current_category:
                current_category = priority_cat
                if priority_cat == 1:
                    f.write("\n## 🔴 HIGH PRIORITY - Likely Real Bugs\n\n")
                elif priority_cat == 2:
                    f.write("\n## 🟡 MEDIUM PRIORITY - Needs Investigation\n\n")
                else:
                    f.write("\n## ⚪ LOW PRIORITY - Testing Framework Issues\n\n")

            # Write group details
            f.write(f"### Group {group_counter}: {group.failure_type}\n\n")
            f.write(f"**Occurrences:** {group.count}\n\n")
            f.write(f"**Failure Message:**\n```\n{group.failure_message}\n```\n\n")

            # Show first few lines of stack trace
            stack_lines = group.stack_trace.split('\n')[:5]
            f.write(f"**Stack Trace (first 5 lines):**\n```\n" + '\n'.join(stack_lines) + "\n```\n\n")

            # Write affected tests with reproduction info
            f.write(f"**Affected Tests ({len(group.affected_tests)}):**\n\n")
            for test in group.affected_tests:
                f.write(f"- **Test:** `{test['test_name']}`\n")
                f.write(f"  - **Position:** `{test['position'] or 'N/A'}`\n")
                f.write(f"  - **Target:** `{test['target'] or 'N/A'}`\n")
                f.write(f"  - **Mode:** `{test['mode'] or 'N/A'}`\n")
                if test['directory']:
                    f.write(f"  - **Directory:** `{test['directory']}`\n")
                f.write("\n")

            f.write("---\n\n")
            group_counter += 1


if __name__ == "__main__":
    print("Parsing failure report...")
    groups = parse_failure_report("/Users/allenwang/xlab/yarn-transform/restart-test-output-20251203-143743_analysis_report.txt")

    print(f"Found {len(groups)} failure groups")

    print("Grouping similar failures...")
    grouped = group_similar_failures(groups)

    print(f"Grouped into {len(grouped)} similar failure categories")

    print("Generating prioritized markdown report...")
    generate_markdown_report(grouped, "/Users/allenwang/xlab/yarn-transform/Yarn_restart_inspection_list.md")

    print("Done! Report written to Yarn_restart_inspection_list.md")

    # Print summary
    high_priority = sum(1 for g in groups if g.priority_score()[0] == 1)
    medium_priority = sum(1 for g in groups if g.priority_score()[0] == 2)
    low_priority = sum(1 for g in groups if g.priority_score()[0] == 3)

    print(f"\nPriority Summary:")
    print(f"  🔴 High Priority: {high_priority} groups")
    print(f"  🟡 Medium Priority: {medium_priority} groups")
    print(f"  ⚪ Low Priority: {low_priority} groups")
