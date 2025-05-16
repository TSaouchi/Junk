def read_numbers_from_file(filepath):
    numbers = []
    with open(filepath, 'r') as file:
        for line in file:
            stripped = line.strip()
            if stripped:  # ignore empty lines
                try:
                    # Try to convert to int, fallback to float
                    num = int(stripped) if stripped.isdigit() else float(stripped)
                    numbers.append(num)
                except ValueError:
                    print(f"Warning: Skipped invalid line '{stripped}'")
    return numbers

# Example usage
file_path = 'numbers.txt'
number_list = read_numbers_from_file(file_path)
print(number_list)
