import os
import pickle
import uuid
from diskcache import Cache


class IDGenerator:
    """Utility to generate unique IDs."""
    @staticmethod
    def generate(prefix: str) -> str:
        return f"{prefix}_{uuid.uuid4().hex[:8]}"


class ResultRepository:
    """Handles the storage of results in binary format."""
    def __init__(self, cache_location: str):
        os.makedirs(cache_location, exist_ok=True)
        self.result_dir = os.path.join(cache_location, "results")
        os.makedirs(self.result_dir, exist_ok=True)

    def store_result(self, result_id: str, result_object: dict):
        """Serialize and store result in binary format."""
        result_path = os.path.join(self.result_dir, f"{result_id}.bin")
        with open(result_path, "wb") as file:
            pickle.dump(result_object, file)

    def retrieve_result(self, result_id: str) -> dict:
        """Deserialize and return the result object."""
        result_path = os.path.join(self.result_dir, f"{result_id}.bin")
        if not os.path.exists(result_path):
            raise ValueError(f"Result with ID {result_id} does not exist.")
        with open(result_path, "rb") as file:
            return pickle.load(file)


class RequestRepository:
    """Handles the storage of requests."""
    def __init__(self, cache_location: str):
        os.makedirs(cache_location, exist_ok=True)
        self.cache = Cache(cache_location)

    def store_request(self, request_id: str, request_data: dict):
        """Store the request data."""
        self.cache[request_id] = request_data

    def retrieve_request(self, request_id: str) -> dict:
        """Retrieve the request data."""
        if request_id not in self.cache:
            raise ValueError(f"Request with ID {request_id} does not exist.")
        return self.cache[request_id]

    def update_request(self, request_id: str, updated_data: dict):
        """Update an existing request."""
        self.cache[request_id] = updated_data


class CacheManager:
    """Manages requests and results."""
    def __init__(self, request_repository: RequestRepository, result_repository: ResultRepository):
        self.request_repository = request_repository
        self.result_repository = result_repository

    def add_request_and_result(self, request: dict, result: dict) -> (str, str):
        """
        Adds a request and its corresponding result.
        - The result is stored in binary format.
        - The request stores a reference to the result (result ID).

        Args:
            request (dict): The request data.
            result (dict): The result data.

        Returns:
            tuple: (request_id, result_id)
        """
        # Generate unique IDs
        request_id = IDGenerator.generate("request")
        result_id = IDGenerator.generate("result")

        # Store the result in binary format
        self.result_repository.store_result(result_id, result)

        # Add a reference to the result in the request
        request["result"] = result_id

        # Store the request
        self.request_repository.store_request(request_id, request)

        return request_id, result_id

    def get_request_with_result(self, request_id: str) -> dict:
        """
        Retrieves a request along with its resolved result.

        Args:
            request_id (str): The unique ID of the request.

        Returns:
            dict: The request data with the result included.
        """
        # Retrieve the request
        request_data = self.request_repository.retrieve_request(request_id)

        # Retrieve the result if it exists
        if "result" in request_data and request_data["result"]:
            result_id = request_data["result"]
            request_data["result"] = self.result_repository.retrieve_result(result_id)

        return request_data

if __name__ == "__main__":
    # Define cache location
    cache_location = r"C:\mycache\application_toto"

    # Initialize repositories
    request_repo = RequestRepository(os.path.join(cache_location, "requests"))
    result_repo = ResultRepository(cache_location)

    # Initialize cache manager
    cache_manager = CacheManager(request_repo, result_repo)

    # Example: Adding a request and result
    request = {
        "client_name": "Alice",
        "operation": "compute_sum",
        "params": [10, 20, 30]
    }
    result = {
        "status": "success",
        "value": 60
    }

    # Add request and result to the cache
    request_id, result_id = cache_manager.add_request_and_result(request, result)
    print(f"Request ID: {request_id}, Result ID: {result_id}")

    # Retrieve the request along with its result
    request_with_result = cache_manager.get_request_with_result(request_id)
    print("Retrieved Request with Result:")
    print(request_with_result)
