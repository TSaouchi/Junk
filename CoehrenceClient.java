package junk;

import com.tangosol.net.NamedCache;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;

public class MultiClusterCacheManager {

    private final ConfigurableCacheFactory europeFactory;
    private final ConfigurableCacheFactory usFactory;

    public MultiClusterCacheManager() {
        // Load XML configs into ECCF factories
        ExtensibleConfigurableCacheFactory.Dependencies europeDeps =
            ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("cache-config-europe.xml");
        europeFactory = new ExtensibleConfigurableCacheFactory(europeDeps);

        ExtensibleConfigurableCacheFactory.Dependencies usDeps =
            ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance("cache-config-us.xml");
        usFactory = new ExtensibleConfigurableCacheFactory(usDeps);
    }

    public NamedCache<String, String> getEuropeCache(String cacheName) {
        return europeFactory.ensureCache(cacheName, getClass().getClassLoader());
    }

    public NamedCache<String, String> getUSCache(String cacheName) {
        return usFactory.ensureCache(cacheName, getClass().getClassLoader());
    }

    public void shutdown() {
        europeFactory.dispose();
        usFactory.dispose();
        CacheFactory.shutdown(); // shutdown global Coherence services
    }

    public static void main(String[] args) {
        MultiClusterCacheManager manager = new MultiClusterCacheManager();

        NamedCache<String, String> europeCache = manager.getEuropeCache("toto");
        europeCache.put("euKey", "Hello Europe!");
        System.out.println("Europe: " + europeCache.get("euKey"));

        NamedCache<String, String> usCache = manager.getUSCache("toto");
        usCache.put("usKey", "Hello USA!");
        System.out.println("US: " + usCache.get("usKey"));

        manager.shutdown();
    }
}
