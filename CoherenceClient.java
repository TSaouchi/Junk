--------------------------- Server side ----------------------
<?xml version="1.0" encoding="UTF-8"?>
<cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd">

    <!-- Map the cache name to a distributed scheme -->
    <caching-scheme-mapping>
        <cache-mapping>
            <cache-name>test-cache</cache-name>
            <scheme-name>distributed-scheme</scheme-name>
        </cache-mapping>
    </caching-scheme-mapping>

    <caching-schemes>

        <!-- Distributed caching scheme -->
        <distributed-scheme>
            <scheme-name>distributed-scheme</scheme-name>
            <service-name>DistributedCache</service-name>
            <backing-map-scheme>
                <local-scheme/>
            </backing-map-scheme>
            <autostart>true</autostart>
        </distributed-scheme>

        <!-- Extend proxy so clients can connect -->
        <proxy-scheme>
            <scheme-name>extend-proxy</scheme-name>
            <service-name>ExtendTcpProxyService</service-name>
            <acceptor-config>
                <tcp-acceptor>
                    <local-address>
                        <address>0.0.0.0</address>  <!-- Bind to all interfaces -->
                        <port>9005</port>           <!-- Client will connect here -->
                    </local-address>
                </tcp-acceptor>
            </acceptor-config>
            <autostart>true</autostart>
        </proxy-scheme>

    </caching-schemes>
</cache-config>

<?xml version="1.0" encoding="UTF-8"?>
<management-config
    xmlns="http://xmlns.oracle.com/coherence/coherence-management-config"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-management-config coherence-management-config.xsd">

  <!-- 1) Enable HTTP-management on ALL storage nodes -->
  <http-server>
    <cluster-config name="${coherence.cluster}">
      <http-managed-nodes>all</http-managed-nodes>
    </cluster-config>

    <!-- 2) Bind the REST-API port -->
    <rest-proxy>
      <http-server>
        <local-address>
          <address>0.0.0.0</address>
        </local-address>
        <port>${coherence.rest.port}</port>
      </http-server>
    </rest-proxy>
  </http-server>
</management-config>

------------------------------------------ client side --------------------------
<?xml version="1.0" encoding="UTF-8"?>
<cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd">

    <!-- Map to use the remote Extend proxy -->
    <caching-scheme-mapping>
        <cache-mapping>
            <cache-name>test-cache</cache-name>
            <scheme-name>extend-scheme</scheme-name>
        </cache-mapping>
    </caching-scheme-mapping>

    <caching-schemes>
        <remote-cache-scheme>
            <scheme-name>extend-scheme</scheme-name>
            <service-name>ExtendTcpCacheService</service-name>
            <initiator-config>
                <tcp-initiator>
                    <remote-addresses>
                        <socket-address>
                            <address>192.168.1.100</address> <!-- IP of your Coherence server -->
                            <port>9005</port>                <!-- Port used by Extend proxy -->
                        </socket-address>
                    </remote-addresses>
                    <connect-timeout>5s</connect-timeout>   <!-- Optional: fail fast -->
                </tcp-initiator>
            </initiator-config>
        </remote-cache-scheme>
    </caching-schemes>
</cache-config>


import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class ExtendClient {
    public static void main(String[] args) {
        System.setProperty("tangosol.coherence.cacheconfig", "client-cache-config.xml");
        System.setProperty("tangosol.coherence.cluster", "remote-cluster");

        try {
            NamedCache<String, String> cache = CacheFactory.getCache("test-cache");

            cache.put("hello", "world");
            String value = cache.get("hello");

            System.out.println("Fetched from remote cache: " + value);
            System.out.println("Cache size: " + cache.size());

            cache.release();
        } catch (Exception e) {
            System.err.println("❌ Failed to connect to remote Coherence cluster:");
            e.printStackTrace();
            System.exit(1);
        } finally {
            CacheFactory.shutdown();
        }
    }
}

  
