package com.example.client;

import com.example.cache.Cache;
import com.example.factory.Factory;
import com.example.model.ApiConfig;
import com.example.executor.RestApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.*;
import org.springframework.web.reactive.function.client.*;

import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class RestApiClientTest {

    private RestApiClient restApiClient;

    private WebClient mockClient;
    private WebClient.RequestBodyUriSpec methodSpec;
    private WebClient.RequestBodySpec uriSpec;

    @SuppressWarnings("rawtypes")
    private WebClient.RequestHeadersSpec headersSpec;

    private WebClient.ResponseSpec responseSpec;

    private Cache<WebClient, ApiConfig> webClientCache;
    private Cache<Map<String, String>, ApiConfig> authHeaderCache;
    private Factory<WebClient, ApiConfig> webClientFactory;
    private Factory<Map<String, String>, ApiConfig> authHeaderFactory;

    private ApiConfig apiConfig;

    @BeforeEach
    void setUp() {
        // Mocks
        mockClient = mock(WebClient.class);
        methodSpec = mock(WebClient.RequestBodyUriSpec.class);
        uriSpec = mock(WebClient.RequestBodySpec.class);
        headersSpec = mock(WebClient.RequestHeadersSpec.class);
        responseSpec = mock(WebClient.ResponseSpec.class);

        // Factories and caches
        webClientCache = mock(Cache.class);
        authHeaderCache = mock(Cache.class);
        webClientFactory = mock(Factory.class);
        authHeaderFactory = mock(Factory.class);

        // Prepare ApiConfig
        apiConfig = new ApiConfig();
        apiConfig.setApiName("testApi");
        apiConfig.setBaseUrl("http://example.com");

        // Initialize RestApiClient
        Map<String, Cache<?, ApiConfig>> cachesMap = List.of(webClientCache, authHeaderCache).stream()
                .collect(Collectors.toMap(c -> c.getClass().getSimpleName(), Function.identity()));

        Map<String, Factory<?, ApiConfig>> factoriesMap = List.of(webClientFactory, authHeaderFactory).stream()
                .collect(Collectors.toMap(f -> f.getClass().getSimpleName(), Function.identity()));

        restApiClient = new RestApiClient(cachesMap, factoriesMap);
    }

    @Test
    void testExecute_withValidInputs_returnsResponseEntity() {
        // Headers
        Map<String, String> authHeaders = Map.of("Authorization", "Basic XXX");

        // Mock cache/factory behavior
        when(webClientCache.getCacheName()).thenReturn(webClientCache.getClass().getSimpleName());
        when(authHeaderCache.getCacheName()).thenReturn(authHeaderCache.getClass().getSimpleName());
        when(webClientFactory.getFactoryName()).thenReturn(webClientFactory.getClass().getSimpleName());
        when(authHeaderFactory.getFactoryName()).thenReturn(authHeaderFactory.getClass().getSimpleName());

        when(webClientCache.getOrCreate(eq(apiConfig), eq(webClientFactory))).thenReturn(mockClient);
        when(authHeaderCache.getOrCreate(eq(apiConfig), eq(authHeaderFactory))).thenReturn(authHeaders);

        // Mock fluent WebClient chain
        when(mockClient.method(any())).thenReturn(methodSpec);
        when(methodSpec.uri(any(Function.class))).thenReturn(uriSpec);
        when(uriSpec.headers(any())).thenReturn(uriSpec);
        when(uriSpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(eq(String.class))).thenReturn(Mono.just(ResponseEntity.ok("OK")));

        // Call method
        Mono<ResponseEntity<String>> responseMono = restApiClient.execute(
                apiConfig,
                HttpMethod.GET,
                "/test-endpoint",
                null,
                String.class,
                null,
                new LinkedMultiValueMap<>()
        );

        ResponseEntity<String> response = responseMono.block();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("OK", response.getBody());
    }
}
