import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.Optional;

public class WebClientFactory implements Factory<WebClient, ApiConfig> {

    private static final int DEFAULT_MAX_CONNECTIONS = 200;
    private static final int DEFAULT_PENDING_ACQUIRE_TIMEOUT_SEC = 30;
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10_000;
    private static final int DEFAULT_MAX_IN_MEMORY_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int DEFAULT_READ_TIMEOUT_SEC = 120;
    private static final int DEFAULT_WRITE_TIMEOUT_SEC = 120;

    @Override
    public WebClient create(ApiConfig apiConfig) {
        return buildWebClient(apiConfig);
    }

    private WebClient buildWebClient(ApiConfig apiConfig) {
        // Connection pool with keep-alive + idle time
        ConnectionProvider provider = ConnectionProvider.builder(apiConfig.getName() + "-pool")
                .maxConnections(Optional.ofNullable(apiConfig.getMaxConnections()).orElse(DEFAULT_MAX_CONNECTIONS))
                .pendingAcquireTimeout(Duration.ofSeconds(
                        Optional.ofNullable(apiConfig.getPendingAcquireTimeoutSec()).orElse(DEFAULT_PENDING_ACQUIRE_TIMEOUT_SEC)))
                .maxIdleTime(Duration.ofSeconds(Optional.ofNullable(apiConfig.getMaxIdleTimeSec()).orElse(60))) // idle timeout
                .maxLifeTime(Duration.ofMinutes(5)) // optional: total lifetime
                .build();

        // HttpClient
        HttpClient httpClient = HttpClient.create(provider)
                .secure(ssl -> ssl.sslContext(apiConfig.getSslContext()))
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        Optional.ofNullable(apiConfig.getConnectTimeoutMillis()).orElse(DEFAULT_CONNECT_TIMEOUT_MS))
                .keepAlive(true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .doOnConnected(conn -> {
                    if (apiConfig.getReadTimeoutSec() != null || apiConfig.getWriteTimeoutSec() != null) {
                        if (apiConfig.getReadTimeoutSec() != null) {
                            conn.addHandlerLast(new ReadTimeoutHandler(apiConfig.getReadTimeoutSec()));
                        } else {
                            conn.addHandlerLast(new ReadTimeoutHandler(DEFAULT_READ_TIMEOUT_SEC));
                        }

                        if (apiConfig.getWriteTimeoutSec() != null) {
                            conn.addHandlerLast(new WriteTimeoutHandler(apiConfig.getWriteTimeoutSec()));
                        } else {
                            conn.addHandlerLast(new WriteTimeoutHandler(DEFAULT_WRITE_TIMEOUT_SEC));
                        }
                    }
                });

        // Exchange strategies (memory limits)
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(Optional.ofNullable(apiConfig.getMaxInMemorySize())
                                .orElse(DEFAULT_MAX_IN_MEMORY_SIZE)))
                .build();

        // Build and return WebClient
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(exchangeStrategies)
                .baseUrl(apiConfig.getBaseUrl())
                .build();
    }

    @Override
    public String generateKey(ApiConfig apiConfig) {
        return apiConfig.getName();
    }
}


public class ApiConfig {

    private String name;
    private String baseUrl;
    private String type; // REST / SOAP
    private SslContext sslContext;

    // Connection pooling and timeout settings
    private Integer maxConnections;           // e.g., 50 or 200
    private Integer pendingAcquireTimeoutSec; // e.g., 15
    private Integer connectTimeoutMillis;     // e.g., 10000

    // Optional per-API read/write timeouts (null = disabled)
    private Integer readTimeoutSec;           // e.g., 15, 120 for long-running APIs
    private Integer writeTimeoutSec;          // e.g., 15

apis:
  toto:
    name: "ApiToto"
    baseUrl: "https://toto.example.com"
    type: "REST"
    maxConnections: 100
    pendingAcquireTimeoutSec: 10
    connectTimeoutMillis: 8000
    readTimeoutSec: 30
    writeTimeoutSec: 15
    certificate:
      crt: "classpath:certs/toto.crt"
      key: "classpath:certs/toto.key"
      pem: "classpath:certs/toto.pem"

  lala:
    name: "ApiLala"
    baseUrl: "https://lala.example.com"
    type: "REST"
    maxConnections: 50
    pendingAcquireTimeoutSec: 15
    connectTimeoutMillis: 10000
    readTimeoutSec: 120
    writeTimeoutSec: 30
    certificate:
      crt: "classpath:certs/lala.crt"
      key: "classpath:certs/lala.key"
      pem: "classpath:certs/lala.pem"



/**
     * Maximum number of concurrent HTTP connections that can be opened to this API.
     * <p>
     * Used to control the connection pool size for this API client.
     * A higher number allows more parallel requests but increases memory and socket usage.
     */
    private Integer maxConnections;

    /**
     * Maximum time (in seconds) a request will wait for a free connection
     * from the connection pool before failing with a timeout error.
     * <p>
     * Useful when all connections are busy — prevents requests from waiting indefinitely.
     */
    private Integer pendingAcquireTimeoutSec;

    /**
     * Maximum time (in milliseconds) allowed for establishing a TCP connection
     * to the remote API server.
     * <p>
     * If the API host is unreachable or slow to respond at the socket level,
     * this timeout controls how long the client will attempt to connect.
     */
    private Integer connectTimeoutMillis;

    /**
     * Maximum duration (in seconds) allowed to read the response
     * once the connection is established and data starts flowing.
     * <p>
     * Prevents hanging on APIs that are slow to return or stream incomplete data.
     */
    private Integer readTimeoutSec;

    /**
     * Maximum duration (in seconds) allowed for sending the request body
     * to the API (e.g., uploading data or a large JSON payload).
     * <p>
     * Protects the client from being stuck when the server or network is slow
     * to receive outgoing data.
     */
    private Integer writeTimeoutSec;
