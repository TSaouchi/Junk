mvn archetype:generate \
    -DgroupId=com.bnpp \
    -DartifactId=my-coherence-client \
    -DarchetypeArtifactId=maven-archetype-quickstart \
    -DinteractiveMode=false


---------------------------------------
    <project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.bnpp</groupId>
    <artifactId>my-coherence-client</artifactId>
    <version>1.0-SNAPSHOT</version>

    <dependencies>
        <!-- Oracle Coherence dependency -->
        <dependency>
            <groupId>com.oracle.coherence.ce</groupId>
            <artifactId>coherence</artifactId>
            <version>22.06.8</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <!-- Maven Compiler Plugin to set Java version -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
----------------------------------
    package com.bnpp;

import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;

public class CoherenceClient {
    public static void main(String[] args) {
        // Set the system property to specify the client cache configuration
        System.setProperty("tangosol.coherence.cacheconfig", "client-cache-config.xml");

        // Obtain the cache named 'toto'
        NamedCache<Object, Object> cache = CacheFactory.getCache("toto");

        // Print the contents of the cache
        System.out.println("Contents of cache 'toto':");
        cache.entrySet().forEach(entry ->
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue())
        );

        // Shutdown the CacheFactory
        CacheFactory.shutdown();
    }
}
---------------------------------------
    <?xml version="1.0"?>
<cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config coherence-cache-config.xsd">
    <caching-scheme-mapping>
        <cache-mapping>
            <cache-name>*</cache-name>
            <scheme-name>remote-cache</scheme-name>
        </cache-mapping>
    </caching-scheme-mapping>

    <caching-schemes>
        <remote-cache-scheme>
            <scheme-name>remote-cache</scheme-name>
            <service-name>RemoteCacheService</service-name>
            <initiator-config>
                <tcp-initiator>
                    <remote-addresses>
                        <socket-address>
                            <address>192.168.1.100</address> <!-- Replace with your cluster's IP address -->
                            <port>7574</port> <!-- Replace with your cluster's port -->
                        </socket-address>
                    </remote-addresses>
                </tcp-initiator>
            </initiator-config>
        </remote-cache-scheme>
    </caching-schemes>
</cache-config>

----------------------------------
mvn compile
mvn exec:java -Dexec.mainClass="com.bnpp.CoherenceClient"
    
