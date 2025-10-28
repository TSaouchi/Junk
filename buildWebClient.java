private WebClient buildWebClient(ApiConfig apiConfig) {

    // 1. Connection pool per API, configurable via ApiConfig
    ConnectionProvider provider = ConnectionProvider.builder(apiConfig.getName() + "-pool")
            .maxConnections(apiConfig.getMaxConnections() != null ? apiConfig.getMaxConnections() : 50)
            .pendingAcquireTimeout(Duration.ofSeconds(
                    apiConfig.getPendingAcquireTimeoutSec() != null ? apiConfig.getPendingAcquireTimeoutSec() : 15))
            .build();

    // 2. HttpClient with pooling, SSL, and optional read/write timeouts
    HttpClient httpClient = HttpClient.create(provider)
            .resolver(DefaultAddressResolverGroup.INSTANCE)
            .secure(ssl -> ssl.sslContext(apiConfig.getSslContext()))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                    apiConfig.getConnectTimeoutMillis() != null ? apiConfig.getConnectTimeoutMillis() : 10000);

    // Optional read/write timeouts (can be null for long-running APIs)
    if (apiConfig.getReadTimeoutSec() != null || apiConfig.getWriteTimeoutSec() != null) {
        httpClient = httpClient.doOnConnected(conn -> {
            if (apiConfig.getReadTimeoutSec() != null)
                conn.addHandlerLast(new ReadTimeoutHandler(apiConfig.getReadTimeoutSec()));
            if (apiConfig.getWriteTimeoutSec() != null)
                conn.addHandlerLast(new WriteTimeoutHandler(apiConfig.getWriteTimeoutSec()));
        });
    }

    // 3. Exchange strategies (keep your existing memory buffer)
    ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
            .codecs(configurer -> configure.defaultCodecs()
                    .maxInMemorySize(50 * 1024 * 1024))
            .build();

    // 4. Build WebClient
    WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(exchangeStrategies)
            .baseUrl(apiConfig.getBaseUrl())
            .build();

    return webClient;
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
