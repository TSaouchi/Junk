from collections import defaultdict

class OptimizedTreeExtractor:
    __slots__ = ('dictionary', 'forward_index', 'list_references', 'non_terminal_refs')
    
    def __init__(self, dictionary):
        """
        Initialize the extractor with __slots__ to reduce memory usage.
        
        Args:
            dictionary (dict): Input dictionary with dot-notation keys.
        """
        self.dictionary = dictionary
        self.forward_index = defaultdict(list)
        self.list_references = defaultdict(list)
        self.non_terminal_refs = {}
        self._build_indexes()
        
    def _build_indexes(self):
        d = self.dictionary
        for key, value in d.items():
            parts = key.split('.')
            parent = parts[0]
            if len(parts) > 1:
                self.forward_index[parent].append(key)
            if isinstance(value, str) and value:
                if not value.startswith('leaf'):
                    self.non_terminal_refs[key] = value
            if isinstance(value, list):
                for item in value:
                    if isinstance(item, str):
                        self.list_references[key].append(item)
    
    def extract_subtree(self, root_name):
        """
        Extracts the subtree starting at root_name using the pre-built indexes.
        
        Args:
            root_name (str): The starting node name.
            
        Returns:
            dict: The extracted subtree.
        """
        result = {}
        visited = set()
        stack = [root_name]
        
        # Local variable references to speed up attribute lookups in the loop.
        d = self.dictionary
        fwd = self.forward_index
        lref = self.list_references
        non_term = self.non_terminal_refs
        
        while stack:
            current = stack.pop()
            if current in visited:
                continue
            visited.add(current)
            children = fwd.get(current)
            if children:
                for child in children:
                    result[child] = d[child]
                    for ref in lref.get(child, []):
                        if ref not in visited:
                            stack.append(ref)
                    if child in non_term:
                        stack.append(non_term[child])
        return result


# Example usage with performance testing:
if __name__ == "__main__":
    import time
    
    # Create a larger test dictionary
    def create_large_dict(size):
        large_dict = {}
        for i in range(size):
            large_dict[f"root{i}.node1"] = f"leaf{i}"
            large_dict[f"root{i}.node2"] = f"subnode{i}"
            large_dict[f"subnode{i}.node1"] = f"leaf{i+size}"
            large_dict[f"subnode{i}.node2"] = [f"leaf{i+size*2}", f"leaf{i+size*3}", i]
            large_dict[f"leaf{i+size*3}.other"] = [i+size*2, i]
        return large_dict
    
    test_dict = create_large_dict(500_000)
        
    # Measure initialization time
    start_time = time.time()
    extractor = OptimizedTreeExtractor(test_dict)
    init_time = time.time() - start_time
    print(f"Initialization time: {init_time:.4f} seconds")
    
    # Measure extraction time
    start_time = time.time()
    result = extractor.extract_subtree(f"root1")
    extract_time = time.time() - start_time
    print(f"Extraction time: {extract_time:.4f} seconds")
