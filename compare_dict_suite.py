import os
import json
import glob
import logging
from typing import List, Dict, Any, Tuple

import pytest

class DictionaryComparisonTestSuite:
    """
    Configuration and runner for dictionary comparison tests 
    that efficiently loads test cases from JSON files using glob.
    """
    
    @classmethod
    def create_test_cases(cls, directory: str) -> List[ComparisonTestCase]:
        """
        Generate test cases by efficiently matching and loading JSON files.
        
        Args:
            directory (str): Path to the directory containing test case JSON files
        
        Returns:
            List[ComparisonTestCase]: List of test cases to compare
        
        Raises:
            ValueError: If directory does not exist or no matching files are found
        """
        # Validate directory existence
        if not os.path.isdir(directory):
            raise ValueError(f"Directory does not exist: {directory}")
        
        # Logger for tracking file processing
        logger = logging.getLogger(__name__)
        
        # Use glob to find all JSON files starting with expected_ or actual_
        expected_files = glob.glob(os.path.join(directory, 'expected_*.json'))
        
        # Store test cases
        test_cases = []
        
        # Process each expected file
        for expected_path in expected_files:
            # Extract base filename without prefix
            base_filename = os.path.basename(expected_path).replace('expected_', '')
            
            # Construct corresponding actual file path
            actual_path = os.path.join(directory, f'actual_{base_filename}')
            
            # Check if actual file exists
            if not os.path.exists(actual_path):
                logger.warning(f"No matching actual file for {base_filename}. Skipping.")
                continue
            
            try:
                # Read expected file
                with open(expected_path, 'r') as expected_file:
                    expected_data = json.load(expected_file)
                
                # Read actual file
                with open(actual_path, 'r') as actual_file:
                    actual_data = json.load(actual_file)
                
                # Create test case
                test_case = ComparisonTestCase(
                    expected=expected_data,
                    actual=actual_data
                )
                test_cases.append(test_case)
                
                logger.info(f"Loaded test case: {test_case.name}")
            
            except json.JSONDecodeError as e:
                logger.error(f"JSON parsing error for {base_filename}: {e}")
            except IOError as e:
                logger.error(f"File reading error for {base_filename}: {e}")
        
        # Validate test cases were found
        if not test_cases:
            raise ValueError(f"No valid test cases found in directory: {directory}")
        
        return test_cases


# Example of how to use in pytest
def pytest_generate_tests(metafunc):
    """
    Dynamically generate test cases for pytest from a directory.
    
    Args:
        metafunc: Pytest metafunc object for test parameterization
    """
    if "comparison_test_case" in metafunc.fixturenames:
        # Specify the directory containing test case JSON files
        test_cases_directory = './test_cases'  # Adjust this path as needed
        
        # Create test cases
        test_cases = DictionaryComparisonTestSuite.create_test_cases(test_cases_directory)
        
        # Parametrize the test with generated cases
        metafunc.parametrize(
            "comparison_test_case", 
            test_cases, 
            ids=[case.name for case in test_cases]
        )
