def deep_vars(obj, depth=1, visited=None):
    """
    Recursively extract attributes of an object up to a given depth.

    Args:
        obj: The object to extract attributes from.
        depth (int): The maximum depth to expand nested attributes.
        visited (set): A set to track visited objects and avoid infinite recursion.

    Returns:
        dict: A dictionary representation of the object's attributes.
    """
    if depth < 0 or obj is None:
        return None

    if visited is None:
        visited = set()

    obj_id = id(obj)
    if obj_id in visited:
        return "<Circular Reference>"  # Prevent infinite loops in cyclic references
    visited.add(obj_id)

    # If obj is already a basic type, return it as is
    if isinstance(obj, (int, float, str, bool)):
        return obj

    # Handle lists, tuples, sets, and dicts by processing elements recursively
    if isinstance(obj, (list, tuple, set)):
        return type(obj)(deep_vars(item, depth - 1, visited) for item in obj)
    
    if isinstance(obj, dict):
        return {key: deep_vars(value, depth - 1, visited) for key, value in obj.items()}

    # If obj has no attributes, convert it to a string
    if not hasattr(obj, '__dict__'):
        return f'"{str(obj)}"'  # Convert objects like <Logger CacheManager (INFO)> into strings

    # Process object attributes
    result = {}
    for key, value in vars(obj).items():
        try:
            if hasattr(value, '__dict__') and depth > 0:  # If it's an object, expand recursively
                result[key] = deep_vars(value, depth - 1, visited)
            else:
                # Handle lists, tuples, sets, dicts inside objects
                if isinstance(value, (list, tuple, set, dict)):
                    result[key] = deep_vars(value, depth - 1, visited)
                else:
                    # Convert unhandled objects to strings
                    result[key] = value if isinstance(value, (int, float, str, bool)) else f'"{str(value)}"'
        except Exception:
            result[key] = "<Error Accessing Attribute>"

    return result
