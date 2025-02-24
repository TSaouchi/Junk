import json
import os
import tempfile
import threading
import multiprocessing
import time
from pathlib import Path
from contextlib import contextmanager

# -------------------------------
# Global Configurations
# -------------------------------
LOCK_DIR = Path(tempfile.gettempdir())  # Lock files stored in temp directory
LOCK_TIMEOUT = 5  # Default timeout in seconds

# Detect OS and import appropriate locking mechanism
if os.name == "nt":
    import msvcrt
    LOCK_TYPE = "windows"
else:
    import fcntl
    LOCK_TYPE = "unix"

# Global thread-level lock for intra-process safety
thread_lock = threading.Lock()

# -------------------------------
# FileLocker Class
# -------------------------------
class FileLocker:
    """Cross-platform file locker to ensure thread and process safety."""

    def __init__(self, lock_name: str, timeout: int = LOCK_TIMEOUT, shared: bool = False):
        """
        :param lock_name: Name of the lock file
        :param timeout: Timeout in seconds before force unlocking
        :param shared: True for shared read lock, False for exclusive write lock
        """
        self.lock_file = LOCK_DIR / lock_name
        self.timeout = timeout
        self.shared = shared  # Shared lock for reading

    @contextmanager
    def lock(self):
        start_time = time.time()
        lock = thread_lock if multiprocessing.current_process().name == 'MainProcess' else multiprocessing.Lock()
        
        with lock:  # Ensure thread safety
            while True:
                try:
                    with self.lock_file.open("w") as f:
                        if LOCK_TYPE == "windows":
                            lock_type = msvcrt.LK_NBRLCK if self.shared else msvcrt.LK_NBLCK
                            msvcrt.locking(f.fileno(), lock_type, 1)  # Windows lock
                        else:
                            lock_type = fcntl.LOCK_SH if self.shared else fcntl.LOCK_EX
                            fcntl.flock(f, lock_type | fcntl.LOCK_NB)  # Unix lock
                        
                        yield  # Lock acquired
                        return  # Exit after operation completes
                except (OSError, BlockingIOError):
                    if time.time() - start_time > self.timeout:
                        print(f"Timeout: Lock on {self.lock_file.name} is stuck, force releasing it.")
                        self.lock_file.unlink(missing_ok=True)  # Remove stuck lock file
                        continue  # Retry locking after force release
                    time.sleep(0.1)  # Wait and retry

# -------------------------------
# Thread & Process Safe JSON Read/Write
# -------------------------------

def write_json_wlock(file_path: Path, data: dict):
    """Safely writes JSON data to a file using FileLocker (thread-safe & process-safe)."""
    lock_name = f"{file_path.name}.lock"
    locker = FileLocker(lock_name, shared=False)  # Exclusive lock for writing
    
    with locker.lock():
        with file_path.open("w", encoding="utf-8") as f:
            json.dump(data, f, indent=4)

def read_json_wlock(file_path: Path):
    """Safely reads JSON data from a file using FileLocker (thread-safe & process-safe)."""
    lock_name = f"{file_path.name}.lock"
    locker = FileLocker(lock_name, shared=True)  # Shared lock for reading
    
    with locker.lock():
        with file_path.open("r", encoding="utf-8") as f:
            return json.load(f)

# -------------------------------
# Example Usage
# -------------------------------
if __name__ == "__main__":
    json_file = Path(tempfile.gettempdir()) / "data.json"

    # Example data to write
    sample_data = {"name": "Toufik", "job": "Python Developer", "projects": 5}

    # Write JSON in a separate process
    def writer_process():
        print("Writing JSON...")
        write_json_wlock(json_file, sample_data)
        print("Write complete.")

    # Read JSON in a separate process
    def reader_process():
        print("Reading JSON...")
        data = read_json_wlock(json_file)
        print("Read complete:", data)

    # Start writer first, then reader
    writer = multiprocessing.Process(target=writer_process)
    reader = multiprocessing.Process(target=reader_process)

    writer.start()
    reader.start()

    writer.join()
    reader.join()
