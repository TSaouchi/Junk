import aiohttp
import asyncio
import os

# Define the base URL and file IDs
BASE_URL = "https://toto.dola/api/storage/substorage/{file_id}/download"
OUTPUT_FOLDER = "./downloads"  # Change this to your preferred folder
PATTERN = "pattern_{file_id}.csv"

# Ensure the output directory exists
os.makedirs(OUTPUT_FOLDER, exist_ok=True)

async def download_file(session, file_id):
    """Download a file asynchronously given a file_id."""
    url = BASE_URL.format(file_id=file_id)
    filename = PATTERN.format(file_id=file_id)
    filepath = os.path.join(OUTPUT_FOLDER, filename)

    try:
        async with session.get(url) as response:
            if response.status == 200:
                with open(filepath, "wb") as f:
                    f.write(await response.read())  # Save the file
                print(f"✅ Downloaded: {filename}")
            else:
                print(f"❌ Failed: {filename} (HTTP {response.status})")
    except Exception as e:
        print(f"⚠️ Error downloading {filename}: {e}")

async def main(file_ids):
    """Download all files concurrently."""
    async with aiohttp.ClientSession() as session:
        tasks = [download_file(session, file_id) for file_id in file_ids]
        await asyncio.gather(*tasks)  # Run downloads in parallel

# List of file IDs to download
file_ids = ["12345", "67890", "abcde"]  # Replace with your actual IDs

# Run the async download script
asyncio.run(main(file_ids))
