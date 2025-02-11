import signal
import datetime
from functools import wraps
from pymongo.errors import PyMongoError


class CacheProduct:
    _initialized = False  # Class-level variable to track initialization

    def __init__(self, username: str, password: str, db_name: str, host: str = 'localhost', port: int = 27017):
        if not CacheProduct._initialized:
            # Perform one-time initialization
            self.setup_shared_resources(username, password, db_name, host, port)
            CacheProduct._initialized = True
            self._setup_graceful_shutdown()

    def setup_shared_resources(self, username: str, password: str, db_name: str, host: str, port: int):
        """
        Initialize MongoDB connection pool and shared resources.
        """
        print("Initializing shared resources...")

        # MongoDB Authentication and Connection Pool
        auth = MongoAuth(username=username, password=password, host=host, port=port)
        self.connection_pool = MongoConnectionPool(auth)
        self.db_name = db_name

        # Query Executor (can be reused in the decorator)
        self.query_executor = MongoQueryExecutor(self.connection_pool, db_name=self.db_name)

        # Data Inserter for caching
        self.data_inserter = MongoDataInserter(self.connection_pool, db_name=self.db_name)

    def _setup_graceful_shutdown(self):
        """
        Set up signal handlers for graceful shutdown.
        """
        signal.signal(signal.SIGTERM, self._shutdown)
        signal.signal(signal.SIGINT, self._shutdown)

    def _shutdown(self, signum, frame):
        """
        Clean up shared resources on shutdown.
        """
        print("Shutting down gracefully...")
        if hasattr(self, 'connection_pool') and self.connection_pool:
            self.connection_pool.close()  # Close MongoDB connections
            print("MongoDB connection pool closed.")
        CacheProduct._initialized = False
        exit(0)

    def __call__(self, func):
        @wraps(func)
        def wrapper(instance, *args, **kwargs):
            # Example: Use shared query_executor or data_inserter
            try:
                # Example operation: Check cache before calling the original function
                cache_key = f"{func.__name__}_{args}_{kwargs}"
                cached_result = self.query_executor.find(
                    collection_name="cache",
                    query={"key": cache_key},
                    projection={"value": 1, "_id": 0},
                    limit=1
                )

                if cached_result:
                    print(f"Cache hit for {cache_key}")
                    return cached_result[0]["value"]

                # Call the original function and store result in cache
                result = func(instance, *args, **kwargs)
                self.data_inserter.insert_one(
                    collection_name="cache",
                    document={"key": cache_key, "value": result, "created_at": datetime.datetime.utcnow()}
                )
                return result

            except PyMongoError as e:
                raise Exception(f"MongoDB error in CacheProduct: {e}")
        
        return wrapper
