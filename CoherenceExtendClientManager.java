import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.ExtensibleConfigurableCacheFactory;
import com.tangosol.net.NamedCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CoherenceExtendClientManager {

    private final Map<String, ConfigurableCacheFactory> factories;

    private CoherenceExtendClientManager(Map<String, ConfigurableCacheFactory> factories) {
        this.factories = factories;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Coherence...");
            shutdown();
            System.out.println("Coherence shutdown complete.");
        }));
    }

    @SuppressWarnings("unchecked")
    public <K, V> NamedCache<K, V> getCache(String cacheName, String region) {
        Objects.requireNonNull(cacheName, "cacheName must not be null");
        Objects.requireNonNull(region, "region must not be null");

        ClassLoader loader;
        ConfigurableCacheFactory factory = factories.get(region);
        if (factory == null) {
            throw new IllegalStateException("No factory configured for region: " + region);
        }
        try {
            loader = Thread.currentThread().getContextClassLoader();
        } catch (SecurityException e) {
            loader = null;
        } catch (SecurityException e) {
            loader = getClass().getClassLoader();
        }
        return (NamedCache<K, V>) factory.ensureCache(cacheName, loader);
    }

    public void shutdown() {
        factories.values().forEach(ConfigurableCacheFactory::dispose);
        factories.clear();
        CacheFactory.shutdown();
    }

    public static class Builder {
        private final Map<String, ConfigurableCacheFactory> factories = new HashMap<>();
        public Builder withRegion(String region, String configFile) {
            Objects.requireNonNull(region, "region must not be null");
            Objects.requireNonNull(configFile, "configFile must not be null");

            ExtensibleConfigurableCacheFactory.Dependencies dependencies =
                ExtensibleConfigurableCacheFactory.DependenciesHelper.newInstance(configFile);
            ConfigurableCacheFactory factory = new ExtensibleConfigurableCacheFactory(dependencies);

            factories.put(region, factory);
            return this;
        }

        public CoherenceExtendClientManager build() {
            return new CoherenceExtendClientManager(new HashMap<>(factories));
        }
    }

    // ----------- Example usage -------------
    public static void main(String[] args) {
        CoherenceExtendClientManager manager = new CoherenceExtendClientManager.Builder()
            .withRegion("EUROPE", "cache-config-europe.xml")
            .withRegion("US", "cache-config-us.xml")
            .build();

        // Europe cache
        NamedCache<String, String> euCache = manager.getCache("toto", "EUROPE");
        euCache.put("euKey", "Hello Europe!");
        System.out.println("Europe: " + euCache.get("euKey"));

        // US cache
        NamedCache<String, String> usCache = manager.getCache("toto", "US");
        usCache.put("usKey", "Hello USA!");
        System.out.println("US: " + usCache.get("usKey"));

        // No explicit shutdown needed, JVM hook handles it
    }
}
