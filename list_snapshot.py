import numpy as np
from pymongo import MongoClient
from bson import Binary
import hashlib
from datetime import datetime

# ---- MongoDB Setup ----
client = MongoClient("mongodb://localhost:27017/")
db = client["your_database"]
snapshots = db["snapshots"]
history = db["history"]

# ---- Config ----
CHUNK_SIZE = 100
DTYPE = np.float32


def chunk_and_flatten(data):
    """Split list into chunks and return flattened array + split indices"""
    chunks = [data[i:i + CHUNK_SIZE] for i in range(0, len(data), CHUNK_SIZE)]
    split_lengths = [len(chunk) for chunk in chunks]
    split_indices = np.cumsum(split_lengths).tolist()
    flat = np.array(data, dtype=DTYPE)
    return flat, split_indices


def compute_hash(array: np.ndarray) -> str:
    """Compute SHA256 hash of binary representation"""
    return hashlib.sha256(array.tobytes()).hexdigest()


def load_previous_snapshot():
    """Load latest snapshot by date (excluding 'history')"""
    prev = snapshots.find_one(sort=[("date", -1)], filter={"_id": {"$ne": "history"}})
    if prev:
        return np.frombuffer(prev["chunks_bin"], dtype=prev["dtype"]).tolist(), prev["_id"]
    return [], None


def compute_diff(old_data, new_data):
    """Return added and removed elements"""
    old_set, new_set = set(old_data), set(new_data)
    return list(new_set - old_set), list(old_set - new_set)


def main(data):
    today = datetime.now().strftime("%Y-%m-%d")
    flat, split_indices = chunk_and_flatten(data)
    hash_val = compute_hash(flat)

    # Check if previous snapshot exists
    old_data, last_snapshot_date = load_previous_snapshot()

    if compute_hash(np.array(old_data, dtype=DTYPE)) == hash_val:
        print(f"No changes detected on {today}. Skipping insertion.")
        return

    # Store today's snapshot
    snapshot_doc = {
        "_id": today,
        "date": today,
        "chunks_bin": Binary(flat.tobytes()),
        "split_indices": split_indices,
        "dtype": str(DTYPE.__name__),
        "hash": hash_val,
        "total_elements": len(flat)
    }
    snapshots.replace_one({"_id": today}, snapshot_doc, upsert=True)

    # Compute diff & update history
    added, removed = compute_diff(old_data, data)

    history.update_one(
        {"_id": "history"},
        {
            "$push": {
                "diffs": {
                    "date": today,
                    "prev_date": last_snapshot_date,
                    "added": added,
                    "removed": removed,
                    "n_added": len(added),
                    "n_removed": len(removed)
                }
            }
        },
        upsert=True
    )

    print(f"Snapshot for {today} stored with {len(flat)} elements.")
    print(f"Diff recorded: +{len(added)}, -{len(removed)}")


# === Example use ===
if __name__ == "__main__":
    # Simulate API data: list of floats
    new_data = list(np.random.rand(100_231).astype(np.float32))
    main(new_data)
