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


@contextmanager
def file_lock(lock_name, timeout=LOCK_TIMEOUT):
    """Cross-platform file lock with timeout handling (thread-safe & process-safe)."""
    lock_file = LOCK_DIR / lock_name
    start_time = time.time()

    with thread_lock:  # Ensure thread safety within the same process
        while True:
            try:
                with lock_file.open("w") as f:
                    if LOCK_TYPE == "windows":
                        msvcrt.locking(f.fileno(), msvcrt.LK_LOCK, 1)  # Windows lock
                    else:
                        fcntl.flock(f, fcntl.LOCK_EX | fcntl.LOCK_NB)  # Unix lock
                    
                    yield  # Lock acquired
                    return  # Exit after successful operation

            except (OSError, BlockingIOError):
                if time.time() - start_time > timeout:
                    print(f"⚠ Timeout: Lock on {lock_name} is stuck, force releasing it.")
                    lock_file.unlink(missing_ok=True)  # Remove stuck lock
                    continue  # Retry locking

                time.sleep(0.1)  # Wait and retry


def write_to_file(file_path: Path, data: dict):
    """Safely writes data to a file using a lock (thread-safe & process-safe)."""
    lock_name = f"{file_path.name}.lock"

    with file_lock(lock_name):
        with file_path.open("w") as f:
            json.dump(data, f, indent=4)
        print(f"✅ Data written to {file_path}")
