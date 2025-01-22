package com.yourcompany;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class CoherenceExample {
    public static void main(String[] args) {
        try {
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
