import asyncio
from api_engine.api_manager import APIManager
from api_engine.clients.api_x_client import ApiXClient

async def main():
    # Initialize API Manager
    api_manager = APIManager()

    # Register API clients
    api_manager.register_client("api_x", ApiXClient(base_url="https://api.example.com"))

    # Update resource
    try:
        response = await api_manager.execute(
            client_name="api_x",
            method="update",
            endpoint="/resource/123",
            data={"name": "Updated Name", "value": 42},
            method="PATCH"  # Or use "PUT"
        )
        print("Update Response:", response)

        # Delete resource
        response = await api_manager.execute(
            client_name="api_x",
            method="delete",
            endpoint="/resource/123"
        )
        print("Delete Response:", response)
    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    asyncio.run(main())
