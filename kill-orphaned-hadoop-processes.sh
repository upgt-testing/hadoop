#!/bin/bash
#
# Kills all orphaned Hadoop processes (NameNode, DataNode, JournalNode)
# that may be left over from failed test runs.
#
# Usage: ./kill-orphaned-hadoop-processes.sh

echo "Searching for orphaned Hadoop processes..."

# Find all java processes running Hadoop components
PIDS=$(ps aux | grep java | grep -E "NameNode|DataNode|JournalNode" | grep -v grep | awk '{print $2}')

if [ -z "$PIDS" ]; then
  echo "No orphaned Hadoop processes found."
  exit 0
fi

echo "Found orphaned Hadoop processes:"
ps aux | grep java | grep -E "NameNode|DataNode|JournalNode" | grep -v grep

echo ""
echo "Killing PIDs: $PIDS"
kill -9 $PIDS

echo ""
echo "Verification - checking if processes are gone:"
sleep 1
REMAINING=$(ps aux | grep java | grep -E "NameNode|DataNode|JournalNode" | grep -v grep | wc -l)

if [ "$REMAINING" -eq 0 ]; then
  echo "✓ All orphaned Hadoop processes killed successfully"
else
  echo "⚠ Warning: Some processes may still be running"
  ps aux | grep java | grep -E "NameNode|DataNode|JournalNode" | grep -v grep
fi
