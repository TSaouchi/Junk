import asyncio
from coherence import Session, Options, NamedCache

async def main():
    # Define the gRPC proxy address
    grpc_address = 'localhost:1408'  # Replace with your proxy's address if different

    # Configure session options
    options = Options(address=grpc_address)

    # Create a session
    session = await Session.create(options)

    # Access a named cache
    cache_name = 'example-cache'  # Replace with your cache name
    cache: NamedCache[str, str] = await session.get_cache(cache_name)

    # Perform cache operations
    await cache.put('key1', 'value1')
    value = await cache.get('key1')
    print(f'Value for key1: {value}')

    # Close the session
    await session.close()

# Run the asynchronous main function
asyncio.run(main())
