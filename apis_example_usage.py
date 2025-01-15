import asyncio
from api_engine.api_manager import APIManager
from api_engine.clients.api_x_client import ApiXClient

async def main():
    api_manager = APIManager()
    api_manager.register_client("api_x", ApiXClient(base_url="https://api.example.com"))

    # GET Example
    response_get = await api_manager.execute(
        client_name="api_x",
        method="fetch",
        endpoint="/stocks",
        params={"symbol": "GOOGL", "interval": "1d"},
        headers={"Authorization": "Bearer YOUR_API_TOKEN"}
    )
    print("GET Response:", response_get)

    # POST Example
    response_post = await api_manager.execute(
        client_name="api_x",
        method="post",
        endpoint="/stocks/add",
        data={"symbol": "GOOGL", "price": 2734.25},
        params={"api_key": "your_api_key"},
        headers={"Content-Type": "application/json"}
    )
    print("POST Response:", response_post)

    # PATCH Example
    response_patch = await api_manager.execute(
        client_name="api_x",
        method="update",
        endpoint="/stocks/123",
        data={"price": 2800.00},
        method="PATCH",
        headers={"Authorization": "Bearer YOUR_API_TOKEN"}
    )
    print("PATCH Response:", response_patch)

    # DELETE Example
    response_delete = await api_manager.execute(
        client_name="api_x",
        method="delete",
        endpoint="/stocks/123",
        params={"api_key": "your_api_key"},
        headers={"Authorization": "Bearer YOUR_API_TOKEN"}
    )
    print("DELETE Response:", response_delete)

if __name__ == "__main__":
    asyncio.run(main())
