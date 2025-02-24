import json
import pytest
import portalocker
from pathlib import Path
from multiprocessing import Process
from time import sleep
from file_locking import LockJSONFileManager as JSONFileManager

TEST_FILE = Path("test_data.json")

@pytest.fixture
def json_manager():
    """Fixture to set up and tear down the JSONFileManager instance."""
    # Ensure a fresh file before each test
    if TEST_FILE.exists():
        TEST_FILE.unlink()  # Remove the file if it exists
    # Create the manager instance
    manager = JSONFileManager(TEST_FILE)
    yield manager
    # Cleanup after test
    if TEST_FILE.exists():
        TEST_FILE.unlink()  # Remove the file after each test


def writer(json_manager, data_writer):
    """Function to write data to the file."""
    json_manager.write(data_writer)


def reader(json_manager, data_writer):
    """Function to read data from the file."""
    sleep(0.5)  # Allow writer to acquire the lock first
    read_data = json_manager.read()
    assert read_data == data_writer, f"Expected {data_writer}, but got {read_data}"


def test_write_data(json_manager):
    """Test writing data to the file with lock."""
    data = {"name": "Toufik", "age": 30, "city": "Alger"}

    json_manager.write(data)
    
    # Read the data back to verify the write operation
    with open(TEST_FILE, "r") as f:
        portalocker.lock(f, portalocker.LOCK_SH)  # Lock for reading
        read_data = json.load(f)
        portalocker.unlock(f)
    
    assert read_data == data, f"Expected {data}, but got {read_data}"


def test_read_data(json_manager):
    """Test reading data from the file."""
    data = {"name": "Toufik", "age": 30, "city": "Alger"}

    # Write data first
    json_manager.write(data)

    # Now test reading data
    read_data = json_manager.read()
    
    assert read_data == data, f"Expected {data}, but got {read_data}"


def test_concurrent_write_read(json_manager):
    """Test simultaneous read and write operations."""
    data_writer = {"name": "Toufik", "age": 30, "city": "Alger"}

    # Create writer and reader processes
    writer_process = Process(target=writer, args=(json_manager, data_writer))
    reader_process = Process(target=reader, args=(json_manager, data_writer))

    # Start both processes
    writer_process.start()
    reader_process.start()

    # Wait for them to finish
    writer_process.join()
    reader_process.join()
