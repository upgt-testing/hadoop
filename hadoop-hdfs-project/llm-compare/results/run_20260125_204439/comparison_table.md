# LLM Test Transformation Comparison Results

**Run Date:** 2026-01-25 20:52:06

## Detailed Results

| Model | Test File | Response Time (s) | Input Tokens | Output Tokens | Cost ($) |
|-------|-----------|-------------------|--------------|---------------|----------|
| Claude Opus 4.5 | TestWebHdfsTokens.java | 20.87 | 7268 | 1682 | $0.2352 |
| GPT-5.2 | TestWebHdfsTokens.java | 47.23 | 5376 | 5707 | $0.2250 |
| Gemini 3 Pro | TestWebHdfsTokens.java | 87.09 | 6519 | 473 | $0.0556 |
| Claude Opus 4.5 | TestPermissionSymlinks.java | 20.69 | 4985 | 1754 | $0.2063 |
| GPT-5.2 | TestPermissionSymlinks.java | 35.31 | 3831 | 4078 | $0.1607 |
| Gemini 3 Pro | TestPermissionSymlinks.java | 82.70 | 4563 | 596 | $0.0445 |
| Claude Opus 4.5 | TestStickyBit.java | 20.01 | 6082 | 1626 | $0.2132 |
| GPT-5.2 | TestStickyBit.java | 40.33 | 4660 | 5238 | $0.2037 |
| Gemini 3 Pro | TestStickyBit.java | 88.30 | 5437 | 481 | $0.0482 |

## Summary Statistics

| Model | Total Cost ($) | Avg Response Time (s) | Total Input Tokens | Total Output Tokens |
|-------|----------------|----------------------|-------------------|---------------------|
| Claude Opus 4.5 | $0.6547 | 20.53 | 18335 | 5062 |
| GPT-5.2 | $0.5894 | 40.96 | 13867 | 15023 |
| Gemini 3 Pro | $0.1482 | 86.03 | 16519 | 1550 |