#!/usr/bin/env python3
"""
Generate Failure Reports for YARN Restart Tests

This script generates various report formats from the analysis:
- Markdown report
- CSV report
- HTML report
"""

import json
import csv
from pathlib import Path
from typing import Dict, List
from datetime import datetime


class ReportGenerator:
    """Generate various report formats from test analysis"""

    def __init__(self, analysis_file: str):
        self.analysis_file = Path(analysis_file)
        with open(self.analysis_file, 'r') as f:
            self.analysis = json.load(f)

    def generate_markdown_report(self, output_file: str):
        """Generate a Markdown report"""
        with open(output_file, 'w') as f:
            f.write("# YARN Restart Test Analysis Report\n\n")
            f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n\n")

            # Summary section
            summary = self.analysis['summary']
            f.write("## Summary\n\n")
            f.write(f"- **Modules Analyzed**: {summary['modulesAnalyzed']}\n")
            f.write(f"- **Unique Test Methods**: {summary['uniqueTestMethods']}\n")
            f.write(f"- **Total Test Executions**: {summary['totalExecutions']}\n")
            f.write(f"- **Passed**: {summary['passedExecutions']} ✓\n")
            f.write(f"- **Failed**: {summary['failedExecutions']} ✗\n")
            f.write(f"- **Pass Rate**: {summary['passRate']:.2f}%\n")
            f.write(f"- **Total Duration**: {summary['totalDurationMinutes']:.2f} minutes\n")
            f.write(f"- **Unique Failure Patterns**: {summary['uniqueFailurePatterns']}\n\n")

            # Module breakdown
            f.write("## Module Breakdown\n\n")
            f.write("| Module | Total | Passed | Failed | Pass Rate |\n")
            f.write("|--------|-------|--------|--------|----------|\n")

            module_summaries = self.analysis['moduleSummaries']
            for module_name in sorted(module_summaries.keys()):
                module = module_summaries[module_name]
                total = module['totalTests']
                passed = module['passed']
                failed = module['failed']
                pass_rate = module['passRate']
                status = "✓" if failed == 0 else "✗"

                f.write(f"| {status} {module_name} | {total} | {passed} | {failed} | "
                       f"{pass_rate:.1f}% |\n")

            # Failure groups
            failure_groups = self.analysis['failureGroups']
            if failure_groups:
                f.write("\n## Failure Groups\n\n")
                f.write("Failures grouped by normalized stack trace (line numbers removed, "
                       "truncated after test method):\n\n")

                for i, group in enumerate(failure_groups, 1):
                    f.write(f"### Group #{i}: {group['exceptionType']}\n\n")
                    f.write(f"- **Failure Count**: {group['failureCount']}\n")
                    f.write(f"- **Affected Test Methods**: {len(group['affectedTestMethods'])}\n")
                    f.write(f"- **Affected Modules**: {', '.join(group['affectedModules'])}\n\n")

                    f.write("#### Affected Test Methods\n\n")
                    for method in group['affectedTestMethods']:
                        f.write(f"- `{method}`\n")

                    f.write("\n#### Failure Scenarios\n\n")
                    f.write("| Test Method | Position | Target | Mode | Module |\n")
                    f.write("|-------------|----------|--------|------|--------|\n")

                    for failure in group['failures']:
                        method = failure['testMethod']
                        position = failure['position'] or 'BASELINE'
                        target = failure['target'] or '-'
                        mode = failure['mode'] or '-'
                        module = failure['module']

                        f.write(f"| {method} | {position} | {target} | {mode} | {module} |\n")

                    f.write("\n#### Normalized Stack Trace\n\n")
                    f.write("```\n")
                    f.write(group['normalizedStackTrace'])
                    f.write("\n```\n\n")

                    # Show one full example stacktrace
                    if group['failures']:
                        first_failure = group['failures'][0]
                        if first_failure.get('stackTrace'):
                            f.write("#### Example Full Stack Trace\n\n")
                            f.write("From: `{}.{}`\n\n".format(
                                first_failure['testClass'],
                                first_failure['testMethod']
                            ))
                            f.write("```\n")
                            f.write(first_failure['stackTrace'])
                            f.write("\n```\n\n")

                    f.write("---\n\n")
            else:
                f.write("\n## Failure Groups\n\n")
                f.write("✓ **No failures detected** - All tests passed!\n\n")

        print(f"✓ Generated Markdown report: {output_file}")

    def generate_csv_report(self, output_file: str):
        """Generate a CSV report of all failures"""
        all_failures = self.analysis['allFailures']

        if not all_failures:
            print("⚠ No failures to write to CSV")
            return

        with open(output_file, 'w', newline='') as f:
            fieldnames = [
                'module',
                'testClass',
                'testMethod',
                'position',
                'target',
                'mode',
                'isBaseline',
                'durationMs',
                'errorMessage',
                'exceptionType'
            ]

            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()

            for failure in all_failures:
                # Extract exception type from error message or stacktrace
                exception_type = self._extract_exception_type(
                    failure.get('stackTrace') or failure.get('errorMessage') or ""
                )

                writer.writerow({
                    'module': failure['module'],
                    'testClass': failure['testClass'],
                    'testMethod': failure['testMethod'],
                    'position': failure.get('position') or 'BASELINE',
                    'target': failure.get('target') or '',
                    'mode': failure.get('mode') or '',
                    'isBaseline': failure['isBaseline'],
                    'durationMs': failure['durationMs'],
                    'errorMessage': (failure.get('errorMessage') or '')[:200],  # Truncate
                    'exceptionType': exception_type
                })

        print(f"✓ Generated CSV report: {output_file}")

    def generate_html_report(self, output_file: str):
        """Generate an HTML report"""
        summary = self.analysis['summary']
        module_summaries = self.analysis['moduleSummaries']
        failure_groups = self.analysis['failureGroups']

        html = f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>YARN Restart Test Analysis</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Arial, sans-serif;
            line-height: 1.6;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            background: #f5f5f5;
        }}
        .container {{
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
        }}
        h1 {{
            color: #333;
            border-bottom: 3px solid #0066cc;
            padding-bottom: 10px;
        }}
        h2 {{
            color: #444;
            margin-top: 30px;
            border-bottom: 2px solid #ddd;
            padding-bottom: 8px;
        }}
        h3 {{
            color: #555;
            margin-top: 20px;
        }}
        .summary-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin: 20px 0;
        }}
        .summary-card {{
            background: #f8f9fa;
            padding: 15px;
            border-radius: 6px;
            border-left: 4px solid #0066cc;
        }}
        .summary-card.success {{
            border-left-color: #28a745;
        }}
        .summary-card.failure {{
            border-left-color: #dc3545;
        }}
        .summary-card .label {{
            font-size: 0.9em;
            color: #666;
            margin-bottom: 5px;
        }}
        .summary-card .value {{
            font-size: 1.8em;
            font-weight: bold;
            color: #333;
        }}
        table {{
            width: 100%;
            border-collapse: collapse;
            margin: 15px 0;
            background: white;
        }}
        th {{
            background: #0066cc;
            color: white;
            padding: 12px;
            text-align: left;
            font-weight: 600;
        }}
        td {{
            padding: 10px 12px;
            border-bottom: 1px solid #ddd;
        }}
        tr:hover {{
            background: #f8f9fa;
        }}
        .status-pass {{
            color: #28a745;
            font-weight: bold;
        }}
        .status-fail {{
            color: #dc3545;
            font-weight: bold;
        }}
        .failure-group {{
            background: #fff3cd;
            border: 1px solid #ffc107;
            border-radius: 6px;
            padding: 20px;
            margin: 20px 0;
        }}
        .failure-group h3 {{
            margin-top: 0;
            color: #856404;
        }}
        .stacktrace {{
            background: #f8f9fa;
            border: 1px solid #ddd;
            border-radius: 4px;
            padding: 15px;
            overflow-x: auto;
            font-family: 'Courier New', monospace;
            font-size: 0.9em;
            white-space: pre-wrap;
            word-wrap: break-word;
        }}
        .badge {{
            display: inline-block;
            padding: 4px 8px;
            border-radius: 3px;
            font-size: 0.85em;
            font-weight: 600;
        }}
        .badge-success {{
            background: #d4edda;
            color: #155724;
        }}
        .badge-danger {{
            background: #f8d7da;
            color: #721c24;
        }}
        .no-failures {{
            background: #d4edda;
            border: 1px solid #28a745;
            border-radius: 6px;
            padding: 20px;
            text-align: center;
            color: #155724;
            font-size: 1.1em;
            margin: 20px 0;
        }}
    </style>
</head>
<body>
    <div class="container">
        <h1>YARN Restart Test Analysis Report</h1>
        <p><strong>Generated:</strong> {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</p>

        <h2>Summary</h2>
        <div class="summary-grid">
            <div class="summary-card">
                <div class="label">Modules Analyzed</div>
                <div class="value">{summary['modulesAnalyzed']}</div>
            </div>
            <div class="summary-card">
                <div class="label">Test Methods</div>
                <div class="value">{summary['uniqueTestMethods']}</div>
            </div>
            <div class="summary-card">
                <div class="label">Total Executions</div>
                <div class="value">{summary['totalExecutions']}</div>
            </div>
            <div class="summary-card success">
                <div class="label">Passed</div>
                <div class="value">{summary['passedExecutions']}</div>
            </div>
            <div class="summary-card failure">
                <div class="label">Failed</div>
                <div class="value">{summary['failedExecutions']}</div>
            </div>
            <div class="summary-card">
                <div class="label">Pass Rate</div>
                <div class="value">{summary['passRate']:.1f}%</div>
            </div>
            <div class="summary-card">
                <div class="label">Duration</div>
                <div class="value">{summary['totalDurationMinutes']:.1f}<span style="font-size:0.5em">min</span></div>
            </div>
            <div class="summary-card">
                <div class="label">Failure Patterns</div>
                <div class="value">{summary['uniqueFailurePatterns']}</div>
            </div>
        </div>

        <h2>Module Breakdown</h2>
        <table>
            <thead>
                <tr>
                    <th>Module</th>
                    <th>Total Tests</th>
                    <th>Passed</th>
                    <th>Failed</th>
                    <th>Pass Rate</th>
                    <th>Status</th>
                </tr>
            </thead>
            <tbody>
"""

        for module_name in sorted(module_summaries.keys()):
            module = module_summaries[module_name]
            total = module['totalTests']
            passed = module['passed']
            failed = module['failed']
            pass_rate = module['passRate']

            if failed == 0:
                status_badge = '<span class="badge badge-success">✓ PASS</span>'
            else:
                status_badge = '<span class="badge badge-danger">✗ FAIL</span>'

            html += f"""
                <tr>
                    <td><strong>{module_name}</strong></td>
                    <td>{total}</td>
                    <td class="status-pass">{passed}</td>
                    <td class="status-fail">{failed}</td>
                    <td>{pass_rate:.1f}%</td>
                    <td>{status_badge}</td>
                </tr>
"""

        html += """
            </tbody>
        </table>
"""

        if failure_groups:
            html += "<h2>Failure Groups</h2>\n"
            html += "<p>Failures grouped by normalized stack trace (line numbers removed, truncated after test method):</p>\n"

            for i, group in enumerate(failure_groups, 1):
                html += f"""
        <div class="failure-group">
            <h3>Group #{i}: {self._escape_html(group['exceptionType'])}</h3>
            <p><strong>Failure Count:</strong> {group['failureCount']}<br>
            <strong>Affected Test Methods:</strong> {len(group['affectedTestMethods'])}<br>
            <strong>Affected Modules:</strong> {', '.join(group['affectedModules'])}</p>

            <h4>Affected Test Methods</h4>
            <ul>
"""
                for method in group['affectedTestMethods']:
                    html += f"                <li><code>{self._escape_html(method)}</code></li>\n"

                html += """
            </ul>

            <h4>Failure Scenarios</h4>
            <table>
                <thead>
                    <tr>
                        <th>Test Method</th>
                        <th>Position</th>
                        <th>Target</th>
                        <th>Mode</th>
                        <th>Module</th>
                    </tr>
                </thead>
                <tbody>
"""

                for failure in group['failures']:
                    method = self._escape_html(failure['testMethod'])
                    position = failure.get('position') or 'BASELINE'
                    target = failure.get('target') or '-'
                    mode = failure.get('mode') or '-'
                    module = failure['module']

                    html += f"""
                    <tr>
                        <td><code>{method}</code></td>
                        <td>{position}</td>
                        <td>{target}</td>
                        <td>{mode}</td>
                        <td>{module}</td>
                    </tr>
"""

                html += """
                </tbody>
            </table>

            <h4>Normalized Stack Trace</h4>
            <div class="stacktrace">"""
                html += self._escape_html(group['normalizedStackTrace'])
                html += """</div>
        </div>
"""
        else:
            html += """
        <div class="no-failures">
            <strong>✓ No failures detected - All tests passed!</strong>
        </div>
"""

        html += """
    </div>
</body>
</html>
"""

        with open(output_file, 'w') as f:
            f.write(html)

        print(f"✓ Generated HTML report: {output_file}")

    @staticmethod
    def _escape_html(text: str) -> str:
        """Escape HTML special characters"""
        if not text:
            return ""
        return (text
                .replace('&', '&amp;')
                .replace('<', '&lt;')
                .replace('>', '&gt;')
                .replace('"', '&quot;')
                .replace("'", '&#39;'))

    @staticmethod
    def _extract_exception_type(text: str) -> str:
        """Extract exception type from text"""
        import re
        if not text:
            return "Unknown"
        lines = text.split('\n')
        if lines:
            match = re.match(r'([a-zA-Z0-9_.]+(?:Exception|Error|Throwable))', lines[0].strip())
            if match:
                return match.group(1)
        return "Unknown"


def main():
    """Main entry point"""
    import sys
    from pathlib import Path

    # Find analysis file
    if len(sys.argv) > 1:
        analysis_file = sys.argv[1]
    else:
        # Look for ANALYSIS.json in most recent output directory
        current_dir = Path.cwd()
        output_dirs = sorted(current_dir.glob("restart-test-output-*"), reverse=True)

        if not output_dirs:
            print("Error: No restart-test-output-* directories found")
            print("Usage: python generate_failure_report.py [analysis_file]")
            sys.exit(1)

        analysis_file = output_dirs[0] / "ANALYSIS.json"

        if not analysis_file.exists():
            print(f"Error: {analysis_file} not found")
            print("Please run analyze_restart_tests.py first")
            sys.exit(1)

    print(f"Loading analysis from: {analysis_file}")

    # Generate reports
    generator = ReportGenerator(str(analysis_file))

    output_dir = Path(analysis_file).parent

    # Generate all report formats
    generator.generate_markdown_report(str(output_dir / "FAILURE_REPORT.md"))
    generator.generate_csv_report(str(output_dir / "FAILURES.csv"))
    generator.generate_html_report(str(output_dir / "FAILURE_REPORT.html"))

    print("\n✓ All reports generated successfully!")


if __name__ == "__main__":
    main()
