import threading
import datetime
from pymongo import MongoClient, ASCENDING, InsertOne
from pymongo.errors import PyMongoError, BulkWriteError

# ---------------------------
# Authentication Manager (Singleton)
# ---------------------------
class MongoAuth:
    """
    Singleton class to manage MongoDB authentication.
    Thread-safe initialization.
    """
    _instance = None
    _lock = threading.Lock()

    def __new__(cls, *args, **kwargs):
        with cls._lock:
            if cls._instance is None:
                cls._instance = super(MongoAuth, cls).__new__(cls)
            return cls._instance

    def __init__(self, username: str = None, password: str = None,
                 host: str = 'localhost', port: int = 27017, auth_db: str = 'admin'):
        if not hasattr(self, '_initialized'):
            self.username = username
            self.password = password
            self.host = host
            self.port = port
            self.auth_db = auth_db
            self._initialized = True

    @classmethod
    def reset_instance(cls):
        """
        Reset the singleton instance to allow reconfiguration.
        """
        with cls._lock:
            cls._instance = None

    def get_connection_uri(self) -> str:
        """
        Constructs the MongoDB connection URI.
        """
        if self.username and self.password:
            return f"mongodb://{self.username}:{self.password}@{self.host}:{self.port}/?authSource={self.auth_db}"
        return f"mongodb://{self.host}:{self.port}/"

# ---------------------------
# Connection Pool Manager
# ---------------------------
class MongoConnectionPool:
    """
    Manages the MongoClient instance with tuned connection pool parameters.
    """
    def __init__(self, auth: MongoAuth, db_params: dict = None,
                 maxPoolSize: int = 200, minPoolSize: int = 20,
                 waitQueueTimeoutMS: int = 1000, connectTimeoutMS: int = 3000,
                 socketTimeoutMS: int = 5000, **kwargs):
        self.uri = auth.get_connection_uri()
        # Pool parameters combined into one dictionary
        pool_kwargs = {
            'maxPoolSize': maxPoolSize,
            'minPoolSize': minPoolSize,
            'waitQueueTimeoutMS': waitQueueTimeoutMS,
            'connectTimeoutMS': connectTimeoutMS,
            'socketTimeoutMS': socketTimeoutMS,
        }
        pool_kwargs.update(kwargs)
        try:
            self.client = MongoClient(self.uri, **pool_kwargs)
        except PyMongoError as e:
            raise Exception(f"Error creating MongoClient: {e}")
        self.db_params = db_params or {}

    def get_database(self, db_name: str) -> 'Database':
        """
        Returns the requested database. If no name is provided, uses one from db_params.
        """
        if not db_name and 'default_db' in self.db_params:
            db_name = self.db_params['default_db']
        if not db_name:
            raise ValueError("Database name must be provided either as argument or in db_params")
        return self.client[db_name]

# ---------------------------
# Query Executor
# ---------------------------
class MongoQueryExecutor:
    """
    Executes read queries on MongoDB with efficient batch processing.
    """
    def __init__(self, connection_pool: MongoConnectionPool, db_name: str):
        self.db = connection_pool.get_database(db_name)

    def find(self, collection_name: str, query: dict, projection: dict = None,
             limit: int = 0, skip: int = 0, sort: list = None, batch_size: int = 1000) -> list:
        """
        Executes a find query with optional projection, sorting, pagination, and batch size.
        """
        try:
            collection = self.db[collection_name]
            cursor = collection.find(query, projection=projection).batch_size(batch_size)
            if sort:
                cursor = cursor.sort(sort)
            if skip:
                cursor = cursor.skip(skip)
            if limit:
                cursor = cursor.limit(limit)
            return list(cursor)
        except PyMongoError as e:
            raise Exception(f"Error executing find query: {e}")

# ---------------------------
# Data Inserter with Bulk Write and Index Management
# ---------------------------
class MongoDataInserter:
    """
    Handles insert operations on MongoDB. Supports single inserts,
    bulk inserts (via bulk_write), and index management.
    """
    def __init__(self, connection_pool: MongoConnectionPool, db_name: str):
        self.db = connection_pool.get_database(db_name)

    def insert_one(self, collection_name: str, document: dict, ttl: int = None, **kwargs):
        """
        Inserts a single document.
        """
        if ttl is not None:
            document['expires_at'] = datetime.datetime.utcnow() + datetime.timedelta(seconds=ttl)
        collection = self.db[collection_name]
        try:
            result = collection.insert_one(document, **kwargs)
            return result.inserted_id
        except PyMongoError as e:
            raise Exception(f"Error inserting document: {e}")

    def insert_many(self, collection_name: str, documents: list, ttl: int = None, **kwargs):
        """
        Inserts multiple documents.
        """
        if ttl is not None:
            for doc in documents:
                doc['expires_at'] = datetime.datetime.utcnow() + datetime.timedelta(seconds=ttl)
        collection = self.db[collection_name]
        try:
            result = collection.insert_many(documents, **kwargs)
            return result.inserted_ids
        except PyMongoError as e:
            raise Exception(f"Error inserting multiple documents: {e}")

    def bulk_insert(self, collection_name: str, documents: list, ttl: int = None):
        """
        Performs a bulk insert operation using bulk_write.
        """
        if ttl is not None:
            for doc in documents:
                doc['expires_at'] = datetime.datetime.utcnow() + datetime.timedelta(seconds=ttl)
        collection = self.db[collection_name]
        operations = [InsertOne(doc) for doc in documents]
        try:
            result = collection.bulk_write(operations, ordered=False)
            return result.inserted_count
        except BulkWriteError as bwe:
            raise Exception(f"Bulk write error: {bwe.details}")

    def create_ttl_index(self, collection_name: str, field_name: str = 'expires_at', expireAfterSeconds: int = 0):
        """
        Creates a TTL index on the specified field.
        """
        collection = self.db[collection_name]
        try:
            collection.create_index(field_name, expireAfterSeconds=expireAfterSeconds)
            print(f"TTL index created on '{collection_name}.{field_name}' with expireAfterSeconds={expireAfterSeconds}.")
        except PyMongoError as e:
            raise Exception(f"Error creating TTL index: {e}")

    def ensure_index(self, collection_name: str, index_fields, index_name: str = None):
        """
        Ensures an index exists on the specified field(s).
        index_fields can be a string (for a single field) or a list of tuples for compound indexes.
        """
        collection = self.db[collection_name]
        try:
            if isinstance(index_fields, list):
                collection.create_index(index_fields, name=index_name)
            elif isinstance(index_fields, str):
                collection.create_index([(index_fields, ASCENDING)], name=index_name)
            else:
                raise ValueError("index_fields must be either a string or a list of tuples")
        except PyMongoError as e:
            raise Exception(f"Error ensuring index on {collection_name}: {e}")

# ---------------------------
# Example Usage
# ---------------------------
if __name__ == "__main__":
    # Schematic Overview:
    # ┌──────────────────────────┐
    # │ Broker Worker Process    │
    # │                          │
    # │  ┌────────────────────┐  │
    # │  │ MongoAuth Singleton│◄─┐
    # │  └────────────────────┘  │
    # │           │              │
    # │  ┌────────────────────┐  │
    # │  │ MongoConnectionPool│──┼──> Creates a tuned MongoClient
    # │  └────────────────────┘  │
    # │           │              │
    # │  ┌────────────────────┐  │
    # │  │ MongoQueryExecutor │  │
    # │  └────────────────────┘  │
    # │           │              │
    # │  ┌────────────────────┐  │
    # │  │ MongoDataInserter  │  │
    # │  └────────────────────┘  │
    # └──────────────────────────┘

    # Initialize authentication
    auth = MongoAuth(username="myUser", password="myPassword",
                     host="localhost", port=27017, auth_db="admin")
    
    # Create connection pool with tuned parameters and specify a default database
    pool = MongoConnectionPool(auth, db_params={'default_db': 'brokerDB'},
                               maxPoolSize=200, minPoolSize=20, waitQueueTimeoutMS=1000, connectTimeoutMS=3000)
    
    # Specify the target database name
    db_name = "brokerDB"
    
    # Initialize Query Executor and Data Inserter
    query_executor = MongoQueryExecutor(pool, db_name)
    data_inserter = MongoDataInserter(pool, db_name)
    
    # Ensure indexes are created only once (outside of individual operations)
    try:
        data_inserter.ensure_index("your_collection", "toto", index_name="idx_toto")
    except Exception as e:
        print(e)
    
    # Create a TTL index on the 'expires_at' field to automatically expire documents
    try:
        data_inserter.create_ttl_index("your_collection", field_name="expires_at", expireAfterSeconds=0)
    except Exception as e:
        print(e)
    
    # Example: Insert a document with a TTL of 7 days (7*24*3600 seconds)
    ttl_seconds = 7 * 24 * 3600
    document = {
        "toto": {"key1": "value1", "key2": "value2"},
        "result": {"some_key": "some_value"},
        "created_at": datetime.datetime.utcnow()
    }
    try:
        inserted_id = data_inserter.insert_one("your_collection", document, ttl=ttl_seconds)
        print(f"Document inserted with ID: {inserted_id}")
    except Exception as e:
        print(e)
    
    # Example: Bulk insert multiple documents
    documents = [
        {
            "toto": {"key1": "bulkValueA", "key2": "bulkValueB"},
            "result": {"some_key": "some_value"},
            "created_at": datetime.datetime.utcnow()
        },
        {
            "toto": {"key1": "bulkValueC", "key2": "bulkValueD"},
            "result": {"some_key": "some_value"},
            "created_at": datetime.datetime.utcnow()
        }
    ]
    try:
        count = data_inserter.bulk_insert("your_collection", documents, ttl=ttl_seconds)
        print(f"Bulk insert completed. Inserted count: {count}")
    except Exception as e:
        print(e)
    
    # Example: Execute a query with a reasonable batch size to process results efficiently
    query = {"toto.key1": "value1"}
    try:
        results = query_executor.find("your_collection", query, batch_size=500)
        print("Query Results:", results)
    except Exception as e:
        print(e)
