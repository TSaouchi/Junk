<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE coherence SYSTEM "coherence.dtd">

<coherence>
  <!-- Cluster Configuration -->
  <cluster-config>
    <member-identity>
      <cluster-name system-property="coherence.cluster">MyCoherenceCluster</cluster-name>
      <role system-property="coherence.role">storage</role>
      <machine-name system-property="coherence.machine">server1</machine-name>
    </member-identity>
    
    <!-- Network Configuration -->
    <multicast-listener>
      <address system-property="coherence.clusteraddress">224.3.7.0</address>
      <port system-property="coherence.clusterport">36000</port>
      <time-to-live system-property="coherence.ttl">4</time-to-live>
      <join-timeout-milliseconds>30000</join-timeout-milliseconds>
    </multicast-listener>
    
    <!-- Unicast listener for Well Known Address (WKA) configuration -->
    <unicast-listener>
      <well-known-addresses>
        <socket-address>
          <address system-property="coherence.localhost">127.0.0.1</address>
          <port system-property="coherence.localport">8088</port>
        </socket-address>
        <socket-address>
          <address>127.0.0.1</address>
          <port>8089</port>
        </socket-address>
        <socket-address>
          <address>127.0.0.1</address>
          <port>8090</port>
        </socket-address>
      </well-known-addresses>
      <port-auto-adjust>true</port-auto-adjust>
      <enabled system-property="coherence.wka">false</enabled>
    </unicast-listener>
    
    <!-- TCP Ring configuration -->
    <tcp-ring-listener>
      <ip-timeout>15s</ip-timeout>
      <ip-monitor>
        <address-provider>multicast</address-provider>
      </ip-monitor>
    </tcp-ring-listener>
    
    <!-- Packet delivery configuration -->
    <packet-speaker>
      <enabled>true</enabled>
      <priority>5</priority>
      <volume-threshold>0.25</volume-threshold>
    </packet-speaker>
    
    <packet-publisher>
      <packet-delivery>
        <timeout-milliseconds>60000</timeout-milliseconds>
        <heartbeat-milliseconds>5000</heartbeat-milliseconds>
        <flow-control>
          <enabled>true</enabled>
          <outstanding-packets>256</outstanding-packets>
          <pause-milliseconds>100</pause-milliseconds>
        </flow-control>
        <packet-bundling>
          <maximum-deferral-time>1ms</maximum-deferral-time>
          <aggression-factor>0</aggression-factor>
        </packet-bundling>
      </packet-delivery>
      
      <notification-queueing>
        <ack-delay-milliseconds>16</ack-delay-milliseconds>
        <nack-delay-milliseconds>1</nack-delay-milliseconds>
      </notification-queueing>
      
      <traffic-jam>
        <maximum-packets>8192</maximum-packets>
        <pause-milliseconds>10</pause-milliseconds>
      </traffic-jam>
    </packet-publisher>
    
    <!-- Shutdown Configuration -->
    <shutdown-listener>
      <enabled>true</enabled>
    </shutdown-listener>
  </cluster-config>

  <!-- Logging Configuration -->
  <logging-config>
    <destination system-property="coherence.log">stderr</destination>
    <severity-level system-property="coherence.log.level">5</severity-level>
    <message-format>{date} {product} {version} &lt;{level}&gt; (thread={thread}, member={member}): {text}</message-format>
    <character-limit>8192</character-limit>
    <logger-name>Coherence</logger-name>
  </logging-config>

  <!-- Cache Factory Configuration -->
  <configurable-cache-factory-config>
    <class-name>com.tangosol.net.DefaultConfigurableCacheFactory</class-name>
    <init-params>
      <init-param>
        <param-type>java.lang.String</param-type>
        <param-value system-property="coherence.cacheconfig">coherence-cache-config.xml</param-value>
      </init-param>
    </init-params>
  </configurable-cache-factory-config>

  <!-- Serialization Configuration -->
  <serializers>
    <serializer id="pof">
      <class-name>com.tangosol.io.pof.ConfigurablePofContext</class-name>
      <init-params>
        <init-param>
          <param-type>java.lang.String</param-type>
          <param-value system-property="coherence.pof.config">pof-config.xml</param-value>
        </init-param>
      </init-params>
    </serializer>
  </serializers>

  <!-- Management Configuration -->
  <management-config>
    <managed-nodes system-property="coherence.management">all</managed-nodes>
    <allow-remote-management system-property="coherence.management.remote">true</allow-remote-management>
    <default-domain-name>coherence.cluster</default-domain-name>
    <service-name>Management</service-name>
    <refresh-policy>refresh-ahead</refresh-policy>
  </management-config>

  <!-- Security Configuration -->
  <security-config>
    <enabled system-property="coherence.security">false</enabled>
  </security-config>

  <!-- License Configuration -->
  <license-config>
    <edition-name system-property="coherence.edition">CE</edition-name>
  </license-config>
</coherence>




<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE cache-config SYSTEM "cache-config.dtd">

<cache-config>
  <caching-scheme-mapping>
    <!-- Person cache configuration -->
    <cache-mapping>
      <cache-name>person-cache</cache-name>
      <scheme-name>distributed-scheme</scheme-name>
      <init-params>
        <init-param>
          <param-name>back-size-limit</param-name>
          <param-value>10MB</param-value>
        </init-param>
      </init-params>
    </cache-mapping>
    
    <!-- Animal cache configuration -->
    <cache-mapping>
      <cache-name>animal-cache</cache-name>
      <scheme-name>distributed-scheme</scheme-name>
      <init-params>
        <init-param>
          <param-name>back-size-limit</param-name>
          <param-value>5MB</param-value>
        </init-param>
      </init-params>
    </cache-mapping>

    <!-- Employee cache configuration (using near cache for better performance) -->
    <cache-mapping>
      <cache-name>employee-cache</cache-name>
      <scheme-name>near-scheme</scheme-name>
      <init-params>
        <init-param>
          <param-name>back-size-limit</param-name>
          <param-value>20MB</param-value>
        </init-param>
      </init-params>
    </cache-mapping>
    
    <!-- Session cache using replicated scheme for fast access -->
    <cache-mapping>
      <cache-name>session-cache</cache-name>
      <scheme-name>replicated-scheme</scheme-name>
    </cache-mapping>
    
    <!-- Configuration cache -->
    <cache-mapping>
      <cache-name>config-*</cache-name>
      <scheme-name>replicated-scheme</scheme-name>
    </cache-mapping>
    
    <!-- Default cache mapping for any other caches -->
    <cache-mapping>
      <cache-name>*</cache-name>
      <scheme-name>distributed-scheme</scheme-name>
    </cache-mapping>
  </caching-scheme-mapping>

  <caching-schemes>
    <!-- Distributed Caching Scheme -->
    <distributed-scheme>
      <scheme-name>distributed-scheme</scheme-name>
      <service-name>DistributedCache</service-name>
      
      <!-- Backup configuration -->
      <backup-count system-property="coherence.distributed.backup">1</backup-count>
      <backup-count-after-writebehind>1</backup-count-after-writebehind>
      
      <!-- Partitioning -->
      <partition-count system-property="coherence.distributed.partitions">257</partition-count>
      
      <!-- Local storage -->
      <local-storage system-property="coherence.distributed.localstorage">true</local-storage>
      
      <!-- Transfer threshold -->
      <transfer-threshold>512</transfer-threshold>
      
      <!-- Backing map configuration -->
      <backing-map-scheme>
        <local-scheme>
          <scheme-name>backing-map-local</scheme-name>
          <high-units system-property="coherence.distributed.localstorage.highunits">10000</high-units>
          <unit-calculator>BINARY</unit-calculator>
          <unit-factor>1048576</unit-factor> <!-- 1MB -->
          <expiry-delay>{back-expiry 0}</expiry-delay>
          <flush-delay>0</flush-delay>
          
          <!-- Optional: Overflow to disk -->
          <!--
          <eviction-policy>LRU</eviction-policy>
          <cache-store-scheme>
            <class-scheme>
              <class-name>com.tangosol.io.nio.MappedBufferManager</class-name>
            </class-scheme>
          </cache-store-scheme>
          -->
        </local-scheme>
      </backing-map-scheme>
      
      <!-- Persistence configuration (optional) -->
      <!--
      <persistence>
        <environment>default-active-dir</environment>
      </persistence>
      -->
      
      <autostart>true</autostart>
      
      <!-- Guardian configuration -->
      <guardian-timeout>60000</guardian-timeout>
    </distributed-scheme>

    <!-- Near Caching Scheme -->
    <near-scheme>
      <scheme-name>near-scheme</scheme-name>
      
      <!-- Front cache (local to each client/member) -->
      <front-scheme>
        <local-scheme>
          <scheme-name>front-cache-local</scheme-name>
          <high-units system-property="coherence.near.front.highunits">1000</high-units>
          <unit-calculator>BINARY</unit-calculator>
          <expiry-delay system-property="coherence.near.front.expiry">30s</expiry-delay>
          <flush-delay>0</flush-delay>
          <eviction-policy>LRU</eviction-policy>
        </local-scheme>
      </front-scheme>
      
      <!-- Back cache (distributed) -->
      <back-scheme>
        <distributed-scheme>
          <scheme-ref>distributed-scheme</scheme-ref>
        </distributed-scheme>
      </back-scheme>
      
      <!-- Invalidation strategy -->
      <invalidation-strategy>present</invalidation-strategy>
      
      <!-- Cache miss optimization -->
      <listen-immediately>false</listen-immediately>
      
      <autostart>true</autostart>
    </near-scheme>

    <!-- Replicated Caching Scheme -->
    <replicated-scheme>
      <scheme-name>replicated-scheme</scheme-name>
      <service-name>ReplicatedCache</service-name>
      
      <!-- Backing map -->
      <backing-map-scheme>
        <local-scheme>
          <high-units>5000</high-units>
          <unit-calculator>BINARY</unit-calculator>
          <expiry-delay>0</expiry-delay>
          <eviction-policy>LRU</eviction-policy>
        </local-scheme>
      </backing-map-scheme>
      
      <!-- Lease granularity -->
      <lease-granularity>member</lease-granularity>
      
      <autostart>true</autostart>
      
      <!-- Guardian configuration -->
      <guardian-timeout>60000</guardian-timeout>
    </replicated-scheme>

    <!-- Local Caching Scheme (for standalone use) -->
    <local-scheme>
      <scheme-name>local-scheme</scheme-name>
      <high-units>1000</high-units>
      <unit-calculator>BINARY</unit-calculator>
      <expiry-delay>0</expiry-delay>
      <flush-delay>0</flush-delay>
      <eviction-policy>LRU</eviction-policy>
    </local-scheme>

    <!-- Read-Write Through Cache Scheme (optional) -->
    <!--
    <read-write-backing-map-scheme>
      <scheme-name>read-write-scheme</scheme-name>
      <internal-cache-scheme>
        <local-scheme>
          <high-units>10000</high-units>
          <unit-calculator>BINARY</unit-calculator>
        </local-scheme>
      </internal-cache-scheme>
      
      <cachestore-scheme>
        <class-scheme>
          <class-name>com.example.cache.DatabaseCacheStore</class-name>
          <init-params>
            <init-param>
              <param-type>java.lang.String</param-type>
              <param-value>jdbc:mysql://localhost:3306/mydb</param-value>
            </init-param>
          </init-params>
        </class-scheme>
      </cachestore-scheme>
      
      <write-delay>0</write-delay>
      <write-batch-factor>0.25</write-batch-factor>
      <write-requeue-threshold>0</write-requeue-threshold>
      <rollback-cachestore-failures>true</rollback-cachestore-failures>
    </read-write-backing-map-scheme>
    -->

    <!-- Overflow Scheme (memory + disk) -->
    <!--
    <overflow-scheme>
      <scheme-name>overflow-scheme</scheme-name>
      <front-scheme>
        <local-scheme>
          <high-units>1000</high-units>
          <unit-calculator>BINARY</unit-calculator>
        </local-scheme>
      </front-scheme>
      <back-scheme>
        <external-scheme>
          <bdb-store-manager>
            <directory>coherence-data</directory>
            <store-name>{cache-name}</store-name>
          </bdb-store-manager>
        </external-scheme>
      </back-scheme>
    </overflow-scheme>
    -->
  </caching-schemes>

  <!-- Default values and parameters -->
  <defaults>
    <serializer>pof</serializer>
    <scope-name system-property="coherence.scope">MyApp</scope-name>
  </defaults>
</cache-config>


<?xml version="1.0"?>
<!DOCTYPE pof-config SYSTEM "pof-config.dtd">

<pof-config>
  <user-type-list>
    <!-- Include default Coherence POF types -->
    <include>coherence-pof-config.xml</include>
    
    <!-- Auto-discover POF annotated classes from specified packages -->
    <user-type>
      <type-id>1000</type-id>
      <class-name>com.tangosol.io.pof.reflect.PofAnnotationSerializer</class-name>
      <init-params>
        <init-param>
          <param-type>java.lang.String</param-type>
          <param-value>com.example.model</param-value>
        </init-param>
      </init-params>
    </user-type>
  </user-type-list>
  
  <!-- Enable POF annotations and references -->
  <enable-references>true</enable-references>
  <allow-subclasses>true</allow-subclasses>
  <allow-interfaces>true</allow-interfaces>
</pof-config>


package com.example.model;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@PortableType(id = 1001, version = 0)
public class Person {
    
    @Portable(0)
    private String name;
    
    @Portable(1)
    private int age;
}

package com.example.model;

import com.tangosol.io.pof.schema.annotation.Portable;
import com.tangosol.io.pof.schema.annotation.PortableType;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Getter;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@PortableType(id = 1002, version = 0)
public class Animal {
    
    @Portable(0)
    private String species;
    
    @Portable(1)
    private int legs;
}














<!-- Enable POF globally -->
    <serializer>
      <instance>
        <class-name>com.tangosol.io.pof.ConfigurablePofContext</class-name>
        <init-params>
          <init-param>
            <param-type>String</param-type>
            <param-value>pof-config.xml</param-value> <!-- Path to POF config -->
          </init-param>
        </init-params>
      </instance>
    </serializer>
  </caching-schemes>
</cache-config>

                                                    
