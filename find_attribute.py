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

    if not hasattr(obj, '__dict__'):  # If not an object with attributes, return directly
        return obj

    result = {}
    for key, value in vars(obj).items():
        if hasattr(value, '__dict__') and depth > 0:  # If it's an object, expand recursively
            result[key] = deep_vars(value, depth - 1, visited)
        else:
            result[key] = value  # Primitive values are directly assigned

    return result
