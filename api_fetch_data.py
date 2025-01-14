import asyncio
import aiohttp

from APIs_configuration import *

async def fetch_all_stock_data(symbols, api_names, apis_config):
    results = {symbol: {} for symbol in symbols}
    
    async with aiohttp.ClientSession() as session:
        tasks = []
        
        # Schedule fetching tasks for each symbol and API combination
        for symbol in symbols:
            for api_name in api_names:
                api_key = apis_config.get(api_name, {}).get("api_key")
                params = apis_config.get(api_name, {}).get("params", {})
                api_instance = APIFactory.get_api(api_name, api_key)
                
                # Create and store a coroutine for each API call
                tasks.append((symbol, api_name, api_instance.get_stock_data(symbol, session, **params)))
        
        # Gather all results asynchronously
        responses = await asyncio.gather(*[task[2] for task in tasks])
        
        # Structure the results by symbol and API name
        for (symbol, api_name, _), response in zip(tasks, responses):
            results[symbol][api_name] = response
    
    return results

if __name__ == '__main__':
    # Example usage
    async def main():
        symbols = ["NVDA", "AAPL", "MSFT"]
        api_names = ["yahoo"]#, "alpha_vantage", "twelve_data"]
        
        apis_config = {
            "yahoo": {
                "params": {"interval": "1d", "range": "5d"}
            },
            # "alpha_vantage": {
            #     "api_key": "YOUR_ALPHA_VANTAGE_API_KEY",
            #     "params": {}
            # },
            # "twelve_data": {
            #     "api_key": "YOUR_TWELVE_DATA_API_KEY",
            #     "params": {"interval": "1day", "outputsize": "5"}
            # }
        }
        
        results = await fetch_all_stock_data(symbols, api_names, apis_config)
        for symbol, api_results in results.items():
            print(f"\nResults for {symbol}:")
            for api_name, data in api_results.items():
                print(f"{api_name}: {data}")

    # Run the main async function
    asyncio.run(main())
