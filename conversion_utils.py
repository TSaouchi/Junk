# conversion_utils.py

def parse_logical_string(value):
    """
    Converts a string representation of a logical value ('yes', 'no', 'true', 'false', 'null')
    into a Python native value (True, False, None).
    """
    from distutils.util import strtobool

    if isinstance(value, str):
        value_lower = value.strip().lower()
        if value_lower in ["null", "none"]:
            return None
        try:
            return bool(strtobool(value_lower))
        except ValueError:
            raise ValueError(f"Invalid value for logical conversion: {value}")
    return value
