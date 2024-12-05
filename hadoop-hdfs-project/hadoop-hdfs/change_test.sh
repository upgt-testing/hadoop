#!/bin/bash

# Read the list of file names from test_list.txt
input_file="test_list.txt"

# Check if the input file exists
if [[ ! -f "$input_file" ]]; then
  echo "Error: File '$input_file' not found!"
  exit 1
fi

# Loop through each file name in the list
while IFS= read -r base_name; do
  # Construct the Java file name
  java_file="${base_name}.java"

  # Find the file in the current directory or subdirectories
  found_file=$(find . -name "$java_file")

  # If the file is found, perform the replacement
  if [[ -n "$found_file" ]]; then
    echo "Processing file: $found_file"
    # Use the correct syntax for sed with in-place editing
    sed -i '' 's/MiniDFSCluster/MiniDFSClusterInJVM/g' "$found_file"
  else
    echo "Warning: File '$java_file' not found."
  fi
done < "$input_file"

echo "Replacement complete."
