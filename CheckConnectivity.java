package junk;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class CheckConnectivity {

    public static void main(String[] args) {
        // Prevent local storage, connect only as a client
        System.setProperty("tangosol.coherence.distributed.localstorage", "false");

        // Set cluster configuration
        System.setProperty("tangosol.coherence.cluster", "MyCluster");
        System.setProperty("tangosol.coherence.clusterport", "9000");
        System.setProperty("tangosol.coherence.wka", "1s9600dpc01082.xmp.net.intra,1s9500dpc01225.xmp.net.intra");

        try {
            // Connect to named cache
            NamedCache<String, String> cache = CacheFactory.getCache("myCache");

            System.out.println("✅ Connected to cache 'myCache'");

            // Print size before
            int sizeBefore = cache.size();
            System.out.println("📦 Cache size before put: " + sizeBefore);

            // Put some data
            cache.put("hello", "world");
            cache.put("user", "admin");

            // Print size after
            int sizeAfter = cache.size();
            System.out.println("📦 Cache size after put: " + sizeAfter);

            // Show one value
            System.out.println("🔍 Cache entry [hello] = " + cache.get("hello"));

        } catch (Exception e) {
            System.err.println("❌ Connection or cache access failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
