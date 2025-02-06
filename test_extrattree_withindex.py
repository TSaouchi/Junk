import unittest
from collections import defaultdict
from copy import deepcopy

class TestOptimizedTreeExtractor(unittest.TestCase):
    def setUp(self):
        """Set up test fixtures for each test method"""
        self.test_dict = {
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
            "subnode3.node2": ["leaf7", 20]
        }
        self.extractor = OptimizedTreeExtractor(self.test_dict)

    def test_initialization(self):
        """Test the initialization of the TreeExtractor"""
        self.assertEqual(self.extractor.dictionary, self.test_dict)
        self.assertIsInstance(self.extractor.forward_index, defaultdict)
        self.assertIsInstance(self.extractor.reverse_index, defaultdict)
        self.assertIsInstance(self.extractor.list_references, defaultdict)

    def test_build_indexes(self):
        """Test if indexes are built correctly"""
        # Test forward index
        self.assertIn("root1.node1", self.extractor.forward_index["root1"])
        self.assertIn("root1.node2", self.extractor.forward_index["root1"])
        self.assertIn("subnode1.node1", self.extractor.forward_index["subnode1"])

        # Test reverse index
        self.assertIn("root1", self.extractor.reverse_index["root1.node1"])
        self.assertIn("root1.node4", self.extractor.reverse_index["subnode1"])

        # Test list references
        self.assertIn("leaf2", self.extractor.list_references["root1.node3"])
        self.assertIn("subnode1", self.extractor.list_references["subnode2.node2"])

    def test_extract_subtree_root1(self):
        """Test extracting subtree starting from root1"""
        result = self.extractor.extract_subtree("root1")
        
        # Check if all root1 nodes are present
        expected_keys = {
            "root1.node1", "root1.node2", "root1.node3", "root1.node4", "root1.node5",
            "subnode1.node1", "subnode2.node1", "subnode2.node2"
        }
        self.assertEqual(set(result.keys()), expected_keys)
        
        # Check specific values
        self.assertEqual(result["root1.node1"], "leaf1")
        self.assertEqual(result["root1.node3"], ["leaf2", 10])
        self.assertEqual(result["subnode1.node1"], "leaf3")

    def test_extract_subtree_root2(self):
        """Test extracting subtree starting from root2"""
        result = self.extractor.extract_subtree("root2")
        
        expected_keys = {
            "root2.node1", "root2.node2",
            "subnode3.node1", "subnode3.node2"
        }
        self.assertEqual(set(result.keys()), expected_keys)

    def test_extract_nonexistent_root(self):
        """Test extracting subtree with non-existent root"""
        result = self.extractor.extract_subtree("nonexistent_root")
        self.assertEqual(result, {})

    def test_circular_references(self):
        """Test handling of circular references"""
        circular_dict = {
            "root1.node1": "subnode1",
            "subnode1.node1": "subnode2",
            "subnode2.node1": "subnode1"  # Creates a circle
        }
        extractor = OptimizedTreeExtractor(circular_dict)
        result = extractor.extract_subtree("root1")
        
        expected_keys = {
            "root1.node1", "subnode1.node1", "subnode2.node1"
        }
        self.assertEqual(set(result.keys()), expected_keys)

    def test_empty_dictionary(self):
        """Test with empty dictionary"""
        extractor = OptimizedTreeExtractor({})
        result = extractor.extract_subtree("root1")
        self.assertEqual(result, {})

    def test_statistics(self):
        """Test statistics generation"""
        stats = self.extractor.get_statistics()
        
        self.assertIn('total_keys', stats)
        self.assertIn('unique_parents', stats)
        self.assertIn('list_references', stats)
        self.assertIn('memory_usage_estimate_mb', stats)
        
        self.assertEqual(stats['total_keys'], len(self.test_dict))
        self.assertGreater(stats['memory_usage_estimate_mb'], 0)

    def test_large_dataset(self):
        """Test with a larger dataset"""
        large_dict = {}
        for i in range(100):
            large_dict[f"root{i}.node1"] = f"leaf{i}"
            large_dict[f"root{i}.node2"] = f"subnode{i}"
        
        extractor = OptimizedTreeExtractor(large_dict)
        result = extractor.extract_subtree("root1")
        
        self.assertIn("root1.node1", result)
        self.assertIn("root1.node2", result)
        self.assertEqual(len(result), 2)

    def test_mixed_value_types(self):
        """Test handling of different value types"""
        mixed_dict = {
            "root1.node1": 123,
            "root1.node2": True,
            "root1.node3": None,
            "root1.node4": ["str", 123, True, None],
            "root1.node5": {"key": "value"}
        }
        extractor = OptimizedTreeExtractor(mixed_dict)
        result = extractor.extract_subtree("root1")
        
        self.assertEqual(len(result), 5)
        self.assertEqual(result["root1.node1"], 123)
        self.assertEqual(result["root1.node2"], True)
        self.assertIsNone(result["root1.node3"])

if __name__ == '__main__':
    unittest.main(verbosity=2)
