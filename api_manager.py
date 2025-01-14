class APIManager:
    def __init__(self):
        self.clients = {}

    def register_client(self, name: str, client):
        self.clients[name] = client

    def get_client(self, name: str):
        if name not in self.clients:
            raise ValueError(f"API client '{name}' is not registered.")
        return self.clients[name]

    async def execute(self, client_name: str, method: str, endpoint: str, **kwargs):
        client = self.get_client(client_name)
        method_func = getattr(client, method)
        return await method_func(endpoint, **kwargs)
