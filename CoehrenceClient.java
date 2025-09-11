package junk;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.ConfigurableCacheFactory;
import com.tangosol.net.NamedCache;
import com.tangosol.net.Service;

public class CoherenceClient {

    static {
        System.setProperty("tangosol.pof.enabled", "true");
        System.setProperty("tangosol.coherence.cacheconfig", "cache-config.xml");

        // Shutdown hook with SLF4J logging
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Coherence...");
            CacheFactory.shutdown();
            System.out.println("Coherence shutdown complete.");
        }));
    }

    public enum ClusterTarget {
        US("RemoteCacheUS"),
        EU("RemoteCacheEU");

        private final String serviceName;

        ClusterTarget(String serviceName) {
            this.serviceName = serviceName;
        }

        public String getServiceName() {
            return serviceName;
        }
    }

    public static <K, V> NamedCache<K, V> getCache(ClusterTarget target, String cacheName) {
        ConfigurableCacheFactory ccf = CacheFactory.getConfigurableCacheFactory();
        Service service = ccf.ensureService(target.getServiceName());
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return ccf.ensureCache(cacheName, loader, service);
    }

    public static void main(String[] args) {
        NamedCache<String, String> usOrders = getCache(ClusterTarget.US, "orders");
        usOrders.put("id-1", "US order");

        NamedCache<String, String> euOrders = getCache(ClusterTarget.EU, "orders");
        euOrders.put("id-1", "EU order");

        log.info("Inserted 'orders' cache in both clusters.");
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
