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
# Data Deleter with Extended Deletion and Collection Drop Logic
# ---------------------------
class MongoDataDeleter:
    """
    Handles deletion operations on MongoDB, including dropping empty collections.
    """
    def __init__(self, connection_pool: MongoConnectionPool, db_name: str):
        self.db = connection_pool.get_database(db_name)

    def delete_one(self, collection_name: str, query: dict):
        """
        Deletes a single document matching the query.
        """
        collection = self.db[collection_name]
        try:
            result = collection.delete_one(query)
            return result.deleted_count
        except PyMongoError as e:
            raise Exception(f"Error deleting document: {e}")

    def delete_many(self, collection_name: str, query: dict):
        """
        Deletes multiple documents matching the query.
        """
        collection = self.db[collection_name]
        try:
            result = collection.delete_many(query)
            return result.deleted_count
        except PyMongoError as e:
            raise Exception(f"Error deleting multiple documents: {e}")

    def drop_collection(self, collection_name: str):
        """
        Drops an entire collection.
        """
        try:
            self.db.drop_collection(collection_name)
            print(f"Collection '{collection_name}' dropped successfully.")
        except PyMongoError as e:
            raise Exception(f"Error dropping collection: {e}")

    def drop_empty_collections(self):
        """
        Drops all empty collections in the database.
        """
        try:
            for collection_name in self.db.list_collection_names():
                if self.db[collection_name].estimated_document_count() == 0:
                    self.drop_collection(collection_name)
        except PyMongoError as e:
            raise Exception(f"Error dropping empty collections: {e}")


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
            
    def aggregate(self, collection_name: str, pipeline: list, batch_size: int = 1000) -> list:
        """
        Executes an aggregation query with the specified pipeline.
        """
        try:
            collection = self.db[collection_name]
            cursor = collection.aggregate(pipeline, batchSize=batch_size)
            return list(cursor)
        except PyMongoError as e:
            raise Exception(f"Error executing aggregation query: {e}")

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
