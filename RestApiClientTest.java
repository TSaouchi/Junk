package com.example.executor;

import com.example.executor.cache.Cache;
import com.example.executor.config.ApiConfig;
import com.example.executor.factory.Factory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class RestApiClientTest {

    private RestApiClient restApiClient;

    private Cache<WebClient, ApiConfig> webClientCache;
    private Factory<WebClient, ApiConfig> webClientFactory;

    private Cache<Map<String, String>, ApiConfig> authHeaderCache;
    private Factory<Map<String, String>, ApiConfig> authHeaderFactory;

    private ApiConfig apiConfig;

    @BeforeEach
    void setup() {
        webClientCache = mock(Cache.class);
        webClientFactory = mock(Factory.class);
        authHeaderCache = mock(Cache.class);
        authHeaderFactory = mock(Factory.class);

        // Wrap mocks in lists and inject
        List<Cache<?, ApiConfig>> caches = List.of(webClientCache, authHeaderCache);
        List<Factory<?, ApiConfig>> factories = List.of(webClientFactory, authHeaderFactory);

        when(webClientCache.getCacheName()).thenReturn("WebClientCache");
        when(webClientFactory.getFactoryName()).thenReturn("WebClientFactory");

        when(authHeaderCache.getCacheName()).thenReturn("AuthHeaderCache");
        when(authHeaderFactory.getFactoryName()).thenReturn("AuthHeaderFactory");

        restApiClient = new RestApiClient(caches, factories);

        apiConfig = new ApiConfig();
        apiConfig.setApiName("MyTestApi");
        apiConfig.setBaseUrl("https://example.com");
    }

    @Test
    @Test
void testExecuteRestCall() {
    WebClient mockClient = mock(WebClient.class);
    WebClient.RequestBodyUriSpec methodSpec = mock(WebClient.RequestBodyUriSpec.class);
    WebClient.RequestBodySpec uriSpec = mock(WebClient.RequestBodySpec.class);
    WebClient.RequestHeadersSpec<?> headersSpec = mock(WebClient.RequestHeadersSpec.class);
    WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

    Map<String, String> headers = Map.of("Authorization", "Basic XXX");

    when(webClientCache.getOrCreate(eq(apiConfig), eq(webClientFactory)))
            .thenReturn(mockClient);

    when(authHeaderCache.getOrCreate(eq(apiConfig), eq(authHeaderFactory)))
            .thenReturn(headers);

    // Mock fluent chain
    when(mockClient.method(any())).thenReturn(methodSpec);
    when(methodSpec.uri(any(Function.class))).thenReturn(uriSpec);
    when(uriSpec.headers(any())).thenReturn(uriSpec);
    when(uriSpec.bodyValue(any())).thenReturn(headersSpec);
    when(headersSpec.retrieve()).thenReturn(responseSpec);
    when(responseSpec.toEntity(eq(String.class))).thenReturn(Mono.just(ResponseEntity.ok("OK")));

    // Execute
    Mono<ResponseEntity<String>> response = restApiClient.execute(
            apiConfig,
            HttpMethod.GET,
            "/test",
            null,
            String.class,
            null,
            new LinkedMultiValueMap<>()
    );

    ResponseEntity<String> result = response.block();
    assertNotNull(result);
    assertEquals("OK", result.getBody());
    }
}
