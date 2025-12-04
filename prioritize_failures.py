#!/usr/bin/env python3
"""
Script to prioritize YARN restart test failures by likelihood of being real bugs.
"""

import re
from typing import List, Dict, Tuple
from dataclasses import dataclass
from enum import Enum


class BugLikelihood(Enum):
    """Categories for bug likelihood"""
    CRITICAL = 1  # Almost certainly a real bug
    HIGH = 2      # Likely a real bug
    MEDIUM = 3    # Could be a bug or test issue
    LOW = 4       # Likely test infrastructure issue
    INFRA = 5     # Definitely test infrastructure issue


@dataclass
class FailureGroup:
    """Represents a failure group from the analysis report"""
    group_num: int
    occurrences: int
    failure_type: str
    failure_message: str
    stack_trace: str
    affected_tests: List[str]

    def get_priority_score(self) -> Tuple[BugLikelihood, int]:
        """
        Determine the likelihood this is a real bug vs test infrastructure issue.
        Returns (likelihood_category, occurrences) for sorting.
        """
        msg_lower = self.failure_message.lower()
        type_lower = self.failure_type.lower()
        trace_lower = self.stack_trace.lower()

        # INFRA: Test framework/adapter issues
        if "no adapter registered" in msg_lower:
            return (BugLikelihood.INFRA, self.occurrences)
        if "clusteradapter" in trace_lower:
            return (BugLikelihood.INFRA, self.occurrences)
        if "meta-inf/services" in msg_lower:
            return (BugLikelihood.INFRA, self.occurrences)

        # INFRA: Test setup/teardown issues
        if "bindexception" in type_lower and "address already in use" in msg_lower:
            return (BugLikelihood.INFRA, self.occurrences)
        if "cannot assign requested address" in msg_lower:
            return (BugLikelihood.INFRA, self.occurrences)
        if "test timed out" in msg_lower:
            return (BugLikelihood.LOW, self.occurrences)

        # CRITICAL: Data loss or corruption
        if "data" in msg_lower and ("lost" in msg_lower or "missing" in msg_lower):
            return (BugLikelihood.CRITICAL, self.occurrences)
        if "corrupt" in msg_lower:
            return (BugLikelihood.CRITICAL, self.occurrences)

        # CRITICAL: State inconsistency after restart
        if "expected:" in msg_lower and "but was:" in msg_lower:
            # These are assertion failures - could indicate real state issues
            if any(keyword in msg_lower for keyword in ["expected:<4> but was:<0>", "expected:<1> but was:<0>"]):
                # Complete loss of state (going to 0) is suspicious
                return (BugLikelihood.CRITICAL, self.occurrences)
            return (BugLikelihood.HIGH, self.occurrences)

        # HIGH: Deadlocks and hangs
        if "deadlock" in msg_lower:
            return (BugLikelihood.CRITICAL, self.occurrences)
        if "hung" in msg_lower or "hanging" in msg_lower:
            return (BugLikelihood.HIGH, self.occurrences)

        # HIGH: Connection/communication failures after restart
        if "connection refused" in msg_lower:
            return (BugLikelihood.HIGH, self.occurrences)
        if "connection closed" in msg_lower or "connection reset" in msg_lower:
            return (BugLikelihood.HIGH, self.occurrences)
        if "eofexception" in type_lower:
            return (BugLikelihood.HIGH, self.occurrences)

        # HIGH: Resource/state management issues
        if "nullpointerexception" in type_lower:
            # NPE could indicate improper handling after restart
            return (BugLikelihood.HIGH, self.occurrences)
        if "illegalstateexception" in type_lower:
            return (BugLikelihood.HIGH, self.occurrences)

        # HIGH: Failures during failover/recovery
        if "failover" in msg_lower and "fail" in msg_lower:
            return (BugLikelihood.HIGH, self.occurrences)
        if "recovery" in msg_lower and "fail" in msg_lower:
            return (BugLikelihood.HIGH, self.occurrences)

        # MEDIUM: Retry/timeout issues (could be real or environmental)
        if "retries" in msg_lower or "retry" in msg_lower:
            return (BugLikelihood.MEDIUM, self.occurrences)
        if "timeout" in msg_lower and "test" not in msg_lower:
            return (BugLikelihood.MEDIUM, self.occurrences)

        # MEDIUM: Unexpected exceptions (need investigation)
        if "unexpected" in msg_lower:
            return (BugLikelihood.MEDIUM, self.occurrences)

        # MEDIUM: File/IO issues (could be real or test environment)
        if "filenotfoundexception" in type_lower or "ioexception" in type_lower:
            return (BugLikelihood.MEDIUM, self.occurrences)

        # LOW: General assertion failures without clear context
        if "assertionerror" in type_lower or "junit.framework.assertionfailederror" in type_lower:
            return (BugLikelihood.LOW, self.occurrences)

        # MEDIUM: Everything else needs investigation
        return (BugLikelihood.MEDIUM, self.occurrences)


def parse_report(file_path: str) -> List[FailureGroup]:
    """Parse the analysis report and extract failure groups"""
    with open(file_path, 'r') as f:
        content = f.read()

    groups = []

    # Split by failure group markers
    pattern = r'={80,}\nFAILURE GROUP #(\d+) \((\d+) occurrences?\)\n={80,}'
    splits = re.split(pattern, content)

    # First element is header, then alternating group_num, occurrences, content
    for i in range(1, len(splits), 3):
        if i + 2 >= len(splits):
            break

        group_num = int(splits[i])
        occurrences = int(splits[i+1])
        group_content = splits[i+2]

        # Extract failure type
        type_match = re.search(r'Failure Type:\s*(.+)', group_content)
        failure_type = type_match.group(1).strip() if type_match else "Unknown"

        # Extract failure message
        msg_match = re.search(r'Failure Message:\s*\n(.+?)(?=\n\nNormalized Stack Trace:)', group_content, re.DOTALL)
        failure_message = msg_match.group(1).strip() if msg_match else "Unknown"

        # Extract stack trace
        trace_match = re.search(r'Normalized Stack Trace:\s*\n-+\n(.+?)\n-+', group_content, re.DOTALL)
        stack_trace = trace_match.group(1).strip() if trace_match else "Unknown"

        # Extract affected tests (just count for now)
        test_matches = re.findall(r'  - (org\.apache\.hadoop\..+)', group_content)
        affected_tests = test_matches

        groups.append(FailureGroup(
            group_num=group_num,
            occurrences=occurrences,
            failure_type=failure_type,
            failure_message=failure_message,
            stack_trace=stack_trace,
            affected_tests=affected_tests
        ))

    return groups


def generate_prioritized_report(groups: List[FailureGroup]) -> str:
    """Generate a prioritized markdown report"""

    # Sort by likelihood (most likely bug first), then by occurrences (descending)
    sorted_groups = sorted(groups, key=lambda g: (g.get_priority_score()[0].value, -g.get_priority_score()[1]))

    # Group by likelihood
    by_likelihood = {}
    for group in sorted_groups:
        likelihood, _ = group.get_priority_score()
        if likelihood not in by_likelihood:
            by_likelihood[likelihood] = []
        by_likelihood[likelihood].append(group)

    # Generate report
    report = []
    report.append("# YARN Restart Test Failure Inspection Priority List\n")
    report.append(f"**Generated:** {__file__}\n")
    report.append(f"**Total Unique Failure Patterns:** {len(groups)}\n")
    report.append("\n---\n")

    # Summary statistics
    report.append("\n## Summary by Priority\n\n")
    for likelihood in BugLikelihood:
        if likelihood in by_likelihood:
            count = len(by_likelihood[likelihood])
            total_occurrences = sum(g.occurrences for g in by_likelihood[likelihood])
            report.append(f"- **{likelihood.name}**: {count} patterns ({total_occurrences} test failures)\n")

    report.append("\n---\n")

    # Detailed listings by priority
    priority_descriptions = {
        BugLikelihood.CRITICAL: (
            "CRITICAL PRIORITY - Almost Certainly Real Bugs",
            "These failures indicate severe issues like data loss, state corruption, or complete loss of functionality after restart. **Investigate these first.**"
        ),
        BugLikelihood.HIGH: (
            "HIGH PRIORITY - Likely Real Bugs",
            "These failures suggest real issues with restart handling, including connection failures, null pointers, or illegal states after restart."
        ),
        BugLikelihood.MEDIUM: (
            "MEDIUM PRIORITY - Needs Investigation",
            "These failures could be real bugs or environmental issues. Require investigation to determine root cause."
        ),
        BugLikelihood.LOW: (
            "LOW PRIORITY - Likely Test Issues",
            "These failures are more likely related to test configuration or timing issues than actual bugs."
        ),
        BugLikelihood.INFRA: (
            "TEST INFRASTRUCTURE ISSUES",
            "These failures are clearly related to test framework setup and configuration, not product bugs."
        ),
    }

    for likelihood in BugLikelihood:
        if likelihood not in by_likelihood:
            continue

        title, description = priority_descriptions[likelihood]
        report.append(f"\n## {title}\n\n")
        report.append(f"*{description}*\n\n")

        for group in by_likelihood[likelihood]:
            report.append(f"### Group #{group.group_num}: {group.failure_type}\n\n")
            report.append(f"**Occurrences:** {group.occurrences} test failures  \n")
            report.append(f"**Failure Type:** `{group.failure_type}`  \n\n")

            report.append("**Failure Message:**\n```\n")
            # Truncate very long messages
            msg = group.failure_message
            if len(msg) > 500:
                msg = msg[:500] + "\n... (truncated)"
            report.append(f"{msg}\n")
            report.append("```\n\n")

            # Show first few affected tests as examples
            report.append("**Example Affected Tests:**\n")
            for test in group.affected_tests[:3]:
                report.append(f"- `{test}`\n")
            if len(group.affected_tests) > 3:
                report.append(f"- ... and {len(group.affected_tests) - 3} more\n")
            report.append("\n")

            # Add stack trace preview (just first line)
            first_line = group.stack_trace.split('\n')[0] if group.stack_trace else ""
            if first_line:
                report.append(f"**Stack Trace:** `{first_line}`\n\n")

            report.append("---\n\n")

    return ''.join(report)


def main():
    import sys

    input_file = sys.argv[1] if len(sys.argv) > 1 else 'restart-test-output-20251203-143743_analysis_report.txt'
    output_file = sys.argv[2] if len(sys.argv) > 2 else 'Yarn_restart_inspection_list.md'

    print(f"Parsing {input_file}...")
    groups = parse_report(input_file)
    print(f"Found {len(groups)} failure groups")

    print(f"Generating prioritized report...")
    report = generate_prioritized_report(groups)

    with open(output_file, 'w') as f:
        f.write(report)

    print(f"Report written to {output_file}")

    # Print summary
    by_likelihood = {}
    for group in groups:
        likelihood, _ = group.get_priority_score()
        if likelihood not in by_likelihood:
            by_likelihood[likelihood] = []
        by_likelihood[likelihood].append(group)

    print("\nSummary:")
    for likelihood in BugLikelihood:
        if likelihood in by_likelihood:
            count = len(by_likelihood[likelihood])
            total_occurrences = sum(g.occurrences for g in by_likelihood[likelihood])
            print(f"  {likelihood.name}: {count} patterns ({total_occurrences} failures)")


if __name__ == '__main__':
    main()
