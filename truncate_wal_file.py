import sqlite3
from diskcache import Cache

cache_dir = '/path/to/your/cache'
db_path = f'{cache_dir}/cache.db'
conn = sqlite3.connect(db_path)

# geet data to .db and shrink .wal
conn.execute('PRAGMA wal_checkpoint(TRUNCATE);')
# just close connection and not the db
conn.close()
