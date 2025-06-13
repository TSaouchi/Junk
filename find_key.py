def find_key(d, key):
    """Yield all values of a key from a nested structure."""
    if isinstance(d, dict):
        for k, v in d.items():
            if k == key:
                yield v
            if isinstance(v, (dict, list)):
                yield from find_key(v, key)
    elif isinstance(d, list):
        for item in d:
            yield from find_key(item, key)
