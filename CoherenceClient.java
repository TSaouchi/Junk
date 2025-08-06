<cache-config xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd">

    <caching-scheme-mapping>
        <cache-mapping>
            <cache-name>*</cache-name> <!-- Match all cache names -->
            <scheme-name>remote</scheme-name> <!-- Use the remote scheme -->
        </cache-mapping>
    </caching-scheme-mapping>

    <caching-schemes>
        <remote-cache-scheme>
            <scheme-name>remote</scheme-name>
            <service-name>RemoteCacheService</service-name> <!-- Must match server-side service -->
            <initiator-config>
                <tcp-initiator>
                    <remote-addresses>
                        <socket-address>
                            <address>192.168.1.100</address> <!-- Remote cluster IP -->
                            <port>7574</port> <!-- Remote cluster port -->
                        </socket-address>
                    </remote-addresses>
                    <connect-timeout>5s</connect-timeout> <!-- Fail fast -->
                </tcp-initiator>
                <outgoing-message-handler>
                    <request-timeout>10s</request-timeout>
                </outgoing-message-handler>
            </initiator-config>
        </remote-cache-scheme>
    </caching-schemes>
</cache-config>


import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class CoherenceClient {

    public static void main(String[] args) {
        // Discovery mode: disable multicast
        System.setProperty("tangosol.coherence.discovery.mode", "wka");

        // WKA target
        System.setProperty("tangosol.coherence.wka", "192.168.1.100");
        System.setProperty("tangosol.coherence.wka.port", "7574");

        // Use custom client-only config
        System.setProperty("tangosol.coherence.cacheconfig", "client-cache-config.xml");

        try {
            NamedCache<String, String> cache = CacheFactory.getCache("test-cache");
            String value = cache.get("hello");
            System.out.println("Fetched from remote cache: " + value);
            System.out.println("Remote cache size: " + cache.size());
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to remote Coherence cluster.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class CoherenceClient {

    public static void main(String[] args) {
        // Discovery mode: disable multicast
        System.setProperty("tangosol.coherence.discovery.mode", "wka");

        // WKA target
        System.setProperty("tangosol.coherence.wka", "192.168.1.100");
        System.setProperty("tangosol.coherence.wka.port", "7574");

        // Use custom client-only config
        System.setProperty("tangosol.coherence.cacheconfig", "client-cache-config.xml");

        try {
            NamedCache<String, String> cache = CacheFactory.getCache("test-cache");
            String value = cache.get("hello");
            System.out.println("Fetched from remote cache: " + value);
            System.out.println("Remote cache size: " + cache.size());
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to remote Coherence cluster.");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
