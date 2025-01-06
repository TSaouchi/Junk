from diskcache import Cache
import uuid
from datetime import datetime


class IDGenerator:
    """Utility to generate unique IDs."""
    @staticmethod
    def generate(prefix: str) -> str:
        return f"{prefix}_{uuid.uuid4().hex[:8]}"


class Client:
    """Represents a client."""
    def __init__(self, client_id: str, name: str, email: str):
        self.client_id = client_id
        self.name = name
        self.email = email
        self.requests = []  # List of request IDs


class Request:
    """Represents a client request."""
    def __init__(self, request_id: str, client_id: str, operation: str, parameters: list, result_id: str):
        self.request_id = request_id
        self.client_id = client_id
        self.operation = operation
        self.parameters = parameters
        self.result_id = result_id
        self.timestamp = datetime.utcnow().isoformat()


class Result:
    """Represents a result for a request."""
    def __init__(self, result_id: str, status: str, output: any):
        self.result_id = result_id
        self.status = status
        self.output = output
        self.timestamp = datetime.utcnow().isoformat()


class CacheRepository:
    """Generic repository to manage DiskCache."""
    def __init__(self, cache_name: str, cache_location: str = None):
        # Set the cache directory
        if cache_location:
            os.makedirs(cache_location, exist_ok=True)  # Ensure directory exists
            cache_path = os.path.join(cache_location, cache_name)
        else:
            cache_path = cache_name  # Default location in current working directory

        self.cache = Cache(cache_path)

    def add(self, key: str, value: any):
        self.cache[key] = value

    def get(self, key: str):
        return self.cache.get(key)

    def update(self, key: str, value: any):
        self.cache[key] = value

    def exists(self, key: str) -> bool:
        return key in self.cache


class ClientService:
    """Service to manage clients."""
    def __init__(self, client_repository: CacheRepository):
        self.client_repository = client_repository

    def add_client(self, name: str, email: str) -> str:
        client_id = IDGenerator.generate("client")
        client = Client(client_id, name, email)
        self.client_repository.add(client_id, client)
        return client_id

    def get_client(self, client_id: str) -> Client:
        client = self.client_repository.get(client_id)
        if not client:
            raise ValueError(f"Client with ID {client_id} does not exist.")
        return client


class RequestService:
    """Service to manage requests."""
    def __init__(self, request_repository: CacheRepository, client_service: ClientService):
        self.request_repository = request_repository
        self.client_service = client_service

    def add_request(self, client_id: str, operation: str, parameters: list) -> tuple:
        # Ensure client exists
        client = self.client_service.get_client(client_id)

        # Generate IDs
        request_id = IDGenerator.generate("request")
        result_id = IDGenerator.generate("result")

        # Create request and save it
        request = Request(request_id, client_id, operation, parameters, result_id)
        self.request_repository.add(request_id, request)

        # Update the client with the new request
        client.requests.append(request_id)
        self.client_service.client_repository.update(client_id, client)

        return request_id, result_id

    def get_request(self, request_id: str) -> Request:
        request = self.request_repository.get(request_id)
        if not request:
            raise ValueError(f"Request with ID {request_id} does not exist.")
        return request


class ResultService:
    """Service to manage results."""
    def __init__(self, result_repository: CacheRepository):
        self.result_repository = result_repository

    def add_result(self, result_id: str, status: str, output: any):
        result = Result(result_id, status, output)
        self.result_repository.add(result_id, result)

    def get_result(self, result_id: str) -> Result:
        result = self.result_repository.get(result_id)
        if not result:
            raise ValueError(f"Result with ID {result_id} does not exist.")
        return result


class ClientRequestResultFacade:
    """Facade to manage all operations."""
    def __init__(self, client_service: ClientService, request_service: RequestService, result_service: ResultService):
        self.client_service = client_service
        self.request_service = request_service
        self.result_service = result_service

    def add_client(self, name: str, email: str) -> str:
        return self.client_service.add_client(name, email)

    def add_request(self, client_id: str, operation: str, parameters: list) -> tuple:
        return self.request_service.add_request(client_id, operation, parameters)

    def add_result(self, result_id: str, status: str, output: any):
        self.result_service.add_result(result_id, status, output)

    def get_client_data(self, client_id: str):
        client = self.client_service.get_client(client_id)
        requests = [self.request_service.get_request(req_id) for req_id in client.requests]
        return {
            "client_info": vars(client),
            "requests": [vars(req) for req in requests]
        }

    def get_request_data(self, request_id: str):
        request = self.request_service.get_request(request_id)
        result = self.result_service.get_result(request.result_id)
        return {
            "request_data": vars(request),
            "result_data": vars(result)
        }


if __name__ == "__main__":
    # Specify custom cache location
    custom_cache_dir = r"C:\mycache\application_toto"

    # Initialize repositories
    client_repo = CacheRepository("clients_cache", custom_cache_dir)
    request_repo = CacheRepository("requests_cache", custom_cache_dir)
    result_repo = CacheRepository("results_cache", custom_cache_dir)

    # Initialize services
    client_service = ClientService(client_repo)
    request_service = RequestService(request_repo, client_service)
    result_service = ResultService(result_repo)

    # Initialize facade
    facade = ClientRequestResultFacade(client_service, request_service, result_service)

    # Add a client
    client_id = facade.add_client("John Doe", "john.doe@example.com")
    print(f"Client ID: {client_id}")

    # Add a request for the client
    request_id, result_id = facade.add_request(client_id, "compute_sum", [1, 2, 3])
    print(f"Request ID: {request_id}, Result ID: {result_id}")

    # Add a result for the request
    facade.add_result(result_id, "success", 6)
    print(f"Result added for ID: {result_id}")

    # Fetch client data
    client_data = facade.get_client_data(client_id)
    print("Client Data:", client_data)

    # Fetch request data
    request_data = facade.get_request_data(request_id)
    print("Request Data:", request_data)
