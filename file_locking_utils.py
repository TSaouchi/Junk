import time
import json
import tempfile
import threading
import multiprocessing
from pathlib import Path
import os
from contextlib import contextmanager

# Cross-platform temporary directory for lock files
LOCK_DIR = Path(tempfile.gettempdir())
LOCK_TIMEOUT = 5  # Timeout in seconds

# Platform-specific imports
if os.name == "nt":
    import msvcrt
    LOCK_TYPE = "windows"
else:
    import fcntl
    LOCK_TYPE = "unix"

# Thread-level lock to ensure thread safety within a process
thread_lock = threading.Lock()


class FileLocker:
    """Cross-platform file locker to ensure thread and process safety."""
    def __init__(self, lock_name: str, timeout: int = LOCK_TIMEOUT):
        self.lock_file = LOCK_DIR / lock_name
        self.timeout = timeout

    @contextmanager
    def lock(self):
        start_time = time.time()
        
        # Choose between threading or multiprocessing lock based on context
        lock = thread_lock if multiprocessing.current_process().name == 'MainProcess' else multiprocessing.Lock()
        
        with lock:  # Ensure thread/process safety within the same process
            while True:
                try:
                    with self.lock_file.open("w") as f:
                        if LOCK_TYPE == "windows":
                            msvcrt.locking(f.fileno(), msvcrt.LK_NBLCK, 1)  # Windows lock
                        else:
                            fcntl.flock(f, fcntl.LOCK_EX | fcntl.LOCK_NB)  # Unix lock
                        
                        yield  # Lock acquired
                        return  # Exit after successful operation
                
                except (OSError, BlockingIOError):
                    if time.time() - start_time > self.timeout:
                        print(f"Timeout: Lock on {self.lock_file.name} is stuck, force releasing it.")
                        self.lock_file.unlink(missing_ok=True)  # Remove stuck lock
                        continue  # Retry locking
                
                    time.sleep(0.1)  # Wait and retry


def write_json_wlock(file_path: Path, data: dict):
    """Safely writes JSON data to a file using FileLocker (thread-safe & process-safe)."""
    lock_name = f"{file_path.name}.lock"
    locker = FileLocker(lock_name)
    
    with locker.lock():
        with file_path.open("w", encoding="utf-8") as f:
            json.dump(data, f, indent=4)
