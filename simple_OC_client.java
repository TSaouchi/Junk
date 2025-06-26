import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class CoherenceCacheSize {
    public static void main(String[] args) {
        try {
            // Point to your coherence-client.xml
            System.setProperty("tangosol.coherence.cacheconfig", "coherence-client.xml");

            // Optionally disable local clustering if only acting as client
            System.setProperty("tangosol.coherence.cluster", "ClientCluster");

            // Start the cluster connection
            CacheFactory.ensureCluster();

            // Connect to cache named "toto"
            NamedCache<?, ?> cache = CacheFactory.getCache("toto");

            // Print size
            System.out.println("Cache 'toto' size: " + cache.size());

            // Shutdown
            CacheFactory.shutdown();
        } catch (Exception e) {
            System.err.println("Error connecting to Coherence: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
