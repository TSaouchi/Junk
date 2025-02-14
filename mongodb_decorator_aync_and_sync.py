import functools
import datetime
import asyncio
from motor.motor_asyncio import AsyncIOMotorClient

# Assume MongoAuth, MongoConnectionPool, MongoDataInserter are available

def mongo_cache_decorator(func):
    """
    Decorator to cache function result in MongoDB, create TTL index, and another index on the document.
    This is designed to wrap both async and non-async functions and integrate with MongoDB operations.
    """
    @functools.wraps(func)
    async def async_wrapper(*args, **kwargs):
        """
        Wrapper for asynchronous functions.
        """
        # Step 1: Call the async function and await the result
        result = await func(*args, **kwargs)

        # Step 2: Process result (this is where you might manipulate or modify the result)
        # For simplicity, let's assume result is a dictionary
        document = {"result": result, "created_at": datetime.datetime.utcnow()}

        # Step 3: Initialize MongoDB connection and insert the result
        try:
            # MongoDB connection parameters (adjust these as needed)
            mongo_auth = MongoAuth(username="your_username", password="your_password", host="localhost", port=27017)
            connection_pool = MongoConnectionPool(mongo_auth)
            db_name = "your_db"
            inserter = MongoDataInserter(connection_pool, db_name)

            # Step 4: Insert the document into MongoDB
            collection_name = "your_collection"
            ttl = 3600  # Set TTL in seconds (1 hour in this example)
            insert_result = await inserter.insert_one(collection_name, document, ttl=ttl)

            # Step 5: Create TTL index and another index on the document field
            await inserter.create_ttl_index(collection_name, field_name="expires_at", expireAfterSeconds=ttl)
            await inserter.ensure_index(collection_name, index_fields="result", index_name="result_index")

            # Step 6: Return the original function result
            return result

        except Exception as e:
            print(f"Error during MongoDB operations: {e}")
            return result  # Return the original result even if MongoDB operations fail

    def sync_wrapper(*args, **kwargs):
        """
        Wrapper for synchronous functions.
        """
        # Step 1: Call the synchronous function to get the result
        result = func(*args, **kwargs)

        # Step 2: Process result (this is where you might manipulate or modify the result)
        document = {"result": result, "created_at": datetime.datetime.utcnow()}

        # Step 3: Initialize MongoDB connection and insert the result
        try:
            # MongoDB connection parameters (adjust these as needed)
            mongo_auth = MongoAuth(username="your_username", password="your_password", host="localhost", port=27017)
            connection_pool = MongoConnectionPool(mongo_auth)
            db_name = "your_db"
            inserter = MongoDataInserter(connection_pool, db_name)

            # Step 4: Insert the document into MongoDB
            collection_name = "your_collection"
            ttl = 3600  # Set TTL in seconds (1 hour in this example)
            insert_result = asyncio.run(inserter.insert_one(collection_name, document, ttl=ttl))

            # Step 5: Create TTL index and another index on the document field
            asyncio.run(inserter.create_ttl_index(collection_name, field_name="expires_at", expireAfterSeconds=ttl))
            asyncio.run(inserter.ensure_index(collection_name, index_fields="result", index_name="result_index"))

            # Step 6: Return the original function result
            return result

        except Exception as e:
            print(f"Error during MongoDB operations: {e}")
            return result  # Return the original result even if MongoDB operations fail

    # Check if the wrapped function is async or sync and return the appropriate wrapper
    if asyncio.iscoroutinefunction(func):
        return async_wrapper
    return sync_wrapper

# Example of how to use the decorator for async functions
@mongo_cache_decorator
async def some_async_function():
    # Simulate some async computation
    return {"data": "async result"}

# Example of how to use the decorator for sync functions
@mongo_cache_decorator
def some_sync_function():
    # Simulate some sync computation
    return {"data": "sync result"}

# Call the functions
async def test_async():
    result = await some_async_function()
    print(result)

def test_sync():
    result = some_sync_function()
    print(result)

# Run the async test
asyncio.run(test_async())

# Run the sync test
test_sync()
