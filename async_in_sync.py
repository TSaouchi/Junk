import asyncio
import functools
from motor.motor_asyncio import AsyncIOMotorClient

import sys

# Compatibility check for Python version
if sys.version_info >= (3, 7):
    # Python 3.7 and above
    try:
        loop = asyncio.get_running_loop()  # This works if an event loop is already running
    except RuntimeError:
        loop = asyncio.new_event_loop()  # Create a new event loop if none exists
        asyncio.set_event_loop(loop)
else:
    # For Python 3.6 and below
    loop = asyncio.get_event_loop()  # This is valid in Python 3.6  
# loop: asyncio.AbstractEventLoop = asyncio.get_event_loop()
# ✅ Create a MongoDB client using a connection pool
client = AsyncIOMotorClient("mongodb://localhost:27017", maxPoolSize=10)
db = client["my_database"]
collection = db["my_collection"]

def cache_mongo(collection, key_field):
    """
    Decorator that checks MongoDB for cached results and writes asynchronously after function execution.
    """
    def decorator(func):
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            query_key = kwargs.get(key_field, args[0])  # Assume first argument is key if not in kwargs
            
            # ✅ Use the global event loop to avoid conflicts
            result = loop.run_until_complete(read_from_mongo(query_key))

            if result is not None:
                return result  # Return cached result
            
            # ✅ Execute the function in the main thread
            output = func(*args, **kwargs)  

            # ✅ Schedule the async write task safely
            asyncio.run_coroutine_threadsafe(write_to_mongo(query_key, output), loop)

            return output

        return wrapper

    return decorator

async def read_from_mongo(key):
    """Reads from MongoDB asynchronously."""
    document = await collection.find_one({"_id": key})
    return document["value"] if document else None

async def write_to_mongo(key, value):
    """Writes to MongoDB asynchronously."""
    await collection.update_one({"_id": key}, {"$set": {"value": value}}, upsert=True)

# Example Usage
@cache_mongo(collection, "param")
def expensive_function(param):
    print(f"Computing for {param} in the main thread")
    return param * 2  # Simulating an expensive computation

# Running the function
print(expensive_function(10))  # First call: Computes and stores in Mongo
print(expensive_function(10))  # Second call: Retrieves from Mongo
