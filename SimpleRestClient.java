import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class SimpleRestClient {

    public static void main(String[] args) throws Exception {
        // === API Configuration ===
        String url = "https://your-api.com/endpoint";  // TODO: update to your actual API endpoint

        // SSL certificate paths
        String certPath = "/path/to/client.crt";        // client certificate (PEM or CRT)
        String keyPath = "/path/to/client.key";         // client private key
        String caPath = "/path/to/ca.pem";              // optional: CA root cert
        String certPassword = "your_cert_password";     // if required

        // Basic authentication
        String username = "your_username";
        String password = "your_password";

        // JSON Body
        String jsonPayload = """
                {
                  "ID": "APPS",
                  "Safe": "IV2",
                  "Object": "myproject"
                }
                """;

        // === Setup SSL Context ===
        SslContext sslContext = buildSslContext(certPath, keyPath, caPath, certPassword);

        // === Build WebClient ===
        WebClient webClient = buildWebClient(sslContext, username, password);

        // === Send POST Request ===
        Mono<ResponseEntity<String>> responseMono = webClient
                .method(HttpMethod.POST)
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(jsonPayload)
                .retrieve()
                .toEntity(String.class);

        // === Block and print result ===
        ResponseEntity<String> response = responseMono.block();

        if (response != null) {
            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Response Body:\n" + response.getBody());
        } else {
            System.out.println("No response received.");
        }
    }

    private static WebClient buildWebClient(SslContext sslContext, String username, String password) {
        HttpClient httpClient = HttpClient.create()
                .secure(ssl -> ssl.sslContext(sslContext));

        String auth = username + ":" + password;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
        String authHeader = "Basic " + encodedAuth;

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.AUTHORIZATION, authHeader)
                .build();
    }

    private static SslContext buildSslContext(String certPath, String keyPath, String caPath, String certPassword) throws SSLException {
        return SslContextBuilder.forClient()
                .keyManager(new File(certPath), new File(keyPath), certPassword)
                .trustManager(caPath != null ? new File(caPath) : null)
                .build();
    }
}
