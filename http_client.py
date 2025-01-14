import aiohttp

class AsyncHttpClient:
    def __init__(self, base_url: str):
        self.base_url = base_url

    async def get(self, endpoint: str, params: dict = None):
        async with aiohttp.ClientSession() as session:
            async with session.get(f"{self.base_url}{endpoint}", params=params) as response:
                response.raise_for_status()
                return await response.json()

    async def post(self, endpoint: str, data: dict = None):
        async with aiohttp.ClientSession() as session:
            async with session.post(f"{self.base_url}{endpoint}", json=data) as response:
                response.raise_for_status()
                return await response.json()

    async def put(self, endpoint: str, data: dict = None):
        async with aiohttp.ClientSession() as session:
            async with session.put(f"{self.base_url}{endpoint}", json=data) as response:
                response.raise_for_status()
                return await response.json()

    async def patch(self, endpoint: str, data: dict = None):
        async with aiohttp.ClientSession() as session:
            async with session.patch(f"{self.base_url}{endpoint}", json=data) as response:
                response.raise_for_status()
                return await response.json()

    async def delete(self, endpoint: str):
        async with aiohttp.ClientSession() as session:
            async with session.delete(f"{self.base_url}{endpoint}") as response:
                response.raise_for_status()
                return {"status": "success", "message": "Resource deleted successfully"}
