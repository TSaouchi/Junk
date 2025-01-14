from aioresponses import aioresponses
import pytest
from api_engine.clients.api_x_client import ApiXClient

@pytest.mark.asyncio
async def test_update():
    client = ApiXClient(base_url="https://api.example.com")

    with aioresponses() as mock:
        mock.patch("https://api.example.com/resource/123", status=200, payload={"status": "success"})
        response = await client.update("/resource/123", {"name": "Updated Name"}, method="PATCH")
        assert response["status"] == "success"

@pytest.mark.asyncio
async def test_delete():
    client = ApiXClient(base_url="https://api.example.com")

    with aioresponses() as mock:
        mock.delete("https://api.example.com/resource/123", status=200, payload={"status": "success"})
        response = await client.delete("/resource/123")
        assert response["status"] == "success"
