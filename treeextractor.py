class TreeExtractor:
    def __init__(self, dictionary):
        """
        Initialize the TreeExtractor with a dictionary.
        
        Args:
            dictionary (dict): Input dictionary with dot-notation keys
        """
        self.dictionary = dictionary
        self.processed_nodes = set()
        
    def extract_subtree(self, root_name):
        """
        Extract a subtree from the dictionary based on a root name.
        
        Args:
            root_name (str): Name of the root to extract subtree from
            
        Returns:
            dict: Extracted subtree with all nested nodes
        """
        self.processed_nodes.clear()
        result = {}
        nodes_to_process = {root_name}
        
        while nodes_to_process:
            current_node = nodes_to_process.pop()
            self._process_node(current_node, nodes_to_process, result)
            
        return result
    
    def _process_node(self, current_node, nodes_to_process, result):
        """
        Process a single node and its children.
        
        Args:
            current_node (str): Current node being processed
            nodes_to_process (set): Set of nodes that still need to be processed
            result (dict): Dictionary to store the results
        """
        self.processed_nodes.add(current_node)
        
        for key, value in self.dictionary.items():
            if not key.startswith(f"{current_node}."):
                continue
                
            result[key] = value
            self._add_referenced_nodes(value, nodes_to_process)
    
    def _add_referenced_nodes(self, value, nodes_to_process):
        """
        Add referenced nodes to the processing queue.
        
        Args:
            value: Value to check for references
            nodes_to_process (set): Set of nodes that still need to be processed
        """
        if isinstance(value, str) and value and not self._is_leaf_node(value):
            self._add_node_if_new(value, nodes_to_process)
        elif isinstance(value, list):
            for item in value:
                if isinstance(item, str):
                    self._add_node_if_new(item, nodes_to_process)
    
    def _add_node_if_new(self, node, nodes_to_process):
        """
        Add a node to the processing queue if it hasn't been processed yet.
        
        Args:
            node (str): Node to potentially add
            nodes_to_process (set): Set of nodes that still need to be processed
        """
        if node not in self.processed_nodes and node not in nodes_to_process:
            nodes_to_process.add(node)
    
    def _is_leaf_node(self, value):
        """
        Check if a value represents a leaf node (doesn't have children).
        
        Args:
            value (str): Value to check
            
        Returns:
            bool: True if the value is a leaf node, False otherwise
        """
        return value.startswith("leaf")

# Example usage:
if __name__ == "__main__":
    request = {
        "root1.node1": "leaf1",
        "root1.node2": "",
        "root1.node3": ["leaf2", 10],
        "root1.node4": "subnode1",
        "root1.node5": "subnode2",
        "subnode1.node1": "leaf3",
        "subnode2.node1": "leaf4",
        "subnode2.node2": ["subnode1"],
        "root2.node1": "leaf5",
        "root2.node2": "subnode3",
        "subnode3.node1": "leaf6",
        "subnode3.node2": ["leaf7", 20],
        "root3.node1": "leaf8",
        "root3.node2": "subnode4",
        "subnode4.node3": "subnode5",
        "subnode4.node1": "leaf9",
        "subnode4.node2": ["leaf10", 30],
        "subnode5.node1": "leaf11",
        "subnode5.node2": ["leaf12", 40],
    }

    extractor = TreeExtractor(request)
    
    # Extract and print root1 subtree
    print("Root1 subtree:")
    root1_tree = extractor.extract_subtree("root1")
    for k, v in sorted(root1_tree.items()):
        print(f"{k}: {v}")
        
    # Extract and print root2 subtree
    print("\nRoot2 subtree:")
    root2_tree = extractor.extract_subtree("root2")
    for k, v in sorted(root2_tree.items()):
        print(f"{k}: {v}")
