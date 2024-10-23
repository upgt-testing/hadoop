#!/bin/bash

# Input file containing the list of test files
test_list="test_list.txt"

# Output file
output_file="line_changes.csv"

# Initialize the output file with headers
echo "test_name,#line_of_changes" > "$output_file"

# Loop through each file listed in the test_list.txt
while IFS= read -r file; do
    # Get the number of added and deleted lines for the specific file
   changes=$(echo $(git diff --numstat bac477b5aa3c50908b8126430b282be9c3d24506 95b822aaa2cbbf31eabcf2f57ea38b70b4bdba79 -- $(find . -name "${file}.java")) | awk '{added+=$1; deleted+=$2} END {print added+deleted}')

  # If changes is empty or null (file not found in diff), set it to 0
  if [ -z "$changes" ]; then
    changes=0
  fi

  # Write the file name and total number of changes to the output file
  echo "$changes" >> "$output_file"
done < "$test_list"

# Notify the user that the results are saved
echo "Results saved to $output_file"
