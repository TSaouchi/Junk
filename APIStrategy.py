from abc import ABC, abstractmethod

class APIFactory:
    
    @staticmethod
    def get_api(api_name, api_key=None):
        if api_name.lower() == "yahoo":
            return YahooFinanceAPI()
        elif api_name.lower() == "alpha_vantage":
            return AlphaVantageAPI(api_key)
        elif api_name.lower() == "twelve_data":
            return TwelveDataAPI(api_key)
        else:
            raise ValueError(f"Unsupported API: {api_name}")

class StockAPI(ABC):
    
    @abstractmethod
    async def get_stock_data(self, symbol, session, **params):
        pass

class YahooFinanceAPI(StockAPI):
    """Yahoo Finance API implementation (unofficial)."""
    
    async def get_stock_data(self, symbol, session, **params):
        url = f"https://query1.finance.yahoo.com/v8/finance/chart/{symbol}"
        query_params = {
            "interval": params.get("interval", "1d"),
            "range": params.get("range", "5d")
        }
        async with session.get(url, params=query_params) as response:
            if response.status == 200:
                data = await response.json()
                return data.get("chart", {}).get("result", [{}])[0]
            return {"error": f"Yahoo request failed with status {response.status}"}

class AlphaVantageAPI(StockAPI):
    """Alpha Vantage API implementation."""
    
    def __init__(self, api_key):
        self.api_key = api_key
        
    async def get_stock_data(self, symbol, session, **params):
        url = "https://www.alphavantage.co/query"
        query_params = {
            "function": "TIME_SERIES_DAILY",
            "symbol": symbol,
            "apikey": self.api_key
        }
        async with session.get(url, params=query_params) as response:
            if response.status == 200:
                data = await response.json()
                return data.get("Time Series (Daily)", {})
            return {"error": f"Alpha Vantage request failed with status {response.status}"}

class TwelveDataAPI(StockAPI):
    """Twelve Data API implementation."""
    
    def __init__(self, api_key):
        self.api_key = api_key
        
    async def get_stock_data(self, symbol, session, **params):
        url = "https://api.twelvedata.com/time_series"
        query_params = {
            "symbol": symbol,
            "interval": params.get("interval", "1day"),
            "outputsize": params.get("outputsize", "5"),
            "apikey": self.api_key
        }
        async with session.get(url, params=query_params) as response:
            if response.status == 200:
                data = await response.json()
                return data.get("values", [])
            return {"error": f"Twelve Data request failed with status {response.status}"}

