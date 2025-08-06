--------------------------- Server side ----------------------
<?xml version="1.0" encoding="UTF-8"?>
<cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd">

    <caching-scheme-mapping>
        <cache-mapping>
            <cache-name>test-cache</cache-name>
            <scheme-name>extend-scheme</scheme-name>
        </cache-mapping>
    </caching-scheme-mapping>

    <caching-schemes>
        <extend-binary-scheme>
            <scheme-name>extend-scheme</scheme-name>
            <service-name>ExtendTcpCacheService</service-name>
            <init-params>
                <param>
                    <name>port</name>
                    <value>1408</value>
                </param>
            </init-params>
        </extend-binary-scheme>
    </caching-schemes>

</cache-config>
------------------------------------------ client side --------------------------
  <?xml version="1.0" encoding="UTF-8"?>
<cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd">

    <caching-scheme-mapping>
        <cache-mapping>
            <cache-name>test-cache</cache-name>
            <scheme-name>extend-scheme</scheme-name>
        </cache-mapping>
    </caching-scheme-mapping>

    <caching-schemes>
        <extend-binary-scheme>
            <scheme-name>extend-scheme</scheme-name>
            <service-name>ExtendTcpCacheService</service-name>
            <initiator-config>
                <tcp-initiator>
                    <remote-addresses>
                        <socket-address>
                            <address>192.168.1.100</address>  <!-- Replace with your cluster's proxy IP -->
                            <port>1408</port>                 <!-- Port must match server-side -->
                        </socket-address>
                    </remote-addresses>
                </tcp-initiator>
            </initiator-config>
        </extend-binary-scheme>
    </caching-schemes>

</cache-config>


  import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class ExtendClient {
    public static void main(String[] args) {
        // Tell Coherence to use the client-side config with Extend settings
        System.setProperty("tangosol.coherence.cacheconfig", "client-cache-config.xml");

        try {
            // Obtain remote cache reference via Extend
            NamedCache<String, String> cache = CacheFactory.getCache("test-cache");

            // Put data into cache
            cache.put("hello", "world");

            // Retrieve and print data
            System.out.println("Cached Value for 'hello': " + cache.get("hello"));

            // Print cache size (remote)
            System.out.println("Cache size: " + cache.size());

            // Properly release cache resources
            cache.release();
        } catch (Exception e) {
            System.err.println("Failed to connect or operate on cache:");
            e.printStackTrace();
            System.exit(1); // Crash as you requested if connection fails
        } finally {
            // Ensure cache factory is shut down cleanly
            CacheFactory.shutdown();
        }
    }
}

  
