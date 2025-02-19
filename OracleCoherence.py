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

----------------------------------------------------------------------------------------------------
import asyncio
from coherence import Session, Options, SessionLifecycleEvent

async def on_connected(event: SessionLifecycleEvent):
    print("Session connected to the Coherence cluster.")

async def on_disconnected(event: SessionLifecycleEvent):
    print("Session disconnected from the Coherence cluster.")

async def on_reconnected(event: SessionLifecycleEvent):
    print("Session reconnected to the Coherence cluster.")

async def on_closed(event: SessionLifecycleEvent):
    print("Session closed.")

async def main():
    # Define the address of the Coherence gRPC proxy
    address = 'your_ip_address:your_port_number'

    # Create session options with the specified address
    options = Options(address=address)

    # Create a session with the specified options
    session = await Session.create(session_options=options)

    # Register event listeners
    session.add_lifecycle_listener(SessionLifecycleEvent.Type.CONNECTED, on_connected)
    session.add_lifecycle_listener(SessionLifecycleEvent.Type.DISCONNECTED, on_disconnected)
    session.add_lifecycle_listener(SessionLifecycleEvent.Type.RECONNECTED, on_reconnected)
    session.add_lifecycle_listener(SessionLifecycleEvent.Type.CLOSED, on_closed)

    # Wait for the session to be connected
    while not session.is_connected():
        print("Waiting for session to connect...")
        await asyncio.sleep(1)

    print("Session is now connected and ready for operations.")

    # Access the specified cache
    cache_name = 'your_cache_name'
    cache = await session.get_cache(cache_name)

    # Example operations
    await cache.put('key', 'value')
    result = await cache.get('key')
    print(f'Value for "key": {result}')

    # Close the session when done
    await session.close()

# Run the asynchronous main function
asyncio.run(main())

---------------------------------------------------------------------------
import asyncio

async def check_connection(host, port):
    try:
        # Try to establish a connection to the host and port
        reader, writer = await asyncio.open_connection(host, port)
        print(f"Connection established with {host}:{port}")
        writer.close()  # Close the connection after the check
        await writer.wait_closed()
        return True
    except Exception as e:
        print(f"Connection failed: {e}")
        return False

# Usage
address = 'your_ip_address'
port = 'your_port_number'
asyncio.run(check_connection(address, port))

-----------------------------------------------------------------------------
import grpc
from grpc_health_v1 import health_pb2_grpc, health_pb2

def check_grpc_health(address):
    # Create a gRPC channel and stub for the health check service
    channel = grpc.insecure_channel(address)
    stub = health_pb2_grpc.HealthStub(channel)

    try:
        # Make the health check request
        response = stub.Check(health_pb2.HealthCheckRequest(service=''))
        if response.status == health_pb2.HealthCheckResponse.Serving:
            print("gRPC server is healthy and serving requests.")
            return True
        else:
            print("gRPC server is not healthy.")
            return False
    except grpc.RpcError as e:
        print(f"Error during health check: {e}")
        return False

# Usage
address = 'your_ip_address:your_port_number'
check_grpc_health(address)
