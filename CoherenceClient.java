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

# Root Logger
log4j.rootLogger=DEBUG, stdout, R

# Console Appender
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d [user: %x] %-5p (%F:%L) - %m%n

# Rolling File Appender
log4j.appender.R=org.apache.log4j.RollingFileAppender
log4j.appender.R.File=/data/primerisk/coherence/logs/COHERENCE_PRIMERISK.log
log4j.appender.R.MaxFileSize=10MB
log4j.appender.R.MaxBackupIndex=10
log4j.appender.R.layout=org.apache.log4j.PatternLayout
log4j.appender.R.layout.ConversionPattern=%d [user: %x] %-5p (%F:%L) - %m%n

# Reduce noisy logs from these packages
log4j.category.org=INFO
log4j.category.net=INFO

# Optional: control coherence logging
log4j.logger.com.tangosol=DEBUG
log4j.logger.com.tangosol.coherence.management=DEBUG
log4j.logger.com.tangosol.coherence.management.server=DEBUG

#!/bin/sh

# ------------------------------------------------------------------------
# Start Oracle Coherence cache server with REST and JMX management enabled
# ------------------------------------------------------------------------

# Java installation
echo "Configuring Java home"
export JAVA_HOME="/apps/install/java/jdk11_current"
JAVA_EXEC="$JAVA_HOME/bin/java"

# Coherence installation
echo "Configuring Coherence home"
export COHERENCE_HOME="/apps/install/coherence/14.1.1.0.0/coherence"

# JVM memory
export XMS="150m"
export XMX="150m"

# Cluster settings
export CLUSTER_NAME="COHERENCE_PRIMERISK"
export CLUSTER_HOST="127.0.0.1"                         # Adjust to your real hostname/IP
export LOCAL_CLUSTER_PORT="9000"
export CLUSTER_PORT="9601"
export COHERENCE_MEMBER="COH_PRIMERISK_$(hostname)"

# Logging and config files
export LOG4J_CONFIG="/data/primerisk/coherence/conf/COHERENCE_PRIMERISK-log4j.configuration"
export CACHE_CONFIG="/data/primerisk/coherence/conf/COHERENCE_PRIMERISK-tangosol-cache-config.xml"
export CACHE_OVERRIDE="/data/primerisk/coherence/conf/COHERENCE_PRIMERISK-tangosol-coherence-override.xml"

# Management configuration (for HTTP-REST)
export MANAGEMENT_CONFIG="/data/primerisk/coherence/conf/coherence-management-config.xml"

# JMX configuration
export JMX_PROPS="-Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9085 \
  -Dcom.sun.management.jmxremote.rmi.port=9085 \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Dcom.sun.management.jmxremote.authenticate=true \
  -Dcom.sun.management.jmxremote.password.file=/data/primerisk/coherence/conf/jmxremote.password \
  -Dcom.sun.management.jmxremote.access.file=/data/primerisk/coherence/conf/jmxremote.access \
  -Djava.rmi.server.hostname=${CLUSTER_HOST}"

# REST Management (Port 9005)
export REST_PROPS="-Dcoherence.management=all \
  -Dcoherence.management.remote=true \
  -Dcoherence.rest.enabled=true \
  -Dcoherence.rest.port=9005 \
  -Dcoherence.management.config=file:${MANAGEMENT_CONFIG}"

# JVM tuning
export JVM_TUNING="-Xms${XMS} -Xmx${XMX} \
  -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=256m \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -XX:+AlwaysPreTouch"

# Complete Java options
export JAVA_OPTS="${JVM_TUNING} ${JMX_PROPS} ${REST_PROPS} \
  -Dcoherence.edition=EE \
  -Dcoherence.mode=prod \
  -Dcoherence.cluster=${CLUSTER_NAME} \
  -Dcoherence.localhost=${CLUSTER_HOST} \
  -Dcoherence.localport=${LOCAL_CLUSTER_PORT} \
  -Dcoherence.clusterport=${CLUSTER_PORT} \
  -Dcoherence.log.level=4 \
  -Dcoherence.cacheconfig=${CACHE_CONFIG} \
  -Dcoherence.override=${CACHE_OVERRIDE} \
  -Dcoherence.distributed.localstorage=true \
  -Dcoherence.member=${COHERENCE_MEMBER} \
  -Dlog4j.configuration=file:${LOG4J_CONFIG} \
  -Djava.rmi.dgc.leaseValue=3600000 \
  -Dsun.rmi.dgc.client.gcInterval=3600000 \
  -Dsun.rmi.dgc.server.gcInterval=3600000"

# PID file
COH_PIDFILE="/data/primerisk/coherence/logs/coherence-dev-primerisk.pid"

# Classpath: include management & REST jars
CLASSPATH="$COHERENCE_HOME/lib/coherence.jar: \
           $COHERENCE_HOME/lib/coherence-management.jar: \
           $COHERENCE_HOME/lib/coherence-rest.jar: \
           /data/primerisk/coherence/lib/log4j-1.2.17.jar: \
           $COHERENCE_HOME/lib/*"

# Find current process (if any)
get_pid() {
  ps -ef | grep "com.tangosol.net.DefaultCacheServer" | grep "${COHERENCE_MEMBER}" | grep -v grep | awk '{print $2}'
}

# Start
start_coherence() {
  PID=$(get_pid)
  if [ -n "$PID" ]; then
    echo "Coherence is already running with PID $PID"
    exit 1
  fi
  echo "Starting Coherence with REST and JMX enabled..."
  nohup "$JAVA_EXEC" -server $JAVA_OPTS -cp "$CLASSPATH" com.tangosol.net.DefaultCacheServer > /dev/null 2>&1 &
  echo $! > "$COH_PIDFILE"
  echo "Started. PID: $(cat "$COH_PIDFILE")"
}

# Stop
stop_coherence() {
  if [ -f "$COH_PIDFILE" ]; then
    PID=$(cat "$COH_PIDFILE")
    echo "Stopping Coherence (PID $PID)..."
    kill "$PID"
    rm -f "$COH_PIDFILE"
    echo "Stopped."
  else
    echo "No Coherence process found."
  fi
}

# Status
status_coherence() {
  PID=$(get_pid)
  if [ -n "$PID" ]; then
    echo "Coherence is running with PID $PID"
  else
    echo "Coherence is not running."
  fi
}

# Main entry
case "$1" in
  start)
    start_coherence
    ;;
  stop)
    stop_coherence
    ;;
  status)
    status_coherence
    ;;
  *)
    echo "Usage: $0 {start|stop|status}"
    exit 1
    ;;
esac

  # Root Logger
log4j.rootLogger=DEBUG, stdout, R

# Console Appender
log4j.appender.stdout=org.apache.log4j.ConsoleAppender
log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
log4j.appender.stdout.layout.ConversionPattern=%d [user: %x] %-5p (%F:%L) - %m%n

# Rolling File Appender
log4j.appender.R=org.apache.log4j.RollingFileAppender
log4j.appender.R.File=/data/primerisk/coherence/logs/COHERENCE_PRIMERISK.log
log4j.appender.R.MaxFileSize=10MB
log4j.appender.R.MaxBackupIndex=10
log4j.appender.R.layout=org.apache.log4j.PatternLayout
log4j.appender.R.layout.ConversionPattern=%d [user: %x] %-5p (%F:%L) - %m%n

# Reduce noisy logs from these packages
log4j.category.org=INFO
log4j.category.net=INFO

# Optional: control coherence logging
log4j.logger.com.tangosol=DEBUG
log4j.logger.com.tangosol.coherence.management=DEBUG
log4j.logger.com.tangosol.coherence.management.server=DEBUG



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

  
