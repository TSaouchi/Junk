import asyncio
from api_engine.api_manager import APIManager
from yahoo_finance_client import YahooFinanceClient

async def main():
    # Initialize API Manager
    api_manager = APIManager()

    # Register Yahoo Finance client
    api_manager.register_client("yahoo_finance", YahooFinanceClient())

    # List registered clients
    clients = api_manager.list_clients()
    print("Registered Clients:", clients)

    # Fetch data using GET
    response_get = await api_manager.execute(
        client_name="yahoo_finance",
        method="fetch",
        endpoint="/GOOGL",
        params={"interval": "1d", "range": "1mo"}
    )
    print("GET Response:", response_get)

    # POST request example
    response_post = await api_manager.execute(
        client_name="yahoo_finance",
        method="post",
        endpoint="/data",
        data={"key": "value"}
    )
    print("POST Response:", response_post)

    # PUT request example
    response_put = await api_manager.execute(
        client_name="yahoo_finance",
        method="put",
        endpoint="/update",
        data={"field": "updated_value"}
    )
    print("PUT Response:", response_put)

    # DELETE request example
    response_delete = await api_manager.execute(
        client_name="yahoo_finance",
        method="delete",
        endpoint="/remove"
    )
    print("DELETE Response:", response_delete)

if __name__ == "__main__":
    asyncio.run(main())
