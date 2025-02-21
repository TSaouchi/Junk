import unittest
import json
import os
import tempfile
import threading
import multiprocessing
import time
import io
from pathlib import Path
from contextlib import redirect_stdout


# import script to test here 

# Helper function for process-based writes.
def process_write(file_path_str, data):
    file_path = Path(file_path_str)
    write_json_wlock(file_path, data)

# -------------------------------
# Unittest Script
# -------------------------------

class TestWriteJsonWLock(unittest.TestCase):
    
    def setUp(self):
        # Create a temporary directory for our test files
        self.temp_dir = tempfile.TemporaryDirectory()
        self.test_file = Path(self.temp_dir.name) / "test.json"

    def tearDown(self):
        # Cleanup temporary directory after each test
        self.temp_dir.cleanup()

    def test_write_json(self):
        """Test that write_json_wlock writes valid JSON to a file."""
        sample_data = {"key": "value", "number": 123}
        write_json_wlock(self.test_file, sample_data)
        with self.test_file.open("r", encoding="utf-8") as f:
            content = json.load(f)
        self.assertEqual(content, sample_data)

    def test_concurrent_writes(self):
        """
        Test concurrent writes to the same file using threads.
        The lock ensures that file writes are not corrupted.
        """
        sample_inputs = [
            {"thread": 1, "data": "first"},
            {"thread": 2, "data": "second"},
            {"thread": 3, "data": "third"}
        ]

        def write_data(data):
            write_json_wlock(self.test_file, data)

        threads = []
        for data in sample_inputs:
            t = threading.Thread(target=write_data, args=(data,))
            threads.append(t)
            t.start()

        for t in threads:
            t.join()

        with self.test_file.open("r", encoding="utf-8") as f:
            content = json.load(f)
        self.assertIn(content, sample_inputs)

    def test_concurrent_process_writes(self):
        """
        Test concurrent writes to the same file using separate processes.
        The lock ensures that concurrent process writes do not corrupt the file.
        """
        sample_inputs = [
            {"process": 1, "data": "first"},
            {"process": 2, "data": "second"},
            {"process": 3, "data": "third"}
        ]

        processes = []
        file_path_str = str(self.test_file)
        for data in sample_inputs:
            p = multiprocessing.Process(target=process_write, args=(file_path_str, data))
            processes.append(p)
            p.start()

        for p in processes:
            p.join()

        with self.test_file.open("r", encoding="utf-8") as f:
            content = json.load(f)
        self.assertIn(content, sample_inputs)

    def test_file_locker_lock(self):
        """
        Test that FileLocker prevents concurrent acquisition in a controlled scenario.
        """
        locker = FileLocker("test.lock")
        acquired_in_thread = []

        def try_lock():
            try:
                with locker.lock():
                    acquired_in_thread.append(True)
            except Exception:
                acquired_in_thread.append(False)

        with locker.lock():
            t = threading.Thread(target=try_lock)
            t.start()
            time.sleep(0.2)
            self.assertFalse(acquired_in_thread, "Thread should not acquire lock while held.")
        t2 = threading.Thread(target=try_lock)
        t2.start()
        t2.join()
        self.assertTrue(acquired_in_thread[-1], "Lock should be acquirable after release.")
    
    @unittest.skipIf(os.name == "nt", "Timeout test is skipped on Windows due to inherent file locking behavior")
    def test_timeout_condition(self):
        """
        Test that FileLocker times out and force-releases a stuck lock.
        
        This is done by manually acquiring a lock on the same lock file in a separate thread
        and holding it beyond the FileLocker timeout. The output is captured to verify the timeout message.
        """
        # Use a unique lock name for testing timeout.
        lock_name = "unittest_timeout_test.lock"
        lock_file_path = LOCK_DIR / lock_name

        # Function to hold the lock indefinitely (for a specified duration)
        def hold_lock(hold_time):
            with lock_file_path.open("w") as f:
                if LOCK_TYPE == "windows":
                    msvcrt.locking(f.fileno(), msvcrt.LK_NBLCK, 1)
                else:
                    fcntl.flock(f, fcntl.LOCK_EX)
                time.sleep(hold_time)
                # Lock released when exiting the with block

        # Start a thread that holds the lock longer than the FileLocker timeout.
        hold_duration = 2  # seconds
        locker_thread = threading.Thread(target=hold_lock, args=(hold_duration,))
        locker_thread.start()

        # Give the thread time to acquire the lock.
        time.sleep(0.2)

        # Create a FileLocker instance with a short timeout.
        short_timeout = 1  # second
        file_locker = FileLocker(lock_name, timeout=short_timeout)

        captured_output = io.StringIO()
        with redirect_stdout(captured_output):
            with file_locker.lock():
                # Dummy operation while lock is acquired.
                pass

        output = captured_output.getvalue()
        # Verify that the timeout message was printed.
        self.assertIn("Timeout: Lock on", output)

        locker_thread.join()


if __name__ == '__main__':
    multiprocessing.freeze_support()  # For Windows support
    unittest.main()
