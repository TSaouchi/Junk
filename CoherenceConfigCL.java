----------------------------------- Server SIde ------------------------
cache config
<?xml version="1.0" encoding="UTF-8"?>
<cache-config xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xmlns="http://xmlns.oracle.com/coherence/coherence-cache-config"
              xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-cache-config
              coherence-cache-config.xsd">

  <caching-scheme-mapping>
    <cache-mapping>
      <cache-name>*</cache-name>
      <scheme-name>distributed-scheme</scheme-name>
    </cache-mapping>
  </caching-scheme-mapping>

  <caching-schemes>
    <distributed-scheme>
      <scheme-name>distributed-scheme</scheme-name>
      <service-name>DistributedCache</service-name>
      <backing-map-scheme>
        <local-scheme>
          <scheme-name>local-default</scheme-name>
        </local-scheme>
      </backing-map-scheme>
      <autostart>true</autostart>
    </distributed-scheme>

    <!-- Remote cache scheme for Extend clients -->
    <remote-cache-scheme>
      <scheme-name>extend-direct</scheme-name>
      <service-name>ExtendTcpCacheService</service-name>
      <initiator-config>
        <tcp-initiator>
          <remote-addresses>
            <socket-address>
              <address>localhost</address>
              <port>9099</port>
            </socket-address>
          </remote-addresses>
        </tcp-initiator>
        <outgoing-message-handler>
          <request-timeout>30s</request-timeout>
        </outgoing-message-handler>
      </initiator-config>
    </remote-cache-scheme>

    <!-- Proxy scheme for server-side Extend support -->
    <proxy-scheme>
      <scheme-name>extend-tcp-proxy</scheme-name>
      <service-name>ExtendTcpProxyService</service-name>
      <acceptor-config>
        <tcp-acceptor>
          <local-address>
            <address>0.0.0.0</address>
            <port>9099</port>
          </local-address>
        </tcp-acceptor>
      </acceptor-config>
      <autostart>true</autostart>
    </proxy-scheme>
  </caching-schemes>
</cache-config>

override
<?xml version="1.0" encoding="UTF-8"?>
<coherence xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns="http://xmlns.oracle.com/coherence/coherence-operational-config"
           xsi:schemaLocation="http://xmlns.oracle.com/coherence/coherence-operational-config
           coherence-operational-config.xsd">

  <cluster-config>
    <member-identity>
      <cluster-name>MyCluster</cluster-name>
    </member-identity>
  </cluster-config>

  <logging-config>
    <severity-level>5</severity-level>
    <message-format>{date} {product} {version} &lt;{level}&gt; (thread={thread}, member={member}): {text}</message-format>
  </logging-config>

  <!-- Enable Management over REST -->
  <management-config>
    <allow-remote-management>true</allow-remote-management>
    <default-domain-name>Coherence</default-domain-name>
    <read-only>false</read-only>
  </management-config>

  <!-- Enable Metrics -->
  <metrics-config>
    <enabled>true</enabled>
    <http-enabled>true</http-enabled>
    <http-port>9612</http-port>
    <extended-statistics>true</extended-statistics>
  </metrics-config>

  <!-- Enable HTTP Management -->
  <management-config>
    <management-http>
      <http-port>30000</http-port>
      <http-path>/management</http-path>
    </management-http>
  </management-config>

  <!-- Extend proxy service configuration -->
  <services>
    <service>
      <service-name>ExtendTcpProxyService</service-name>
      <service-type>Proxy</service-type>
      <init-params>
        <init-param>
          <param-name>acceptor-config</param-name>
          <param-value>
            <tcp-acceptor>
              <local-address>
                <address>0.0.0.0</address>
                <port>9099</port>
              </local-address>
            </tcp-acceptor>
          </param-value>
        </init-param>
        <init-param>
          <param-name>proxy-config</param-name>
          <param-value>
            <cache-service-proxy>
              <enabled>true</enabled>
            </cache-service-proxy>
          </param-value>
        </init-param>
      </init-params>
    </service>
  </services>

</coherence>

sh 
#!/bin/bash

# Oracle Coherence Startup Script with Metrics Management Enabled

# Set Coherence Home (adjust path as needed)
export COHERENCE_HOME=/opt/oracle/coherence

# Set Java Home
export JAVA_HOME=/opt/java

# Coherence JAR files
COHERENCE_JAR=$COHERENCE_HOME/lib/coherence.jar
COHERENCE_MGMT_JAR=$COHERENCE_HOME/lib/coherence-management.jar
COHERENCE_METRICS_JAR=$COHERENCE_HOME/lib/coherence-metrics.jar

# JVM Memory Settings
JVM_HEAP="-Xms2g -Xmx4g"
JVM_OPTS="-XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Coherence System Properties
COHERENCE_OPTS="-Dcoherence.cluster=MyCluster"
COHERENCE_OPTS="$COHERENCE_OPTS -Dcoherence.member=CoherenceServer1"
COHERENCE_OPTS="$COHERENCE_OPTS -Dcoherence.role=CoherenceServer"
COHERENCE_OPTS="$COHERENCE_OPTS -Dcoherence.cacheconfig=coherence-cache-config.xml"
COHERENCE_OPTS="$COHERENCE_OPTS -Dcoherence.override=tangosol-coherence-override.xml"

# Extend Proxy Service Configuration
EXTEND_OPTS="-Dcoherence.proxy.enabled=true"
EXTEND_OPTS="$EXTEND_OPTS -Dcoherence.proxy.port=9099"
EXTEND_OPTS="$EXTEND_OPTS -Dcoherence.proxy.address=0.0.0.0"

# Management and Metrics Configuration
MGMT_OPTS="-Dcoherence.management=all"
MGMT_OPTS="$MGMT_OPTS -Dcoherence.management.remote=true"
MGMT_OPTS="$MGMT_OPTS -Dcom.sun.management.jmxremote"
MGMT_OPTS="$MGMT_OPTS -Dcom.sun.management.jmxremote.port=9999"
MGMT_OPTS="$MGMT_OPTS -Dcom.sun.management.jmxremote.ssl=false"
MGMT_OPTS="$MGMT_OPTS -Dcom.sun.management.jmxremote.authenticate=false"

# Metrics specific properties
METRICS_OPTS="-Dcoherence.metrics.http.enabled=true"
METRICS_OPTS="$METRICS_OPTS -Dcoherence.metrics.http.port=9612"
METRICS_OPTS="$METRICS_OPTS -Dcoherence.metrics.extended=true"

# HTTP Management REST endpoint
HTTP_MGMT_OPTS="-Dcoherence.management.http=inherit"
HTTP_MGMT_OPTS="$HTTP_MGMT_OPTS -Dcoherence.management.http.port=30000"
HTTP_MGMT_OPTS="$HTTP_MGMT_OPTS -Dcoherence.management.http.path=/management"

# Classpath
CLASSPATH="$COHERENCE_JAR:$COHERENCE_MGMT_JAR:$COHERENCE_METRICS_JAR:."

# Start Coherence Server
echo "Starting Oracle Coherence with Metrics Management..."
echo "Management HTTP endpoint: http://localhost:30000/management"
echo "Metrics endpoint: http://localhost:9612/metrics"
echo "JMX endpoint: service:jmx:rmi:///jndi/rmi://localhost:9999/jmxrmi"

$JAVA_HOME/bin/java \
    $JVM_HEAP \
    $JVM_OPTS \
    $COHERENCE_OPTS \
    $EXTEND_OPTS \
    $MGMT_OPTS \
    $METRICS_OPTS \
    $HTTP_MGMT_OPTS \
    -cp $CLASSPATH \
    com.tangosol.net.DefaultCacheServer

# Alternative for Storage Disabled member (client)
# com.tangosol.net.CacheFactory
