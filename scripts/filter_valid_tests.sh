#!/bin/bash

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "Error: jq is required but not installed. Please install jq first."
    echo "You can install it using: sudo apt-get install jq (for Debian/Ubuntu)"
    exit 1
fi

# Display help information
show_help() {
    echo "Usage: $0 <input_json_file> [output_json_file] [options]"
    echo "Filters JSON data to include only entries with valid test programs."
    echo ""
    echo "Options:"
    echo "  -h, --help                 Display this help message"
    echo "  -f, --field FIELD          Specify the field to filter on (default: test_program)"
    echo "  -v, --value VALUE          Specify the value to exclude (default: INCORRECT TEST)"
    echo "  -i, --include              Include entries matching the value instead of excluding them"
    echo "  -s, --stats                Show detailed statistics about the filtered data"
    echo ""
    echo "Examples:"
    echo "  $0 data.json               # Filter out entries with test_program = INCORRECT TEST"
    echo "  $0 data.json output.json   # Save filtered results to output.json"
    echo "  $0 data.json -f type -v method -i  # Include only entries where type = method"
    exit 0
}

# Default values
FIELD="test_program"
VALUE="INCORRECT TEST"
INCLUDE=false
SHOW_STATS=false

# Parse command line arguments
INPUT_FILE=""
OUTPUT_FILE=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            show_help
            ;;
        -f|--field)
            FIELD="$2"
            shift 2
            ;;
        -v|--value)
            VALUE="$2"
            shift 2
            ;;
        -i|--include)
            INCLUDE=true
            shift
            ;;
        -s|--stats)
            SHOW_STATS=true
            shift
            ;;
        *)
            if [ -z "$INPUT_FILE" ]; then
                INPUT_FILE="$1"
            elif [ -z "$OUTPUT_FILE" ]; then
                OUTPUT_FILE="$1"
            else
                echo "Error: Unexpected argument: $1"
                show_help
            fi
            shift
            ;;
    esac
done

# Check if input file is provided
if [ -z "$INPUT_FILE" ]; then
    echo "Error: Input file is required."
    show_help
fi

# Check if input file exists
if [ ! -f "$INPUT_FILE" ]; then
    echo "Error: Input file '$INPUT_FILE' does not exist."
    exit 1
fi

# Construct the filter command based on include/exclude option
if [ "$INCLUDE" = true ]; then
    FILTER_CMD="[.[] | select(.$FIELD == \"$VALUE\")]"
else
    FILTER_CMD="[.[] | select(.$FIELD != \"$VALUE\")]"
fi

# Process the data
if [ -n "$OUTPUT_FILE" ]; then
    # Output to file
    jq "$FILTER_CMD" "$INPUT_FILE" > "$OUTPUT_FILE"
    echo "Filtered data has been written to '$OUTPUT_FILE'."
    
    # Show statistics
    FILTERED_COUNT=$(jq 'length' "$OUTPUT_FILE")
    TOTAL_COUNT=$(jq 'length' "$INPUT_FILE")
    EXCLUDED_COUNT=$((TOTAL_COUNT - FILTERED_COUNT))
    
    echo "Total entries in original file: $TOTAL_COUNT"
    echo "Total entries in filtered output: $FILTERED_COUNT"
    echo "Entries excluded: $EXCLUDED_COUNT"
    
    if [ "$SHOW_STATS" = true ]; then
        echo "Detailed statistics:"
        if [ "$INCLUDE" = true ]; then
            echo "Entries where $FIELD = \"$VALUE\": $FILTERED_COUNT"
        else
            echo "Entries where $FIELD != \"$VALUE\": $FILTERED_COUNT"
            echo "Entries where $FIELD = \"$VALUE\": $EXCLUDED_COUNT"
        fi
        
        # Show additional statistics if available
        if [ "$FIELD" = "test_program" ]; then
            echo "Libraries represented in filtered output:"
            jq 'group_by(.library) | map({library: .[0].library, count: length}) | sort_by(.count) | reverse' "$OUTPUT_FILE"
        fi
    fi
else
    # Output to stdout
    jq "$FILTER_CMD" "$INPUT_FILE"
fi