import json
import portalocker
import multiprocessing
import time
from pathlib import Path

class LockJSONFileManager:
    """
    Manages reading and writing JSON files with file locking using portalocker."""
    
    def __init__(self, file_path):
        self.file_path = Path(file_path)
        # Ensure the file exists to avoid FileNotFoundError
        if not self.file_path.exists():
            self.file_path.write_text(json.dumps({}))

    def write(self, data):
        """
        Write JSON data to a file with an exclusive lock (single writer).
        """
        with open(self.file_path, "w") as f:
            portalocker.lock(f, portalocker.LOCK_EX)  # Lock for writing
            json.dump(data, f)
            f.flush()  
            portalocker.unlock(f) 

    def read(self):
        """
        Read JSON data from a file with a shared lock (multiple readers allowed)."""
        with open(self.file_path, "r") as f:
            portalocker.lock(f, portalocker.LOCK_SH)  # Lock for reading
            data = json.load(f)
            portalocker.unlock(f)  # Unlock explicitly
        return data

# Example functions for multiprocessing
def writer_process(json_manager):
    """Simulate a writer process."""
    sample_data = {"name": "Toufik", "age": 30, "city": "Alger"}
    json_manager.write(sample_data)

def reader_process(json_manager):
    """Simulate a reader process."""
    time.sleep(1)  # Allow writer to start first
    json_manager.read()

if __name__ == "__main__":
    TEST_FILE = "test_data.json"
    
    # Create an instance of JSONFileManager
    json_manager = LockJSONFileManager(TEST_FILE)

    # Create processes
    writer = multiprocessing.Process(target=writer_process, args=(json_manager,))
    reader = multiprocessing.Process(target=reader_process, args=(json_manager,))

    # Start writer first
    writer.start()
    time.sleep(0.5)  # Ensure writer gets lock first
    reader.start()

    # Wait for both to finish
    writer.join()
    reader.join()

    print("✅ Process completed!")
