from datetime import datetime
from collections import defaultdict

def filter_old_collections(collection_names, n):
    """
    Filters collection names to keep the most recent `n` days by client, returning the older ones.
    
    Args:
        collection_names (list): List of collection names in the format "YYYYMMDD_clientname".
        n (int): Number of most recent days to keep per client.
    
    Returns:
        dict: A dictionary containing:
            - "recent": A list of the most recent `n` collections per client.
            - "old": A list of all the remaining older collection names.
    """
    # Dictionary to group collections by client
    client_collections = defaultdict(list)
    
    # Parse and group collections
    for name in collection_names:
        try:
            date_str, client_name = name.split("_", 1)  # Split into date and client
            date_obj = datetime.strptime(date_str, "%Y%m%d")  # Parse date in YYYYMMDD format
            client_collections[client_name].append((date_obj, name))  # Store tuple (date, name)
        except ValueError:
            print(f"Invalid collection name format: {name}")
            continue
    
    recent_collections = []
    old_collections = []
    
    # Process collections for each client
    for client, collections in client_collections.items():
        # Sort collections by date (most recent first)
        collections.sort(reverse=True, key=lambda x: x[0])
        
        # Split into recent and old
        recent = [name for _, name in collections[:n]]  # Most recent `n` collections
        old = [name for _, name in collections[n:]]    # Remaining older collections
        
        recent_collections.extend(recent)
        old_collections.extend(old)
    
    return {"recent": recent_collections, "old": old_collections}

# Example usage:
collection_names = [
    "20200210_clientA", "20250209_clientA", "20250208_clientA", "20250211_clientA",
    "20250211_clientB", "20250209_clientB", "20250205_clientB",
    "20250210_clientC", "20250206_clientC", "20250204_clientC"
]

result = filter_old_collections(collection_names, n=0)


print("Recent Collections:", result["recent"])
print("Old Collections:", result["old"])


