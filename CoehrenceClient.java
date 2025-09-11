package junk;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CoherenceClient {

    private static final ConfigurableCacheFactory usFactory;
    private static final ConfigurableCacheFactory euFactory;

    static {
        System.setProperty("tangosol.pof.enabled", "true");

        // Create separate factories for US and EU
        usFactory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("cache-config-us.xml", null);

        euFactory = CacheFactory.getCacheFactoryBuilder()
                .getConfigurableCacheFactory("cache-config-eu.xml", null);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Coherence factories...");
            try {
                if (usFactory != null) usFactory.shutdown();
                if (euFactory != null) euFactory.shutdown();
            } catch (Exception e) {
                log.error("Error during shutdown", e);
            }
            log.info("Coherence shutdown complete.");
        }));
    }

    public enum ClusterTarget {
        US, EU
    }

    @SuppressWarnings("unchecked")
    public static <K, V> NamedCache<K, V> getCache(ClusterTarget target, String cacheName) {
        ConfigurableCacheFactory ccf = (target == ClusterTarget.US) ? usFactory : euFactory;
        ClassLoader loader;
        try {
            loader = Thread.currentThread().getContextClassLoader();
        } catch (SecurityException e) {
            loader = null;
        }
        return ccf.ensureCache(cacheName, loader);
    }

    public static void main(String[] args) {
        NamedCache<String, String> usOrders = getCache(ClusterTarget.US, "orders");
        usOrders.put("id-1", "US order");

        NamedCache<String, String> euOrders = getCache(ClusterTarget.EU, "orders");
        euOrders.put("id-1", "EU order");

        log.info("Inserted 'orders' cache in both clusters using the same cache name.");
    }
}



<caching-scheme-mapping>
    <!-- All caches can be mapped to US -->
    <cache-mapping>
        <cache-name>*</cache-name>
        <scheme-name>us-remote</scheme-name>
    </cache-mapping>

    <!-- All caches can be mapped to EU -->
    <cache-mapping>
        <cache-name>*</cache-name>
        <scheme-name>eu-remote</scheme-name>
    </cache-mapping>
</caching-scheme-mapping>

<caching-schemes>
    <remote-cache-scheme>
        <scheme-name>us-remote</scheme-name>
        <service-name>RemoteCacheUS</service-name>
        <initiator-config>
            <tcp-initiator>
                <remote-addresses>
                    <socket-address>
                        <address>us-server-host</address>
                        <port>9090</port>
                    </socket-address>
                </remote-addresses>
            </tcp-initiator>
        </initiator-config>
    </remote-cache-scheme>

    <remote-cache-scheme>
        <scheme-name>eu-remote</scheme-name>
        <service-name>RemoteCacheEU</service-name>
        <initiator-config>
            <tcp-initiator>
                <remote-addresses>
                    <socket-address>
                        <address>eu-server-host</address>
                        <port>9090</port>
                    </socket-address>
                </remote-addresses>
            </tcp-initiator>
        </initiator-config>
    </remote-cache-scheme>
</caching-schemes>
