import json
import portalocker
import multiprocessing
import time
from pathlib import Path

TEST_FILE = Path("test_data.json")

def write_json_wlock(data):
    """Write JSON data to a file with an exclusive lock (one writer at a time)."""
    with open(TEST_FILE, "w") as f:
        print("[Writer] Acquiring lock...")
        portalocker.lock(f, portalocker.LOCK_EX)  # Exclusive lock
        print("[Writer] Lock acquired! Writing data...")
        json.dump(data, f)
        f.flush()  # Ensure data is written
        print("[Writer] Data written. Unlocking...")
        portalocker.unlock(f)  # Unlock explicitly
        print("[Writer] Unlocked!")

def read_json_wlock():
    """Read JSON data from a file with a shared lock (multiple readers allowed, no writes)."""
    with open(TEST_FILE, "r") as f:
        print("[Reader] Acquiring lock...")
        portalocker.lock(f, portalocker.LOCK_SH)  # Shared lock
        print("[Reader] Lock acquired! Reading data...")
        data = json.load(f)
        portalocker.unlock(f)  # Unlock explicitly
        print("[Reader] Data read:", data)
        print("[Reader] Unlocked!")

def writer_process():
    """Simulate a writer process."""
    sample_data = {"name": "Toufik", "age": 30, "city": "Alger"}
    write_json_wlock(sample_data)

def reader_process():
    """Simulate a reader process."""
    time.sleep(1)  # Delay to let writer start first
    read_json_wlock()

if __name__ == "__main__":
    # Ensure the file exists before reading
    TEST_FILE.touch()

    # Create processes
    writer = multiprocessing.Process(target=writer_process)
    reader = multiprocessing.Process(target=reader_process)

    # Start writer first
    writer.start()
    time.sleep(0.5)  # Ensure writer gets lock first
    reader.start()

    # Wait for both to finish
    writer.join()
    reader.join()

    print("✅ Process completed!")
