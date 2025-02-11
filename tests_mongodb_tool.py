import unittest
from unittest.mock import patch, MagicMock
from pymongo.errors import PyMongoError, BulkWriteError
from pymongo import InsertOne
from datetime import datetime, timedelta
from mongo_code import MongoAuth, MongoConnectionPool, MongoQueryExecutor, MongoDataInserter  # Replace with your module name

class TestMongoAuth(unittest.TestCase):
    def test_singleton(self):
        auth1 = MongoAuth(username="user", password="pass")
        auth2 = MongoAuth()
        self.assertIs(auth1, auth2)

    def test_reset_instance(self):
        MongoAuth.reset_instance()
        auth1 = MongoAuth(username="user", password="pass")
        MongoAuth.reset_instance()
        auth2 = MongoAuth()
        self.assertIsNot(auth1, auth2)

    def test_get_connection_uri_with_credentials(self):
        auth = MongoAuth(username="user", password="pass", host="host", port=1234, auth_db="authdb")
        expected_uri = "mongodb://user:pass@host:1234/?authSource=authdb"
        self.assertEqual(auth.get_connection_uri(), expected_uri)

    def test_get_connection_uri_without_credentials(self):
        auth = MongoAuth(host="host", port=1234)
        expected_uri = "mongodb://host:1234/"
        self.assertEqual(auth.get_connection_uri(), expected_uri)


class TestMongoConnectionPool(unittest.TestCase):
    @patch("mongo_code.MongoClient")  # Replace with your module name
    def test_initialization(self, mock_client):
        auth = MongoAuth(username="user", password="pass")
        pool = MongoConnectionPool(auth, maxPoolSize=10)
        mock_client.assert_called_once()

    @patch("mongo_code.MongoClient")  # Replace with your module name
    def test_get_database(self, mock_client):
        auth = MongoAuth(username="user", password="pass")
        pool = MongoConnectionPool(auth)
        mock_db = MagicMock()
        mock_client.return_value.__getitem__.return_value = mock_db
        self.assertEqual(pool.get_database("test_db"), mock_db)


class TestMongoQueryExecutor(unittest.TestCase):
    @patch("mongo_code.MongoClient")  # Replace with your module name
    def test_find(self, mock_client):
        auth = MongoAuth(username="user", password="pass")
        pool = MongoConnectionPool(auth)
        executor = MongoQueryExecutor(pool, "test_db")
        mock_collection = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.sort.return_value = mock_cursor
        mock_cursor.skip.return_value = mock_cursor
        mock_cursor.limit.return_value = mock_cursor
        mock_cursor.batch_size.return_value = mock_cursor
        mock_cursor.__iter__.return_value = [{"_id": 1}, {"_id": 2}]
        mock_client.return_value.__getitem__.return_value.__getitem__.return_value = mock_collection
        mock_collection.find.return_value = mock_cursor
        result = executor.find("test_collection", {"key": "value"}, limit=10, sort=[("_id", 1)])
        self.assertEqual(result, [{"_id": 1}, {"_id": 2}])


class TestMongoDataInserter(unittest.TestCase):
    @patch("mongo_code.MongoClient")  # Replace with your module name
    def test_insert_one(self, mock_client):
        auth = MongoAuth(username="user", password="pass")
        pool = MongoConnectionPool(auth)
        inserter = MongoDataInserter(pool, "test_db")
        mock_collection = MagicMock()
        mock_client.return_value.__getitem__.return_value.__getitem__.return_value = mock_collection
        mock_result = MagicMock(inserted_id="mock_id")
        mock_collection.insert_one.return_value = mock_result
        result = inserter.insert_one("test_collection", {"key": "value"}, ttl=3600)
        self.assertEqual(result, "mock_id")

    @patch("mongo_code.MongoClient")  # Replace with your module name
    def test_insert_many(self, mock_client):
        auth = MongoAuth(username="user", password="pass")
        pool = MongoConnectionPool(auth)
        inserter = MongoDataInserter(pool, "test_db")
        mock_collection = MagicMock()
        mock_client.return_value.__getitem__.return_value.__getitem__.return_value = mock_collection
        mock_result = MagicMock(inserted_ids=["id1", "id2"])
        mock_collection.insert_many.return_value = mock_result
        result = inserter.insert_many("test_collection", [{"key": "value1"}, {"key": "value2"}], ttl=3600)
        self.assertEqual(result, ["id1", "id2"])

    @patch("mongo_code.MongoClient")  # Replace with your module name
    def test_bulk_insert(self, mock_client):
        auth = MongoAuth(username="user", password="pass")
        pool = MongoConnectionPool(auth)
        inserter = MongoDataInserter(pool, "test_db")
        mock_collection = MagicMock()
        mock_client.return_value.__getitem__.return_value.__getitem__.return_value = mock_collection
        mock_result = MagicMock(inserted_count=2)
        mock_collection.bulk_write.return_value = mock_result
        result = inserter.bulk_insert("test_collection", [{"key": "value1"}, {"key": "value2"}])
        self.assertEqual(result, 2)

    @patch("mongo_code.MongoClient")  # Replace with your module name
    def test_create_ttl_index(self, mock_client):
        auth = MongoAuth(username="user", password="pass")
        pool = MongoConnectionPool(auth)
        inserter = MongoDataInserter(pool, "test_db")
        mock_collection = MagicMock()
        mock_client.return_value.__getitem__.return_value.__getitem__.return_value = mock_collection
        inserter.create_ttl_index("test_collection", "expires_at", 3600)
        mock_collection.create_index.assert_called_once_with("expires_at", expireAfterSeconds=3600)

    @patch("mongo_code.MongoClient")  # Replace with your module name
    def test_ensure_index(self, mock_client):
        auth = MongoAuth(username="user", password="pass")
        pool = MongoConnectionPool(auth)
        inserter = MongoDataInserter(pool, "test_db")
        mock_collection = MagicMock()
        mock_client.return_value.__getitem__.return_value.__getitem__.return_value = mock_collection
        inserter.ensure_index("test_collection", [("key", 1)], "test_index")
        mock_collection.create_index.assert_called_once_with([("key", 1)], name="test_index")

if __name__ == "__main__":
    unittest.main()
