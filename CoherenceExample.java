package com.yourcompany;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.InvocationService;
import com.tangosol.net.QuietCacheFactory;
import com.tangosol.net.Service;
import com.tangosol.net.DefaultConfigurableCacheFactory;
import com.tangosol.net.Coherence;

public class CoherenceExample {
    public static void main(String[] args) {
        try {
            // Specify the Coherence cluster address (IP and port of the Coherence server)
            String clusterAddress = "your_ip_address:your_port_number";  // Replace with actual IP and port

            // Set up the CacheFactory to connect to the Coherence cluster using the specified address
            System.setProperty("tangosol.coherence.cluster", clusterAddress);

            // Start the Coherence cache (this assumes a Coherence cluster is running)
            String cacheName = "your_cache_name"; // Replace with your cache name
            NamedCache cache = CacheFactory.getCache(cacheName);

            // Print all cache entries
            System.out.println("Cache content:");
            for (Object key : cache.keySet()) {
                System.out.println("Key: " + key + ", Value: " + cache.get(key));
            }
        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
        } finally {
            // Shutdown the Coherence cache factory
            CacheFactory.shutdown();
        }
    }
}
