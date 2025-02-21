import json
import os
import tempfile
import threading
import multiprocessing
import time
import io
from pathlib import Path
from contextlib import contextmanager, redirect_stdout

# -------------------------------
# Production Code Under Test
# -------------------------------

LOCK_DIR = Path(tempfile.gettempdir())
LOCK_TIMEOUT = 5  # Default timeout in seconds

if os.name == "nt":
    import msvcrt
    LOCK_TYPE = "windows"
else:
    import fcntl
    LOCK_TYPE = "unix"

# Global thread-level lock for intra-process safety
thread_lock = threading.Lock()

class FileLocker:
    """Cross-platform file locker to ensure thread and process safety."""
    def __init__(self, lock_name: str, timeout: int = LOCK_TIMEOUT):
        self.lock_file = LOCK_DIR / lock_name
        self.timeout = timeout

    @contextmanager
    def lock(self):
        start_time = time.time()
        # Use a global thread lock for main process; otherwise, a process-specific lock
        lock = thread_lock if multiprocessing.current_process().name == 'MainProcess' else multiprocessing.Lock()
        with lock:
            while True:
                try:
                    with self.lock_file.open("w") as f:
                        if LOCK_TYPE == "windows":
                            msvcrt.locking(f.fileno(), msvcrt.LK_NBLCK, 1)  # Windows non-blocking lock
                        else:
                            fcntl.flock(f, fcntl.LOCK_EX | fcntl.LOCK_NB)  # Unix non-blocking lock
                        yield  # Lock acquired
                        return  # Exit after operation completes
                except (OSError, BlockingIOError):
                    if time.time() - start_time > self.timeout:
                        print(f"Timeout: Lock on {self.lock_file.name} is stuck, force releasing it.")
                        self.lock_file.unlink(missing_ok=True)  # Remove stuck lock file
                        continue  # Retry locking after force release
                    time.sleep(0.1)  # Wait and retry

def write_json_wlock(file_path: Path, data: dict):
    """Safely writes JSON data to a file using FileLocker (thread-safe & process-safe)."""
    lock_name = f"{file_path.name}.lock"
    locker = FileLocker(lock_name)
    with locker.lock():
        with file_path.open("w", encoding="utf-8") as f:
            json.dump(data, f, indent=4)
