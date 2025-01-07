from abc import ABC, abstractmethod
import os
import json
import hashlib
import pickle
import sqlite3
import bson
from datetime import datetime, timedelta
from pathlib import Path
import threading
import logging
from functools import wraps
from contextlib import contextmanager
from dataclasses import dataclass

@dataclass
class CacheEntry:
    """Value object representing a cache entry"""
    request_key: str
    request: dict
    result_key: str
    created_at: datetime
    expires_at: datetime

@dataclass
class CacheConfig:
    """Configuration value object"""
    cache_dir: Path
    max_cache_size_bytes: int
    default_expiration: timedelta
    serialization_method: str  # 'pickle', 'bson', or 'ascii'
    
    @classmethod
    def create(cls, 
               cache_dir=".cache", 
               max_cache_size_mb=500,
               default_expiration_days=30,
               serialization_method='pickle'):
        if serialization_method not in ['pickle', 'bson', 'ascii']:
            raise ValueError("serialization_method must be 'pickle', 'bson', or 'ascii'")
        
        return cls(
            cache_dir=Path(cache_dir),
            max_cache_size_bytes=max_cache_size_mb * 1024 * 1024,
            default_expiration=timedelta(days=default_expiration_days),
            serialization_method=serialization_method
        )

class IKeyGenerator(ABC):
    """Strategy interface for key generation"""
    @abstractmethod
    def generate_key(self, data):
        pass

class ISerializer(ABC):
    """Strategy interface for serialization"""
    @abstractmethod
    def serialize(self, data):
        pass
    
    @abstractmethod
    def deserialize(self, data):
        pass
    
    @abstractmethod
    def get_extension(self):
        pass

class PickleSerializer(ISerializer):
    """Pickle-based serializer"""
    def serialize(self, data):
        return pickle.dumps(data)
    
    def deserialize(self, data):
        return pickle.loads(data)
    
    def get_extension(self):
        return '.pkl'

class BsonSerializer(ISerializer):
    """BSON-based serializer"""
    def serialize(self, data):
        return bson.dumps(data)
    
    def deserialize(self, data):
        return bson.loads(data)
    
    def get_extension(self):
        return '.bson'

class AsciiSerializer(ISerializer):
    """ASCII-based serializer using readable format"""
    def serialize(self, data):
        # Convert data to a readable ASCII format
        ascii_data = json.dumps(data, indent=2, sort_keys=True)
        return ascii_data.encode('ascii')
    
    def deserialize(self, data):
        # Convert ASCII data back to original format
        ascii_str = data.decode('ascii')
        return json.loads(ascii_str)
    
    def get_extension(self):
        return '.dat'

class SHA256KeyGenerator(IKeyGenerator):
    """Key generator using SHA256"""
    def generate_key(self, data):
        if isinstance(data, dict):
            data_str = json.dumps(data, sort_keys=True)
        else:
            data_str = str(data)
        return hashlib.sha256(data_str.encode()).hexdigest()

class SQLiteConnection:
    """Database connection handler"""
    def __init__(self, db_path, lock):
        self.db_path = db_path
        self.lock = lock
    
    @contextmanager
    def get_connection(self):
        with self.lock:
            conn = sqlite3.connect(self.db_path)
            conn.row_factory = sqlite3.Row
            try:
                yield conn
                conn.commit()
            except Exception as e:
                conn.rollback()
                raise e
            finally:
                conn.close()

class CacheStorage:
    """Storage for cache entries using SQLite"""
    def __init__(self, connection):
        self.connection = connection
        self._init_db()
    
    def _init_db(self):
        with self.connection.get_connection() as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS cache_entries (
                    request_key TEXT PRIMARY KEY,
                    request JSON NOT NULL,
                    result_key TEXT NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    expires_at TIMESTAMP NOT NULL
                )
            """)
            conn.execute("CREATE INDEX IF NOT EXISTS idx_expires_at ON cache_entries(expires_at)")
    
    def get(self, key):
        with self.connection.get_connection() as conn:
            row = conn.execute(
                "SELECT * FROM cache_entries WHERE request_key = ?", 
                (key,)
            ).fetchone()
            
            if row:
                return CacheEntry(
                    request_key=row['request_key'],
                    request=json.loads(row['request']),
                    result_key=row['result_key'],
                    created_at=datetime.fromisoformat(row['created_at']),
                    expires_at=datetime.fromisoformat(row['expires_at'])
                )
        return None
    
    def put(self, entry):
        with self.connection.get_connection() as conn:
            conn.execute("""
                INSERT OR REPLACE INTO cache_entries
                (request_key, request, result_key, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?)
            """, (
                entry.request_key,
                json.dumps(entry.request),
                entry.result_key,
                entry.created_at.isoformat(),
                entry.expires_at.isoformat()
            ))
    
    def delete(self, key):
        with self.connection.get_connection() as conn:
            conn.execute("DELETE FROM cache_entries WHERE request_key = ?", (key,))

class ResultStorage:
    """Storage for result data using files"""
    def __init__(self, results_dir, serializer):
        self.results_dir = results_dir
        self.serializer = serializer
        self.results_dir.mkdir(parents=True, exist_ok=True)
    
    def _get_path(self, key):
        return self.results_dir / f"{key}{self.serializer.get_extension()}"
    
    def get(self, key):
        path = self._get_path(key)
        if path.exists():
            with open(path, 'rb') as f:
                return self.serializer.deserialize(f.read())
        return None
    
    def put(self, key, value):
        path = self._get_path(key)
        with open(path, 'wb') as f:
            f.write(self.serializer.serialize(value))
    
    def delete(self, key):
        path = self._get_path(key)
        if path.exists():
            path.unlink()

class ReferenceCounter:
    """Reference counting manager"""
    def __init__(self, connection):
        self.connection = connection
        self._init_db()
    
    def _init_db(self):
        with self.connection.get_connection() as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS result_references (
                    result_key TEXT PRIMARY KEY,
                    reference_count INTEGER NOT NULL DEFAULT 0
                )
            """)
    
    def increment(self, key):
        with self.connection.get_connection() as conn:
            conn.execute("""
                INSERT INTO result_references (result_key, reference_count)
                VALUES (?, 1)
                ON CONFLICT(result_key) DO UPDATE SET
                reference_count = reference_count + 1
            """, (key,))
    
    def decrement(self, key):
        with self.connection.get_connection() as conn:
            conn.execute("""
                UPDATE result_references
                SET reference_count = reference_count - 1
                WHERE result_key = ?
            """, (key,))
            
            row = conn.execute(
                "SELECT reference_count FROM result_references WHERE result_key = ?",
                (key,)
            ).fetchone()
            
            count = row['reference_count'] if row else 0
            if count <= 0:
                conn.execute("DELETE FROM result_references WHERE result_key = ?", (key,))
            
            return count

class CacheManager:
    """Main cache manager"""
    def __init__(self, config, cache_storage, result_storage, 
                 reference_counter, key_generator, logger):
        self.config = config
        self.cache_storage = cache_storage
        self.result_storage = result_storage
        self.reference_counter = reference_counter
        self.key_generator = key_generator
        self.logger = logger
    
    def get_cached_result(self, request):
        try:
            request_key = self.key_generator.generate_key(request)
            entry = self.cache_storage.get(request_key)
            
            if not entry or entry.expires_at < datetime.now():
                return None
            
            return self.result_storage.get(entry.result_key)
            
        except Exception as e:
            self.logger.error(f"Error retrieving cached result: {str(e)}")
            return None
    
    def cache_request(self, request, result):
        try:
            request_key = self.key_generator.generate_key(request)
            result_key = self.key_generator.generate_key(result)
            
            # Save result
            self.result_storage.put(result_key, result)
            
            # Create cache entry
            entry = CacheEntry(
                request_key=request_key,
                request=request,
                result_key=result_key,
                created_at=datetime.now(),
                expires_at=datetime.now() + self.config.default_expiration
            )
            
            self.cache_storage.put(entry)
            self.reference_counter.increment(result_key)
            
            self._cleanup_cache()
            
        except Exception as e:
            self.logger.error(f"Error caching request: {str(e)}")
            raise

    def _cleanup_cache(self):
        """Clean up expired entries and manage cache size"""
        try:
            # Implementation of cleanup logic
            pass
        except Exception as e:
            self.logger.error(f"Error during cache cleanup: {str(e)}")

class CacheFactory:
    """Factory for creating cache instances"""
    @staticmethod
    def create_cache(config=None):
        if not config:
            config = CacheConfig.create()
        
        # Create basic components
        lock = threading.Lock()
        logger = logging.getLogger("CacheManager")
        
        # Create database connection
        db_connection = SQLiteConnection(config.cache_dir / "cache.db", lock)
        
        # Select serializer based on configuration
        serializers = {
            'pickle': PickleSerializer(),
            'bson': BsonSerializer(),
            'ascii': AsciiSerializer()
        }
        serializer = serializers[config.serialization_method]
        
        # Create components
        cache_storage = CacheStorage(db_connection)
        result_storage = ResultStorage(config.cache_dir / "results", serializer)
        reference_counter = ReferenceCounter(db_connection)
        key_generator = SHA256KeyGenerator()
        
        return CacheManager(
            config=config,
            cache_storage=cache_storage,
            result_storage=result_storage,
            reference_counter=reference_counter,
            key_generator=key_generator,
            logger=logger
        )

def cache_request_decorator(cache):
    """Decorator for automatic request caching"""
    def decorator(func):
        @wraps(func)
        def wrapper(request, *args, **kwargs):
            cached_result = cache.get_cached_result(request)
            if cached_result is not None:
                return cached_result
            
            result = func(request, *args, **kwargs)
            cache.cache_request(request, result)
            return result
        return wrapper
    return decorator
