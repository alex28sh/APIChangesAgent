#!/bin/bash

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "Error: jq is required but not installed. Please install jq first."
    echo "You can install it using: sudo apt-get install jq (for Debian/Ubuntu)"
    exit 1
fi

# Check if input file is provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <input_json_file> [output_json_file]"
    echo "If output_json_file is not provided, the filtered result will be printed to stdout."
    exit 1
fi

INPUT_FILE="$1"
OUTPUT_FILE="$2"

# Check if input file exists
if [ ! -f "$INPUT_FILE" ]; then
    echo "Error: Input file '$INPUT_FILE' does not exist."
    exit 1
fi

# Filter the JSON to keep only entries where test_program is not "INCORRECT TEST"
FILTER_CMD='[.[] | select(.test_program != "INCORRECT TEST")]'

if [ -n "$OUTPUT_FILE" ]; then
    # Output to file
    jq "$FILTER_CMD" "$INPUT_FILE" > "$OUTPUT_FILE"
    echo "Filtered data has been written to '$OUTPUT_FILE'."
    echo "Total entries in filtered output: $(jq 'length' "$OUTPUT_FILE")"
else
    # Output to stdout
    jq "$FILTER_CMD" "$INPUT_FILE"
fi
