import abc
import dataclasses
import logging

from deepdiff import DeepDiff
import pytest


# Single Responsibility: Separate interface for comparison strategy
class ComparisonStrategy(abc.ABC):
    """Abstract base class for comparison strategies."""
    
    @abc.abstractmethod
    def calculate_change_percentage(self, dict1: dict, dict2: dict):
        """Calculate the percentage of changes between two dictionaries."""
        pass


# Concrete implementation of comparison strategy
class DeepDiffComparisonStrategy(ComparisonStrategy):
    """Implements comparison using DeepDiff library."""
    
    def calculate_change_percentage(self, dict1: dict, dict2: dict):
        """
        Calculate percentage of changes using DeepDiff.
        
        Args:
            dict1 : First dictionary to compare
            dict2 : Second dictionary to compare
        
        Returns:
            float: Percentage of changes between dictionaries
        """
        # Perform deep comparison between the two dictionaries
        diff = DeepDiff(dict1, dict2, verbose_level=2)
        
        # Get the total number of elements in both dictionaries
        total_elements = len(dict1) + len(dict2)
        
        # Count the number of changes (added, removed, changed)
        changes = (
            len(diff.get("values_changed", {})) + 
            len(diff.get("dictionary_item_added", {})) + 
            len(diff.get("dictionary_item_removed", {}))
        )
        
        # Calculate the percentage of changes
        return (changes / total_elements) * 100 if total_elements else 0


# Data Transfer Object for test cases
@dataclasses.dataclass
class ComparisonTestCase:
    """Data Transfer Object to represent a test case."""
    expected: dict
    actual: dict
    
    @property
    def name(self):
        """Generate a descriptive name for the test case."""
        return f"{self.expected.get('name', 'Unknown')} vs {self.actual.get('name', 'Unknown')}"


# Dependency Injection: Comparison Service
class ComparisonService:
    """
    Service responsible for comparing dictionaries.
    Follows Dependency Injection and Open/Closed principles.
    """
    
    def __init__(self, strategy: ComparisonStrategy, threshold: float = 10.0):
        """
        Initialize the comparison service.
        
        Args:
            strategy (ComparisonStrategy): Comparison strategy to use
            threshold (float, optional): Change threshold for pass/fail. Defaults to 10.0.
        """
        self._strategy = strategy
        self._threshold = threshold
        self._logger = logging.getLogger(self.__class__.__name__)
    
    def compare(self, test_case: ComparisonTestCase):
        """
        Compare two dictionaries using the specified strategy.
        
        Args:
            test_case (ComparisonTestCase): Test case to compare
        
        Returns:
            Optional[str]: Error message if comparison fails, None otherwise
        """
        # Compare the dictionaries
        diff = DeepDiff(test_case.expected, test_case.actual, verbose_level=2)
        
        # Calculate change percentage
        change_percentage = self._strategy.calculate_change_percentage(
            test_case.expected, 
            test_case.actual
        )
        
        # If differences exist and exceed threshold, prepare error message
        if diff and change_percentage > self._threshold:
            return (
                f"Comparison Failed for {test_case.name}:\n"
                f"Change Percentage: {change_percentage:.2f}%\n"
                f"Differences: {diff.pretty()}"
            )
        
        # Log successful comparison
        self._logger.info(
            f"Test PASSED for {test_case.name} with {change_percentage:.2f}% changes"
        )
        
        return None


# Test Configuration
class DictionaryComparisonTestSuite:
    """Configuration and runner for dictionary comparison tests."""
    
    @staticmethod
    def create_test_cases():
        """
        Generate test cases for comparison.
        
        Returns:
            List[ComparisonTestCase]: List of test cases to compare
        """
        return [
            ComparisonTestCase(
                expected={"name": "Toto", "age": 30, "city": "Paris"},
                actual={"name": "Toto", "age": 31, "city": "Paris"}
            ),
            ComparisonTestCase(
                expected={"name": "Alice", "age": 25, "city": "New York"},
                actual={"name": "Alice", "age": 25, "city": "London"}
            ),
            ComparisonTestCase(
                expected={"name": "Bob", "age": 22, "city": "London"},
                actual={"name": "Bob", "age": 22, "city": "London"}
            )
        ]


# Pytest Parametrization
def pytest_generate_tests(metafunc):
    """
    Dynamically generate test cases for pytest.
    
    Args:
        metafunc: Pytest metafunc object for test parameterization
    """
    if "comparison_test_case" in metafunc.fixturenames:
        # Create test cases
        test_cases = DictionaryComparisonTestSuite.create_test_cases()
        
        # Parametrize the test with generated cases
        metafunc.parametrize(
            "comparison_test_case", 
            test_cases, 
            ids=[case.name for case in test_cases]
        )


# Main Test Function
def test_data_comparison(comparison_test_case: ComparisonTestCase, caplog):
    """
    Test function to compare dictionaries.
    
    Args:
        comparison_test_case (ComparisonTestCase): Test case to compare
        caplog: Pytest logging capture fixture
    """
    # Set logging level
    caplog.set_level(logging.INFO)
    
    # Create comparison service with strategy
    comparison_service = ComparisonService(
        strategy=DeepDiffComparisonStrategy(),
        threshold=10.0
    )
    
    # Perform comparison
    error_message = comparison_service.compare(comparison_test_case)
    
    # Fail test if error message exists
    if error_message:
        pytest.fail(error_message, pytrace=False)
