from collections import defaultdict

class OptimizedTreeExtractor:
    def __init__(self, dictionary):
        """
        Initialize the TreeExtractor with a dictionary and build indexes.
        
        Args:
            dictionary (dict): Input dictionary with dot-notation keys
        """
        self.dictionary = dictionary
        # Index storing parent -> children relationships
        self.forward_index = defaultdict(set)
        # Index storing node -> parent relationships
        self.reverse_index = defaultdict(set)
        # Index storing references from list values
        self.list_references = defaultdict(set)
        # Build indexes
        self._build_indexes()
        
    def _build_indexes(self):
        """
        Build indexes for faster traversal. This is done once during initialization.
        """
        for key, value in self.dictionary.items():
            parts = key.split('.')
            parent = parts[0]
            
            # Build forward and reverse indexes
            if len(parts) > 1:
                self.forward_index[parent].add(key)
                self.reverse_index[key].add(parent)
            
            # Index string references
            if isinstance(value, str) and value:
                self.reverse_index[value].add(key)
            
            # Index list references
            if isinstance(value, list):
                for item in value:
                    if isinstance(item, str):
                        self.list_references[key].add(item)
                        self.reverse_index[item].add(key)
    
    def extract_subtree(self, root_name):
        """
        Extract a subtree using the pre-built indexes.
        
        Args:
            root_name (str): Name of the root to extract subtree from
            
        Returns:
            dict: Extracted subtree
        """
        result = {}
        visited = set()
        nodes_to_process = {root_name}
        
        while nodes_to_process:
            current = nodes_to_process.pop()
            if current in visited:
                continue
                
            visited.add(current)
            
            # Add direct children
            for child_key in self.forward_index[current]:
                result[child_key] = self.dictionary[child_key]
                nodes_to_process.update(
                    ref for ref in self.list_references[child_key]
                    if ref not in visited
                )
                
                value = self.dictionary[child_key]
                if isinstance(value, str) and value and not value.startswith('leaf'):
                    nodes_to_process.add(value)
        
        return result

    def get_statistics(self):
        """
        Get statistics about the indexed data.
        
        Returns:
            dict: Statistics about the indexes
        """
        return {
            'total_keys': len(self.dictionary),
            'unique_parents': len(self.forward_index),
            'list_references': sum(len(refs) for refs in self.list_references.values()),
            'memory_usage_estimate_mb': (
                len(str(self.dictionary)) +
                len(str(self.forward_index)) +
                len(str(self.reverse_index)) +
                len(str(self.list_references))
            ) / (1024 * 1024)  # Rough estimate in MB
        }

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
            large_dict[f"subnode{i}.node2"] = [f"leaf{i+size*2}", i]
        return large_dict
    
    # Test with both small and large datasets
    test_cases = [
        ("small", request),  # Original test case
        ("medium", create_large_dict(1000)),
        ("large", create_large_dict(10000))
    ]
    
    for size, test_dict in test_cases:
        print(f"\nTesting {size} dataset:")
        
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
        
        # Print statistics
        stats = extractor.get_statistics()
        print("Statistics:", stats)
