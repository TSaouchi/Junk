import cx_Oracle
import os
from dotenv import load_dotenv
from threading import Lock
import time

# Load environment variables from .env file
load_dotenv()

# Environment variables
DB_HOST = os.getenv("DB_HOST")
DB_PORT = os.getenv("DB_PORT", 1521)  # Default Oracle port
DB_SID = os.getenv("DB_SID")
DB_USER = os.getenv("DB_USER")
DB_PASSWORD = os.getenv("DB_PASSWORD")

class OracleDBConnectionPool:
    """Manages a connection pool to the Oracle Database."""
    _instance = None
    _lock = Lock()

    def __new__(cls):
        """Singleton pattern to ensure only one instance of the connection pool is created."""
        with cls._lock:
            if cls._instance is None:
                cls._instance = super(OracleDBConnectionPool, cls).__new__(cls)
                cls._instance._initialize_pool()
        return cls._instance

    def _initialize_pool(self):
        """Initialize the database connection pool."""
        self.pool = cx_Oracle.SessionPool(
            user=DB_USER,
            password=DB_PASSWORD,
            dsn=f"{DB_HOST}:{DB_PORT}/{DB_SID}",
            min=2,  # Minimum number of connections in the pool
            max=10,  # Maximum number of connections in the pool
            increment=1,  # Number of connections to add when needed
            threaded=True,  # Enable thread safety
        )

    def get_connection(self):
        """Get a connection from the pool."""
        return self.pool.acquire()

    def release_connection(self, conn):
        """Release a connection back to the pool."""
        self.pool.release(conn)


class OracleDBInsert:
    """Handles inserting data into the Oracle Database."""
    
    @staticmethod
    def insert_data(table_name, data):
        """Insert data into the specified table."""
        # Get connection from the pool
        connection = OracleDBConnectionPool().get_connection()
        cursor = connection.cursor()
        
        try:
            # Assuming `data` is a list of tuples representing rows to insert
            placeholders = ', '.join([':' + str(i + 1) for i in range(len(data[0]))])
            query = f"INSERT INTO {table_name} VALUES ({placeholders})"
            cursor.executemany(query, data)  # Bulk insert
            connection.commit()
            print(f"Inserted {len(data)} rows into {table_name}.")
        except cx_Oracle.DatabaseError as e:
            print(f"Error inserting data: {e}")
            connection.rollback()
        finally:
            cursor.close()
            OracleDBConnectionPool().release_connection(connection)


class OracleDBQuery:
    """Handles querying and fetching data from the Oracle Database."""

    @staticmethod
    def fetch_data(query, params=None):
        """Execute a query and return the result."""
        # Get connection from the pool
        connection = OracleDBConnectionPool().get_connection()
        cursor = connection.cursor()
        
        try:
            cursor.execute(query, params or {})
            result = cursor.fetchall()  # Fetch all rows
            return result
        except cx_Oracle.DatabaseError as e:
            print(f"Error executing query: {e}")
            return []
        finally:
            cursor.close()
            OracleDBConnectionPool().release_connection(connection)


# Example 1: Insert and Query Single Data
if __name__ == "__main__":
    # Insert single data
    table_name = 'employees'
    data = [(101, 'John Doe', 'HR', 50000)]
    OracleDBInsert.insert_data(table_name, data)
    
    # Query the inserted data
    query = "SELECT * FROM employees WHERE emp_id = :emp_id"
    params = {'emp_id': 101}
    result = OracleDBQuery.fetch_data(query, params)
    print(f"Query result: {result}")


# Example 2: Insert and Query Multiple Data Using Multithreading
import threading

# Function to insert and query data in a thread
def insert_and_query_thread(table_name, data):
    OracleDBInsert.insert_data(table_name, data)
    query = "SELECT * FROM employees WHERE department = :dept"
    params = {'dept': 'HR'}
    result = OracleDBQuery.fetch_data(query, params)
    print(f"Thread result: {result}")


if __name__ == "__main__":
    table_name = 'employees'
    data1 = [(102, 'Jane Smith', 'HR', 60000)]
    data2 = [(103, 'Bob Brown', 'Engineering', 70000)]
    
    threads = []
    
    # Create threads for insertion and querying
    for data in [data1, data2]:
        thread = threading.Thread(target=insert_and_query_thread, args=(table_name, data))
        threads.append(thread)
        thread.start()
    
    # Wait for all threads to finish
    for thread in threads:
        thread.join()

# Example 3: Insert and Query Multiple Data Using Multiprocessing
import multiprocessing

# Function to insert and query data in a process
def insert_and_query_process(table_name, data):
    OracleDBInsert.insert_data(table_name, data)
    query = "SELECT * FROM employees WHERE department = :dept"
    params = {'dept': 'HR'}
    result = OracleDBQuery.fetch_data(query, params)
    print(f"Process result: {result}")

if __name__ == "__main__":
    table_name = 'employees'
    data1 = [(104, 'Alice Green', 'HR', 75000)]
    data2 = [(105, 'Charlie White', 'Engineering', 80000)]
    
    processes = []
    
    # Create processes for insertion and querying
    for data in [data1, data2]:
        process = multiprocessing.Process(target=insert_and_query_process, args=(table_name, data))
        processes.append(process)
        process.start()
    
    # Wait for all processes to finish
    for process in processes:
        process.join()


# Example 4: Use Multiprocessing and Inside Each Process Use Multithreading
import multiprocessing
import threading

# Function to insert and query data in a thread
def insert_and_query_thread(table_name, data):
    OracleDBInsert.insert_data(table_name, data)
    query = "SELECT * FROM employees WHERE department = :dept"
    params = {'dept': 'HR'}
    result = OracleDBQuery.fetch_data(query, params)
    print(f"Thread result: {result}")

# Function to spawn threads inside each process
def insert_and_query_process_with_threads(table_name, data_list):
    threads = []
    for data in data_list:
        thread = threading.Thread(target=insert_and_query_thread, args=(table_name, data))
        threads.append(thread)
        thread.start()
    
    # Wait for all threads to finish
    for thread in threads:
        thread.join()

if __name__ == "__main__":
    table_name = 'employees'
    data1 = [(106, 'David Black', 'HR', 85000)]
    data2 = [(107, 'Emma Blue', 'Engineering', 90000)]
    
    processes = []
    
    # Create processes, each with multiple threads for insertion and querying
    for data in [data1, data2]:
        process = multiprocessing.Process(target=insert_and_query_process_with_threads, args=(table_name, [data]))
        processes.append(process)
        process.start()
    
    # Wait for all processes to finish
    for process in processes:
        process.join()
