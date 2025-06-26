import com.tangosol.net.CacheFactory;
import com.tangosol.net.Cluster;
import com.tangosol.net.NamedCache;

import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class StrictCoherenceClient {

    private final Map<String, NamedCache<Object, Object>> caches = new ConcurrentHashMap<>();
    private boolean connected = false;

    static {
        System.setProperty("tangosol.coherence.cacheconfig", "coherence-client.xml");
        System.setProperty("tangosol.coherence.cluster", "ClientCluster");
        // System.setProperty("tangosol.coherence.log.level", "9"); // For debug
    }

    public void connect() {
        if (connected) return;

        Cluster cluster = CacheFactory.ensureCluster();
        int members = cluster.getMemberSet().size();

        if (members <= 1) {
            throw new IllegalStateException("❌ Not connected to remote Coherence cluster.");
        }

        System.out.println("✅ Connected to remote cluster: " + cluster.getClusterName());
        connected = true;
    }

    // === Strict getCache: ensure it exists remotely ===
    private NamedCache<Object, Object> getExistingCache(String cacheName) {
        if (!connected) {
            throw new IllegalStateException("🚫 Must call connect() first.");
        }

        // Try to get the cache (this may succeed even if cache is not defined remotely yet)
        NamedCache<Object, Object> cache = CacheFactory.getCache(cacheName);

        // Validate: remote cache must have a backing map
        try {
            // This will trigger communication with the server
            cache.size(); 
        } catch (Exception e) {
            throw new RuntimeException("❌ Cache '" + cacheName + "' does not exist remotely or is not reachable.", e);
        }

        return cache;
    }

    public void fetchAllFromCache(String cacheName) {
        NamedCache<Object, Object> cache = getExistingCache(cacheName);
        Set<Object> keys = cache.keySet();
        System.out.println("📦 Contents of '" + cacheName + "':");
        for (Object key : keys) {
            Object value = cache.get(key);
            System.out.printf(" - [%s] → %s%n", key, value);
        }
    }

    public Object fetchByKey(String cacheName, Object key) {
        NamedCache<Object, Object> cache = getExistingCache(cacheName);
        Object value = cache.get(key);
        System.out.printf("🔍 [%s] from '%s' → %s%n", key, cacheName, value);
        return value;
    }

    public void shutdown() {
        CacheFactory.shutdown();
        connected = false;
        caches.clear();
        System.out.println("🔒 Coherence client shut down.");
    }

    public static void main(String[] args) {
        StrictCoherenceClient client = new StrictCoherenceClient();
        try {
            client.connect();
            client.fetchAllFromCache("toto");           // Must exist
            client.fetchByKey("users", "user-123");     // Must exist

        } catch (Exception e) {
            System.err.println("🚨 Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            client.shutdown();
        }
    }
}
