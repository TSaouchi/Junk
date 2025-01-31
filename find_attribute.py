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
        if hasattr(value, '__dict__') and depth > 0:  # Expand recursively
            result[key] = deep_vars(value, depth - 1, visited)
        elif not isinstance(value, (int, float, str, bool, list, dict, tuple, set)):
            result[key] = str(value)  # Convert to string if not a primitive type
        else:
            result[key] = value  # Keep original value for primitive types
    
    return result
