import re
from typing import List, Optional

def extract_parts_from_list(
    data_list: List[str], 
    regex_pattern: str, 
    extract_type: Optional[str] = None,  # 'digits' or 'characters'
    part: Optional[str] = None          # 'prefix', 'suffix', or 'middle'
) -> List[str]:
    """
    Extract specific parts of items in a list based on a regex pattern.
    
    Args:
        data_list (List[str]): List of strings to process.
        regex_pattern (str): Regex pattern to filter and extract from the list.
        extract_type (Optional[str]): 'digits' or 'characters' to extract numeric or alphabetic parts.
        part (Optional[str]): Which part to extract - 'prefix', 'suffix', or 'middle'.
        
    Returns:
        List[str]: List of extracted or filtered items.
    """
    # Validate inputs
    if extract_type not in {None, "digits", "characters"}:
        raise ValueError("extract_type must be 'digits', 'characters', or None.")
    if part not in {None, "prefix", "suffix", "middle"}:
        raise ValueError("part must be 'prefix', 'suffix', 'middle', or None.")

    # Compile the regex pattern for performance
    pattern = re.compile(regex_pattern)
    result = []

    for item in data_list:
        match = pattern.match(item)
        if not match:
            continue  # Skip items that don't match the regex

        # If no specific extraction is required, return items matching the regex exactly
        if extract_type is None and part is None:
            result.append(item)
            continue

        # Extract named group based on the `part`
        if part == "prefix":
            extracted_part = match.group("prefix") if "prefix" in pattern.groupindex else None
        elif part == "suffix":
            extracted_part = match.group("suffix") if "suffix" in pattern.groupindex else None
        elif part == "middle":
            extracted_part = match.group("middle") if "middle" in pattern.groupindex else None
        else:
            extracted_part = None

        # Apply `extract_type` to filter digits or characters
        if extract_type == "digits" and extracted_part:
            extracted_part = ''.join(filter(str.isdigit, extracted_part))
        elif extract_type == "characters" and extracted_part:
            extracted_part = ''.join(filter(str.isalpha, extracted_part))

        # Append valid results
        if extracted_part:
            result.append(extracted_part)

    return result


# Examples
if __name__ == "__main__":
    example_list = [
        "20240506_static_data",
        "random_key",
        "20240507_dynamic_data",
        "prefix_middle_suffix",
        "20240506_summary",
        "4875_other_data"
    ]

    # Example 1: Extract suffixes from items starting with dates
    suffixes = extract_parts_from_list(
        example_list,
        regex_pattern=r"^(?P<prefix>\d{8})_(?P<suffix>.+)$",
        extract_type=None,
        part="suffix"
    )
    print("Suffixes:", suffixes)
    # Output: ['static_data', 'dynamic_data', 'summary']

    # Example 2: Extract digits from prefixes (dates)
    prefixes = extract_parts_from_list(
        example_list,
        regex_pattern=r"^(?P<prefix>\d{8})_(?P<suffix>.+)$",
        extract_type="digits",
        part="prefix"
    )
    print("Prefixes:", prefixes)
    # Output: ['20240506', '20240507', '20240506']

    # Example 3: Return items matching regex exactly
    exact_matches = extract_parts_from_list(
        example_list,
        regex_pattern=r"^20240506_(.+)$"
    )
    print("Exact Matches:", exact_matches)
    # Output: ['20240506_static_data', '20240506_summary']

    # Example 4: Extract middle parts
    middle_parts = extract_parts_from_list(
        example_list,
        regex_pattern=r"^(?P<prefix>\w+)_(?P<middle>[a-z]+)_(?P<suffix>.+)$",
        extract_type="characters",
        part="middle"
    )
    print("Middle Parts:", middle_parts)
    # Output: ['middle']
