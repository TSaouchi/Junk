import os
import json
import uuid
import fcntl
import typing
from datetime import datetime, timedelta
from diskcache import Cache
from dataclasses import dataclass
from typing import Optional, Tuple, Any

@dataclass
class RequestData:
    client_name: str
    operation: str
    params: list
    timestamp: datetime
    result_id: Optional[str] = None

    def to_dict(self) -> dict:
        return {
            'client_name': self.client_name,
            'operation': self.operation,
            'params': self.params,
            'timestamp': self.timestamp.isoformat(),
            'result_id': self.result_id
        }

    @classmethod
    def from_dict(cls, data: dict) -> 'RequestData':
        data['timestamp'] = datetime.fromisoformat(data['timestamp'])
        return cls(**data)

class IDGenerator:
    """Utility to generate unique IDs with prefix validation."""
    VALID_PREFIXES = {'request', 'result'}
    
    @staticmethod
    def generate(prefix: str) -> str:
        if prefix not in IDGenerator.VALID_PREFIXES:
            raise ValueError(f"Invalid prefix. Must be one of: {IDGenerator.VALID_PREFIXES}")
        return f"{prefix}_{uuid.uuid4().hex[:8]}"

class ResultRepository:
    """Handles the storage of results in JSON format with file locking."""
    def __init__(self, cache_location: str, max_size_mb: int = 1000):
        self.result_dir = os.path.join(cache_location, "results")
        os.makedirs(self.result_dir, exist_ok=True)
        self.max_size_bytes = max_size_mb * 1024 * 1024

    def _check_size_limit(self, data_size: int):
        """Check if adding new data would exceed size limit."""
        current_size = sum(
            os.path.getsize(os.path.join(self.result_dir, f))
            for f in os.listdir(self.result_dir)
            if os.path.isfile(os.path.join(self.result_dir, f))
        )
        if current_size + data_size > self.max_size_bytes:
            raise ValueError(f"Storage limit of {self.max_size_bytes // 1024 // 1024}MB would be exceeded")

    def store_result(self, result_id: str, result_object: dict):
        """Store result in JSON format with file locking."""
        result_path = os.path.join(self.result_dir, f"{result_id}.json")
        
        # Check size before writing
        data = json.dumps(result_object).encode('utf-8')
        self._check_size_limit(len(data))
        
        with open(result_path, "wb") as file:
            # Acquire exclusive lock
            fcntl.flock(file.fileno(), fcntl.LOCK_EX)
            try:
                file.write(data)
            finally:
                fcntl.flock(file.fileno(), fcntl.LOCK_UN)

    def retrieve_result(self, result_id: str) -> dict:
        """Retrieve result with file locking."""
        result_path = os.path.join(self.result_dir, f"{result_id}.json")
        if not os.path.exists(result_path):
            raise ValueError(f"Result with ID {result_id} does not exist.")
            
        with open(result_path, "rb") as file:
            # Acquire shared lock
            fcntl.flock(file.fileno(), fcntl.LOCK_SH)
            try:
                return json.loads(file.read().decode('utf-8'))
            finally:
                fcntl.flock(file.fileno(), fcntl.LOCK_UN)

    def delete_result(self, result_id: str):
        """Delete result with proper error handling."""
        result_path = os.path.join(self.result_dir, f"{result_id}.json")
        try:
            if os.path.exists(result_path):
                os.remove(result_path)
        except OSError as e:
            raise RuntimeError(f"Failed to delete result {result_id}: {e}")

class RequestRepository:
    """Handles the storage of requests with TTL."""
    def __init__(self, cache_location: str, ttl_days: int = 30):
        os.makedirs(cache_location, exist_ok=True)
        self.cache = Cache(cache_location)
        self.ttl = ttl_days * 24 * 60 * 60  # Convert days to seconds

    def store_request(self, request_id: str, request_data: RequestData):
        """Store request with TTL."""
        self.cache.set(request_id, request_data.to_dict(), expire=self.ttl)

    def retrieve_request(self, request_id: str) -> RequestData:
        """Retrieve request data."""
        data = self.cache.get(request_id)
        if data is None:
            raise ValueError(f"Request with ID {request_id} does not exist or has expired.")
        return RequestData.from_dict(data)

    def cleanup_expired(self):
        """Clean up expired entries."""
        self.cache.expire()

class CacheManager:
    """Manages requests and results with improved error handling and validation."""
    def __init__(
        self,
        request_repository: RequestRepository,
        result_repository: ResultRepository,
        max_request_size: int = 1024 * 1024  # 1MB
    ):
        self.request_repository = request_repository
        self.result_repository = result_repository
        self.max_request_size = max_request_size

    def _validate_data_size(self, data: dict) -> int:
        """Validate data size."""
        size = len(json.dumps(data).encode('utf-8'))
        if size > self.max_request_size:
            raise ValueError(f"Data size ({size} bytes) exceeds maximum allowed size ({self.max_request_size} bytes)")
        return size

    def add_request_and_result(
        self,
        request_data: dict,
        result_data: dict
    ) -> Tuple[str, str]:
        """Add request and result with validation and error handling."""
        # Validate data sizes
        self._validate_data_size(request_data)
        self._validate_data_size(result_data)

        # Generate IDs
        request_id = IDGenerator.generate("request")
        result_id = IDGenerator.generate("result")

        try:
            # Create RequestData object
            request = RequestData(
                client_name=request_data['client_name'],
                operation=request_data['operation'],
                params=request_data['params'],
                timestamp=datetime.now(),
                result_id=result_id
            )

            with self.request_repository.cache.transact():
                # Store result first
                self.result_repository.store_result(result_id, result_data)
                # Store request
                self.request_repository.store_request(request_id, request)

            return request_id, result_id

        except Exception as e:
            # Cleanup in case of error
            self.result_repository.delete_result(result_id)
            raise RuntimeError(f"Failed to store request and result: {str(e)}") from e

    def get_request_with_result(self, request_id: str) -> dict:
        """Retrieve request and result with proper error handling."""
        try:
            request_data = self.request_repository.retrieve_request(request_id)
            result = None
            
            if request_data.result_id:
                result = self.result_repository.retrieve_result(request_data.result_id)
            
            response = request_data.to_dict()
            response['result'] = result
            return response

        except Exception as e:
            raise RuntimeError(f"Failed to retrieve request {request_id}: {str(e)}") from e

    def cleanup(self):
        """Perform cleanup operations."""
        self.request_repository.cleanup_expired()

# Usage example
if __name__ == "__main__":
    cache_location = "C:/mycache/application_toto"
    request_repo = RequestRepository(cache_location, ttl_days=30)
    result_repo = ResultRepository(cache_location, max_size_mb=1000)
    
    cache_manager = CacheManager(
        request_repo,
        result_repo,
        max_request_size=1024 * 1024  # 1MB
    )
    
    # Example request and result
    request_data = {
        "client_name": "Alice",
        "operation": "compute_sum",
        "params": [10, 20, 30]
    }
    
    result_data = {
        "status": "success",
        "value": 60,
        "timestamp": datetime.now().isoformat()
    }

    try:
        # Add request and result atomically
        request_id, result_id = cache_manager.add_request_and_result(request_data, result_data)

        # Retrieve the saved request and result
        request = cache_manager.get_request_with_result(request_id)
        print("Retrieved Request:", request)

        # Cleanup expired entries
        cache_manager.cleanup()

    except Exception as e:
        print(f"An error occurred: {e}")
