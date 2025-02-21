import unittest
from unittest.mock import patch, MagicMock
import json
from pathlib import Path
import tempfile
import time
import os
import threading
import multiprocessing

# The code under test (import your script here)
from your_script import write_to_file, file_lock, LOCK_DIR

class TestFileLocking(unittest.TestCase):
    @patch("your_script.time.sleep", return_value=None)  # Mock sleep to speed up the tests
    def test_lock_acquisition(self, mock_sleep):
        """Test that file lock is acquired successfully and prevents other writes."""
        config_path = Path(LOCK_DIR / "test_config.json")
        config_data = {"a": 1, "b": 2}

        # Simulate a single process
        with file_lock(config_path.name):
            with config_path.open("w") as f:
                json.dump(config_data, f, indent=4)

        # Check that the file exists and data is written
        with open(config_path, "r") as f:
            data = json.load(f)

        self.assertEqual(data, config_data)
        os.remove(config_path)

    @patch("your_script.time.sleep", return_value=None)  # Mock sleep to speed up the tests
    def test_lock_timeout(self, mock_sleep):
        """Test that lock acquisition times out as expected."""
        config_path = Path(LOCK_DIR / "test_config_timeout.json")
        config_data = {"x": 1, "y": 2}

        # Start a process that holds the lock for too long
        def long_lock():
            with file_lock(config_path.name):
                time.sleep(2)  # Hold the lock longer than the timeout (5 seconds)
        
        # Start long lock in a separate thread to simulate a blocking lock
        lock_thread = threading.Thread(target=long_lock)
        lock_thread.start()

        # Allow some time for the thread to start and block
        time.sleep(1)

        # Attempt to acquire the lock with a timeout
        with self.assertRaises(BlockingIOError):  # It should raise after waiting for timeout
            with file_lock(config_path.name, timeout=1):
                with config_path.open("w") as f:
                    json.dump(config_data, f, indent=4)

        lock_thread.join()

    @patch("your_script.time.sleep", return_value=None)  # Mock sleep to speed up the tests
    def test_file_lock_cleanup(self, mock_sleep):
        """Test that the lock is cleaned up if a timeout occurs."""
        config_path = Path(LOCK_DIR / "test_config_cleanup.json")
        config_data = {"a": 1, "b": 2}

        def long_lock():
            with file_lock(config_path.name):
                time.sleep(2)

        # Start the long lock
        lock_thread = threading.Thread(target=long_lock)
        lock_thread.start()

        time.sleep(1)  # Wait for the thread to hold the lock

        # Try to acquire the lock and timeout
        with self.assertRaises(BlockingIOError):
            with file_lock(config_path.name, timeout=1):
                with config_path.open("w") as f:
                    json.dump(config_data, f, indent=4)

        lock_thread.join()  # Wait for the lock thread to finish

        # Check that the lock file has been cleaned up (it shouldn't exist now)
        self.assertFalse(config_path.exists())

    @patch("your_script.time.sleep", return_value=None)  # Mock sleep to speed up the tests
    def test_multiple_process_locking(self, mock_sleep):
        """Test that the file lock works across multiple processes."""
        config_path = Path(LOCK_DIR / "test_config_process.json")
        config_data = {"p": 1, "q": 2}

        def process_worker():
            with file_lock(config_path.name):
                with config_path.open("w") as f:
                    json.dump(config_data, f, indent=4)

        processes = []
        for _ in range(5):  # Start multiple processes
            process = multiprocessing.Process(target=process_worker)
            processes.append(process)
            process.start()

        # Wait for all processes to complete
        for process in processes:
            process.join()

        # Ensure the data was written successfully and only once
        with open(config_path, "r") as f:
            data = json.load(f)

        self.assertEqual(data, config_data)
        os.remove(config_path)

    @patch("your_script.time.sleep", return_value=None)  # Mock sleep to speed up the tests
    def test_file_lock_acquisition_with_threading(self, mock_sleep):
        """Test that the file lock works across multiple threads."""
        config_path = Path(LOCK_DIR / "test_config_thread.json")
        config_data = {"m": 1, "n": 2}

        def thread_worker():
            with file_lock(config_path.name):
                with config_path.open("w") as f:
                    json.dump(config_data, f, indent=4)

        threads = []
        for _ in range(5):  # Start multiple threads
            thread = threading.Thread(target=thread_worker)
            threads.append(thread)
            thread.start()

        # Wait for all threads to complete
        for thread in threads:
            thread.join()

        # Ensure the data was written successfully and only once
        with open(config_path, "r") as f:
            data = json.load(f)

        self.assertEqual(data, config_data)
        os.remove(config_path)

if __name__ == "__main__":
    unittest.main()
