from api_engine.base_api import BaseAPI
from api_engine.utils.http_client import AsyncHttpClient

class ApiXClient(BaseAPI):
    def __init__(self, base_url: str):
        self.http_client = AsyncHttpClient(base_url)

    async def fetch(self, endpoint: str, params: dict = None):
        return await self.http_client.get(endpoint, params)

    async def post(self, endpoint: str, data: dict = None):
        return await self.http_client.post(endpoint, data)

    async def update(self, endpoint: str, data: dict, method: str = "PUT"):
        if method == "PUT":
            return await self.http_client.put(endpoint, data)
        elif method == "PATCH":
            return await self.http_client.patch(endpoint, data)
        else:
            raise ValueError("Invalid method for update. Use 'PUT' or 'PATCH'.")

    async def delete(self, endpoint: str):
        return await self.http_client.delete(endpoint)
