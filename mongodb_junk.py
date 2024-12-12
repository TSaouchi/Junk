import os
from pymongo import MongoClient, errors
from dotenv import load_dotenv
from threading import Thread, Lock
from multiprocessing import Process, Manager
import time

# Load environment variables from .env file
load_dotenv()

# Environment variables
DB_URI = os.getenv("MONGODB_URI", "mongodb://localhost:27017")  # MongoDB URI
DB_NAME = os.getenv("MONGODB_NAME", "test_db")  # Database name


class MongoDBConnectionPool:
    """Manages a connection pool to the MongoDB database."""
    _instance = None
    _lock = Lock()

    def __new__(cls):
        """Singleton pattern to ensure only one instance of the connection pool is created."""
        with cls._lock:
            if cls._instance is None:
                cls._instance = super(MongoDBConnectionPool, cls).__new__(cls)
                cls._instance._initialize_client()
        return cls._instance

    def _initialize_client(self):
        """Initialize the MongoDB client."""
        try:
            self.client = MongoClient(DB_URI, maxPoolSize=50, minPoolSize=5)
            self.db = self.client[DB_NAME]
            print(f"Connected to MongoDB database: {DB_NAME}")
        except errors.PyMongoError as e:
            print(f"Error connecting to MongoDB: {e}")
            raise

    def get_database(self):
        """Get the MongoDB database object."""
        return self.db


# Function to insert and query data
def insert_and_query(collection_name, data):
    db = MongoDBConnectionPool().get_database()
    collection = db[collection_name]
    
    # Insert data
    try:
        result = collection.insert_many(data)
        print(f"Inserted {len(result.inserted_ids)} documents into '{collection_name}'.")
    except errors.PyMongoError as e:
        print(f"Error inserting data: {e}")

    # Query data
    try:
        query_result = list(collection.find({}))
        print(f"Queried {len(query_result)} documents from '{collection_name}'.")
    except errors.PyMongoError as e:
        print(f"Error querying data: {e}")


# 1. Single Insert and Query
def single_insert_and_query():
    print("\n--- Single Insert and Query ---")
    data = [{"emp_id": 1, "name": "Alice", "department": "HR", "salary": 70000}]
    insert_and_query("employees_single", data)


# 2. Multithreading Insert and Query
def multithreading_insert_and_query():
    print("\n--- Multithreading Insert and Query ---")

    def thread_worker(thread_data):
        insert_and_query("employees_multithread", thread_data)

    threads = []
    data = [{"emp_id": i, "name": f"Emp-{i}", "department": "IT", "salary": 50000 + i} for i in range(10)]

    # Split data for multiple threads
    split_data = [data[:5], data[5:]]  # Two threads

    for chunk in split_data:
        thread = Thread(target=thread_worker, args=(chunk,))
        threads.append(thread)
        thread.start()

    for thread in threads:
        thread.join()


# 3. Multiprocessing Insert and Query
def multiprocessing_insert_and_query():
    print("\n--- Multiprocessing Insert and Query ---")

    def process_worker(process_data):
        insert_and_query("employees_multiprocess", process_data)

    processes = []
    data = [{"emp_id": i, "name": f"Emp-{i}", "department": "Sales", "salary": 40000 + i} for i in range(10)]

    # Split data for multiple processes
    split_data = [data[:5], data[5:]]  # Two processes

    for chunk in split_data:
        process = Process(target=process_worker, args=(chunk,))
        processes.append(process)
        process.start()

    for process in processes:
        process.join()


# 4. Multiprocessing with Multithreading
def multiprocessing_with_multithreading():
    print("\n--- Multiprocessing with Multithreading ---")

    def multithread_worker(process_data):
        def thread_worker(thread_data):
            insert_and_query("employees_multi_process_thread", thread_data)

        threads = []
        split_data = [process_data[:2], process_data[2:]]  # Two threads per process
        for chunk in split_data:
            thread = Thread(target=thread_worker, args=(chunk,))
            threads.append(thread)
            thread.start()

        for thread in threads:
            thread.join()

    processes = []
    data = [{"emp_id": i, "name": f"Emp-{i}", "department": "Finance", "salary": 60000 + i} for i in range(10)]

    # Split data for multiple processes
    split_data = [data[:5], data[5:]]  # Two processes

    for chunk in split_data:
        process = Process(target=multithread_worker, args=(chunk,))
        processes.append(process)
        process.start()

    for process in processes:
        process.join()


# Run all scenarios
if __name__ == "__main__":
    # 1. Single Insert and Query
    single_insert_and_query()

    # 2. Multithreading Insert and Query
    multithreading_insert_and_query()

    # 3. Multiprocessing Insert and Query
    multiprocessing_insert_and_query()

    # 4. Multiprocessing with Multithreading
    multiprocessing_with_multithreading()
