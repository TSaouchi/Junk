python -c "from pymongo import MongoClient; import sys; client=MongoClient('<your_mongo_uri>'); print('Connected:', bool(client.admin.command('ping')))"
