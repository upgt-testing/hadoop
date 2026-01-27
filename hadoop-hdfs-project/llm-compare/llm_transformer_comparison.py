#!/usr/bin/env python3
"""
LLM Test Transformation Comparison Tool

Compares Claude Opus 4.5, GPT-5.2, and Gemini 3 Pro on transforming
HDFS tests into restart-aware versions.
"""

import os
import sys
import json
import time
import csv
import argparse
from datetime import datetime
from pathlib import Path
from typing import Dict, Any, Optional, List

# API clients
import anthropic
import openai
import requests  # For Gemini REST API

# Configuration - Load API keys from file
API_KEYS_FILE = Path(__file__).parent / "api_keys.json"

def load_api_keys() -> Dict[str, str]:
    """Load API keys from api_keys.json file."""
    if API_KEYS_FILE.exists():
        with open(API_KEYS_FILE, "r") as f:
            return json.load(f)
    # Fallback to environment variables
    return {
        "claude": os.environ.get("ANTHROPIC_API_KEY", ""),
        "openai": os.environ.get("OPENAI_API_KEY", ""),
        "gemini": os.environ.get("GEMINI_API_KEY", ""),
    }

API_KEYS = load_api_keys()

MODELS = {
    "claude": "claude-opus-4-5-20251101",
    "openai": "gpt-5.2",
    "gemini": "gemini-3-pro-preview",
}

# Pricing per 1M tokens (USD)
PRICING = {
    "claude": {"input": 15.0, "output": 75.0},
    "openai": {"input": 10.0, "output": 30.0},  # Placeholder pricing for GPT-5.2
    "gemini": {"input": 7.0, "output": 21.0},   # Placeholder pricing for Gemini 3 Pro
}

# Paths
BASE_DIR = Path(__file__).parent
TESTS_DIR = BASE_DIR / "hdfs-tests"
RESULTS_DIR = BASE_DIR / "results"

# Transformation prompt
TRANSFORMATION_PROMPT = """Given a cluster test, create a restart-aware test that preserves
the original workload and oracles, while injecting multiple graceful
restart points using the unified restart API.

Unified restart API (must be used verbatim):
Palingenesis.at("position_label").on("cluster_variable").restart("node_role").withIndex(<idx>).execute();

Guidelines:
1. Identify appropriate restart points in the test (e.g., after cluster setup, before/after key operations)
2. Add the Palingenesis import statement
3. Insert restart calls at strategic locations that would test restart resilience
4. Preserve all original test logic and assertions
5. Use descriptive position labels (e.g., "after_cluster_setup", "before_token_operation")

Output ONLY a unified diff that makes the test compilable. The diff should:
- Start with --- a/original_file and +++ b/modified_file headers
- Include proper @@ line number hunks
- Be directly applicable with `patch` command

Here is the test code to transform:

```java
{test_code}
```

Output the diff now:"""


def read_test_file(filepath: Path) -> str:
    """Read a Java test file and return its contents."""
    with open(filepath, "r", encoding="utf-8") as f:
        return f.read()


def call_claude_api(prompt: str, test_code: str) -> Dict[str, Any]:
    """Call Claude API and return response with metrics."""
    client = anthropic.Anthropic(api_key=API_KEYS["claude"])

    full_prompt = prompt.format(test_code=test_code)

    start_time = time.time()
    response = client.messages.create(
        model=MODELS["claude"],
        max_tokens=8192,
        messages=[
            {"role": "user", "content": full_prompt}
        ]
    )
    end_time = time.time()

    # Extract metrics
    input_tokens = response.usage.input_tokens
    output_tokens = response.usage.output_tokens
    response_text = response.content[0].text

    # Calculate cost
    cost = (input_tokens * PRICING["claude"]["input"] / 1_000_000 +
            output_tokens * PRICING["claude"]["output"] / 1_000_000)

    return {
        "response": response_text,
        "response_time": end_time - start_time,
        "input_tokens": input_tokens,
        "output_tokens": output_tokens,
        "cost": cost,
        "model": MODELS["claude"],
    }


def call_openai_api(prompt: str, test_code: str) -> Dict[str, Any]:
    """Call OpenAI API and return response with metrics."""
    client = openai.OpenAI(api_key=API_KEYS["openai"])

    full_prompt = prompt.format(test_code=test_code)

    start_time = time.time()
    response = client.chat.completions.create(
        model=MODELS["openai"],
        messages=[
            {"role": "user", "content": full_prompt}
        ]
    )
    end_time = time.time()

    # Extract metrics
    input_tokens = response.usage.prompt_tokens
    output_tokens = response.usage.completion_tokens
    response_text = response.choices[0].message.content

    # Calculate cost
    cost = (input_tokens * PRICING["openai"]["input"] / 1_000_000 +
            output_tokens * PRICING["openai"]["output"] / 1_000_000)

    return {
        "response": response_text,
        "response_time": end_time - start_time,
        "input_tokens": input_tokens,
        "output_tokens": output_tokens,
        "cost": cost,
        "model": MODELS["openai"],
    }


def call_gemini_api(prompt: str, test_code: str) -> Dict[str, Any]:
    """Call Gemini API using REST endpoint and return response with metrics."""
    full_prompt = prompt.format(test_code=test_code)

    # Gemini REST API endpoint - use v1beta for preview models
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{MODELS['gemini']}:generateContent"

    headers = {
        "Content-Type": "application/json",
    }

    payload = {
        "contents": [
            {
                "parts": [
                    {"text": full_prompt}
                ]
            }
        ],
        "generationConfig": {
            "maxOutputTokens": 8192,
        }
    }

    start_time = time.time()
    response = requests.post(
        url,
        headers=headers,
        params={"key": API_KEYS["gemini"]},
        json=payload,
        timeout=300
    )
    end_time = time.time()

    response.raise_for_status()
    result = response.json()

    # Extract response text
    response_text = result["candidates"][0]["content"]["parts"][0]["text"]

    # Extract token usage from usageMetadata
    usage = result.get("usageMetadata", {})
    input_tokens = usage.get("promptTokenCount", 0)
    output_tokens = usage.get("candidatesTokenCount", 0)

    # Calculate cost
    cost = (input_tokens * PRICING["gemini"]["input"] / 1_000_000 +
            output_tokens * PRICING["gemini"]["output"] / 1_000_000)

    return {
        "response": response_text,
        "response_time": end_time - start_time,
        "input_tokens": input_tokens,
        "output_tokens": output_tokens,
        "cost": cost,
        "model": MODELS["gemini"],
    }


API_CALLERS = {
    "claude": call_claude_api,
    "openai": call_openai_api,
    "gemini": call_gemini_api,
}

# Track last call time per model for rate limiting
LAST_CALL_TIME = {
    "claude": 0,
    "openai": 0,
    "gemini": 0,
}

# Wait time between calls to the same model (seconds)
MODEL_WAIT_TIME = 60


def extract_diff(response_text: str) -> str:
    """Extract diff content from LLM response."""
    # Try to find diff content between code blocks
    if "```diff" in response_text:
        start = response_text.find("```diff") + 7
        end = response_text.find("```", start)
        if end > start:
            return response_text[start:end].strip()

    # Try generic code block
    if "```" in response_text:
        start = response_text.find("```") + 3
        # Skip language identifier if present
        newline = response_text.find("\n", start)
        if newline > start and newline - start < 20:
            start = newline + 1
        end = response_text.find("```", start)
        if end > start:
            return response_text[start:end].strip()

    # Return full response if no code blocks found
    return response_text.strip()


def save_diff(diff_content: str, output_path: Path) -> None:
    """Save diff content to a file."""
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(diff_content)


def generate_metrics_csv(results: list, output_path: Path) -> None:
    """Generate CSV file with all metrics."""
    fieldnames = ["model", "test_file", "response_time_s", "input_tokens",
                  "output_tokens", "cost_usd"]

    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()

        for result in results:
            writer.writerow({
                "model": result["model_name"],
                "test_file": result["test_file"],
                "response_time_s": f"{result['response_time']:.2f}",
                "input_tokens": result["input_tokens"],
                "output_tokens": result["output_tokens"],
                "cost_usd": f"{result['cost']:.4f}",
            })


def generate_comparison_table(results: list, output_path: Path) -> None:
    """Generate markdown comparison table."""
    lines = [
        "# LLM Test Transformation Comparison Results",
        "",
        f"**Run Date:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}",
        "",
        "## Detailed Results",
        "",
        "| Model | Test File | Response Time (s) | Input Tokens | Output Tokens | Cost ($) |",
        "|-------|-----------|-------------------|--------------|---------------|----------|",
    ]

    for result in results:
        lines.append(
            f"| {result['model_name']} | {result['test_file']} | "
            f"{result['response_time']:.2f} | {result['input_tokens']} | "
            f"{result['output_tokens']} | ${result['cost']:.4f} |"
        )

    # Summary statistics by model
    lines.extend(["", "## Summary Statistics", ""])

    model_stats = {}
    for result in results:
        model = result["model_name"]
        if model not in model_stats:
            model_stats[model] = {
                "total_cost": 0,
                "total_time": 0,
                "total_input_tokens": 0,
                "total_output_tokens": 0,
                "count": 0,
            }
        model_stats[model]["total_cost"] += result["cost"]
        model_stats[model]["total_time"] += result["response_time"]
        model_stats[model]["total_input_tokens"] += result["input_tokens"]
        model_stats[model]["total_output_tokens"] += result["output_tokens"]
        model_stats[model]["count"] += 1

    lines.extend([
        "| Model | Total Cost ($) | Avg Cost/File ($) | Avg Response Time (s) | Total Input Tokens | Total Output Tokens |",
        "|-------|----------------|-------------------|----------------------|-------------------|---------------------|",
    ])

    for model, stats in model_stats.items():
        avg_time = stats["total_time"] / stats["count"] if stats["count"] > 0 else 0
        avg_cost = stats["total_cost"] / stats["count"] if stats["count"] > 0 else 0
        lines.append(
            f"| {model} | ${stats['total_cost']:.4f} | ${avg_cost:.4f} | {avg_time:.2f} | "
            f"{stats['total_input_tokens']} | {stats['total_output_tokens']} |"
        )

    with open(output_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))


def save_raw_responses(results: list, output_path: Path) -> None:
    """Save raw API responses to JSON file."""
    raw_data = []
    for result in results:
        raw_data.append({
            "model": result["model_name"],
            "test_file": result["test_file"],
            "response": result["raw_response"],
            "metrics": {
                "response_time": result["response_time"],
                "input_tokens": result["input_tokens"],
                "output_tokens": result["output_tokens"],
                "cost": result["cost"],
            }
        })

    with open(output_path, "w", encoding="utf-8") as f:
        json.dump(raw_data, f, indent=2)


def run_comparison(max_tests: int = 0, models: List[str] = None) -> None:
    """Run the full comparison across all models and test files.

    Args:
        max_tests: Maximum number of test files to process (0 = all)
        models: List of model keys to use (None = all)
    """
    # Create timestamped output directory
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    run_dir = RESULTS_DIR / f"run_{timestamp}"
    diffs_dir = run_dir / "diffs"
    diffs_dir.mkdir(parents=True, exist_ok=True)

    print(f"Output directory: {run_dir}")
    print("=" * 60)

    # Get test files
    test_files = list(TESTS_DIR.glob("*.java"))
    if max_tests > 0:
        test_files = test_files[:max_tests]
    print(f"Processing {len(test_files)} test file(s)")

    # Filter models if specified
    if models is None:
        models = list(API_CALLERS.keys())

    all_results = []

    for test_file in test_files:
        test_name = test_file.stem
        test_code = read_test_file(test_file)
        print(f"\nProcessing: {test_file.name}")
        print("-" * 40)

        for model_key in models:
            if model_key not in API_CALLERS:
                print(f"  Skipping unknown model: {model_key}")
                continue
            caller = API_CALLERS[model_key]
            model_display = {
                "claude": "Claude Opus 4.5",
                "openai": "GPT-5.2",
                "gemini": "Gemini 3 Pro",
            }[model_key]

            # Wait if needed to respect rate limits
            elapsed = time.time() - LAST_CALL_TIME[model_key]
            if LAST_CALL_TIME[model_key] > 0 and elapsed < MODEL_WAIT_TIME:
                wait_time = MODEL_WAIT_TIME - elapsed
                print(f"  Waiting {wait_time:.0f}s before calling {model_display}...", flush=True)
                time.sleep(wait_time)

            print(f"  Calling {model_display}...", end=" ", flush=True)

            try:
                result = caller(TRANSFORMATION_PROMPT, test_code)
                LAST_CALL_TIME[model_key] = time.time()

                # Extract and save diff
                diff_content = extract_diff(result["response"])
                diff_filename = f"{model_key}_{test_name}.diff"
                save_diff(diff_content, diffs_dir / diff_filename)

                # Store results
                all_results.append({
                    "model_name": model_display,
                    "model_key": model_key,
                    "test_file": test_file.name,
                    "response_time": result["response_time"],
                    "input_tokens": result["input_tokens"],
                    "output_tokens": result["output_tokens"],
                    "cost": result["cost"],
                    "raw_response": result["response"],
                })

                print(f"Done ({result['response_time']:.2f}s, ${result['cost']:.4f})")

            except Exception as e:
                print(f"Error: {e}")
                all_results.append({
                    "model_name": model_display,
                    "model_key": model_key,
                    "test_file": test_file.name,
                    "response_time": 0,
                    "input_tokens": 0,
                    "output_tokens": 0,
                    "cost": 0,
                    "raw_response": f"ERROR: {str(e)}",
                })

    # Generate output files
    print("\n" + "=" * 60)
    print("Generating output files...")

    generate_metrics_csv(all_results, run_dir / "metrics.csv")
    print(f"  Created: metrics.csv")

    generate_comparison_table(all_results, run_dir / "comparison_table.md")
    print(f"  Created: comparison_table.md")

    save_raw_responses(all_results, run_dir / "raw_responses.json")
    print(f"  Created: raw_responses.json")

    print(f"\nDiff files saved to: {diffs_dir}")
    print(f"Total diff files: {len(list(diffs_dir.glob('*.diff')))}")

    # Print summary
    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)

    model_totals = {}
    for result in all_results:
        model = result["model_name"]
        if model not in model_totals:
            model_totals[model] = {"cost": 0, "time": 0, "count": 0}
        model_totals[model]["cost"] += result["cost"]
        model_totals[model]["time"] += result["response_time"]
        model_totals[model]["count"] += 1

    for model, totals in model_totals.items():
        avg_time = totals["time"] / totals["count"] if totals["count"] > 0 else 0
        print(f"{model}:")
        print(f"  Total Cost: ${totals['cost']:.4f}")
        print(f"  Avg Response Time: {avg_time:.2f}s")

    print(f"\nResults saved to: {run_dir}")


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description="LLM Test Transformation Comparison Tool"
    )
    parser.add_argument(
        "-n", "--num-tests",
        type=int,
        default=0,
        help="Number of test files to process (0 = all, default: 0)"
    )
    parser.add_argument(
        "-m", "--models",
        type=str,
        default="claude,openai,gemini",
        help="Comma-separated list of models to use (default: claude,openai,gemini)"
    )
    parser.add_argument(
        "--test",
        action="store_true",
        help="Quick test mode: run 1 test file with all models"
    )
    args = parser.parse_args()

    print("LLM Test Transformation Comparison Tool")
    print("=" * 60)

    # Validate API keys
    missing_keys = []
    for name, key in API_KEYS.items():
        if not key:
            missing_keys.append(name.upper())

    if missing_keys:
        print("\nWARNING: Missing API keys for:", ", ".join(missing_keys))
        print(f"Add keys to {API_KEYS_FILE} or set environment variables.")
        print("\nContinuing with available APIs...\n")
    else:
        print(f"API keys loaded from: {API_KEYS_FILE}")

    # Check test files exist
    if not TESTS_DIR.exists():
        print(f"ERROR: Test directory not found: {TESTS_DIR}")
        return

    test_files = list(TESTS_DIR.glob("*.java"))
    if not test_files:
        print(f"ERROR: No Java test files found in: {TESTS_DIR}")
        return

    print(f"Test files directory: {TESTS_DIR}")
    print(f"Test files found: {[f.name for f in test_files]}")
    print()

    # Parse options
    max_tests = 1 if args.test else args.num_tests
    models = [m.strip() for m in args.models.split(",")]

    if args.test:
        print("*** TEST MODE: Running 1 test file only ***\n")

    run_comparison(max_tests=max_tests, models=models)


if __name__ == "__main__":
    main()
