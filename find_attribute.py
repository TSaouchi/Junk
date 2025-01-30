def find_attribute(obj, target_attr, visited=None):
    """
    Recursively search for an attribute in an object and its nested attributes.
    
    Args:
        obj: The object to search through
        target_attr: The name of the attribute to find
        visited: Set to keep track of visited objects to avoid infinite recursion
        
    Returns:
        tuple: (found_value, path_to_attribute) or (None, None) if not found
    """
    if visited is None:
        visited = set()
    
    # Avoid circular references and already visited objects
    obj_id = id(obj)
    if obj_id in visited:
        return None, None
    visited.add(obj_id)
    
    # Get all attributes of the object
    try:
        attributes = dir(obj)
    except Exception:
        return None, None
    
    # First, check direct attributes
    if target_attr in attributes:
        try:
            return getattr(obj, target_attr), [target_attr]
        except Exception:
            pass
    
    # Then recursively check nested attributes
    for attr in attributes:
        try:
            value = getattr(obj, attr)
            
            # Skip methods and built-in attributes
            if callable(value) or attr.startswith('__'):
                continue
            
            # Recursively search in this attribute
            found_value, path = find_attribute(value, target_attr, visited)
            if found_value is not None:
                return found_value, [attr] + path
                
        except Exception:
            continue
            
    return None, None

# Example usage
class NestedClass:
    def __init__(self):
        self.hidden_value = 42

class MainClass:
    def __init__(self):
        self.nested = NestedClass()
        self.some_value = "test"

# Example of how to use it
obj = MainClass()
value, path = find_attribute(obj, 'hidden_value')
if value is not None:
    print(f"Found value: {value}")
    print(f"Path to value: {' -> '.join(path)}")
else:
    print("Attribute not found")
