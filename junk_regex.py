import re

def extract_parts_from_list(data_list, regex_pattern, extract_type=None, part=None):
    """
    Extract specific parts of items in a list based on a regex pattern, 
    extraction type (digits or characters), and a specified part (prefix, suffix, or middle).
    If `extract_type` and `part` are None, return items matching the regex pattern exactly.

    Args:
        data_list (list): List of strings to process.
        regex_pattern (str): Regex pattern to filter and extract from the list.
        extract_type (str): 'digits' or 'characters' to extract only numeric or alphabetic parts.
        part (str): Which part to extract - 'prefix', 'suffix', or 'middle'.

    Returns:
        list: List of extracted or filtered items.
    """
    result = []
    pattern = re.compile(regex_pattern)
    
    for item in data_list:
        match = pattern.match(item)
        if not match:
            continue  # Skip items that don't match the pattern

        # If extract_type and part are None, return items matching the pattern exactly
        if extract_type is None and part is None:
            result.append(item)
            continue
        
        # Extract specific parts based on the part argument
        extracted_part = None
        if part == "prefix":
            extracted_part = match.group(1) if match.lastindex >= 1 else None
        elif part == "suffix":
            extracted_part = match.group(2) if match.lastindex >= 2 else None
        elif part == "middle" and match.lastindex >= 3:
            extracted_part = match.group(3)
        
        # Apply extract_type filter (digits or characters) if applicable
        if extract_type == "digits" and extracted_part:
            extracted_part = ''.join(filter(str.isdigit, extracted_part))
        elif extract_type == "characters" and extracted_part:
            extracted_part = ''.join(filter(str.isalpha, extracted_part))
        
        # Append the extracted part if valid
        if extracted_part:
            result.append(extracted_part)

    return result

# Example Usage
example_list = [
    "20240506_static_data",
    "random_key",
    "20240507_dynamic_data",
    "prefix_middle_suffix",
    "20240506_summary",
    "4875_other_data"
]

# 1. Extract suffixes from items starting with 8-digit dates
suffixes = extract_parts_from_list(
    example_list,
    regex_pattern=r"^(\d{8})_(.+)$",
    extract_type=None,
    part="suffix"
)

# 2. Extract prefixes (dates) from items starting with 8-digit dates
prefixes = extract_parts_from_list(
    example_list,
    regex_pattern=r"^(\d{8})_(.+)$",
    extract_type="digits",
    part="prefix"
)

# 3. Extract middle parts from items matching "prefix_middle_suffix" format
middle_parts = extract_parts_from_list(
    example_list,
    regex_pattern=r"^(\w+)_([a-z]+)_(.+)$",
    extract_type="characters",
    part="middle"
)

# 4. Return items matching the regex exactly
exact_matches = extract_parts_from_list(
    example_list,
    regex_pattern=r"^20240506_(.+)$"
)

print("Suffixes:", suffixes)
print("Prefixes:", prefixes)
print("Middle Parts:", middle_parts)
print("Exact Matches:", exact_matches)
