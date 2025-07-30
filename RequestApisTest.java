package com.bnpparibas.cib.psnf.maestro.service;

import com.bnpparibas.cib.psnf.maestro.apis.ApiResponse;
import com.bnpparibas.cib.psnf.maestro.apis.ApiResponselwrap;
import com.bnpparibas.cib.psnf.maestro.apis.productmasterrest.config.ProductMasterRest;
import com.bnpparibas.cib.psnf.maestro.apis.productmasterrest.responsehandler.DataframeParser;
import com.bnpparibas.cib.psnf.maestro.apis.productmasterrest.responsehandler.JsonRawResponse;
import com.bnpparibas.cib.psnf.maestro.apis.productmastersoap.config.ProductMasterSoap;
import com.bnpparibas.cib.psnf.maestro.apis.productmastersoap.responsehandler.XMLParser;
import com.bnpparibas.cib.psnf.maestro.apis.productmastersoap.responsehandler.XmlRawResponse;
import com.bnpparibas.cib.psnf.maestro.tools.apiengine.service.ApiExecutionService;
import com.bnpparibas.cib.psnf.maestro.tools.reactiveengine.executor.ReactiveEngine;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestApisTest {

    @Mock
    private ApiExecutionService apiExecutionService;

    @Mock
    private ProductMasterRest productMasterRest;

    @Mock
    private ProductMasterSoap productMasterSoap;

    private RequestApis requestApis;

    @BeforeEach
    void setUp() {
        requestApis = new RequestApis(apiExecutionService, productMasterRest, productMasterSoap);
    }

    @Test
    void testRestApiSuccessResponseWrapping() {
        String service = "PRICE";
        String expectedJson = "{\"dataframe\": [{\"key\": \"value\"}]}";

        productMasterRest.apiConfig = mock(Object.class);
        productMasterRest.endpoints = Map.of(service, "/mock/rest/endpoint");

        ResponseEntity<Object> mockResponse = new ResponseEntity<>(expectedJson, HttpStatus.OK);
        when(apiExecutionService.execute(
                eq(productMasterRest.apiConfig),
                eq("/mock/rest/endpoint"),
                eq(HttpMethod.GET),
                isNull(),
                eq(Object.class),
                isNull(),
                any(MultiValueMap.class)
        )).thenReturn(Mono.just(mockResponse));

        Mono<ApiResponse> wrapped = ApiResponselwrap.wrap(apiExecutionService.execute(
                productMasterRest.apiConfig,
                "/mock/rest/endpoint",
                HttpMethod.GET,
                null,
                Object.class,
                null,
                new LinkedMultiValueMap<>()
        ));

        StepVerifier.create(wrapped)
                .assertNext(apiResponse -> {
                    assertTrue(apiResponse instanceof JsonRawResponse);
                    assertEquals(expectedJson, ((JsonRawResponse) apiResponse).getBody());
                })
                .verifyComplete();
    }

    @Test
    void testSoapApiSuccessResponseWrapping() {
        String soapService = "VOLATILITY";
        String expectedXml = "<soap:Envelope><data>value</data></soap:Envelope>";

        productMasterSoap.apiConfig = mock(Object.class);
        productMasterSoap.endpoints = Map.of(soapService, "/mock/soap/endpoint");
        productMasterSoap.username = "user";
        productMasterSoap.password = "pass";
        productMasterSoap.header = HttpHeaders.EMPTY;

        ResponseEntity<String> mockResponse = new ResponseEntity<>(expectedXml, HttpStatus.OK);
        when(apiExecutionService.execute(
                eq(productMasterSoap.apiConfig),
                eq(HttpMethod.POST),
                eq("/mock/soap/endpoint"),
                eq(String.class),
                any(HttpHeaders.class),
                anyString(),
                isNull()
        )).thenReturn(Mono.just(mockResponse));

        Mono<ApiResponse> wrapped = ApiResponselwrap.wrap(apiExecutionService.execute(
                productMasterSoap.apiConfig,
                HttpMethod.POST,
                "/mock/soap/endpoint",
                String.class,
                HttpHeaders.EMPTY,
                "<requestBody>",
                null
        ));

        StepVerifier.create(wrapped)
                .assertNext(apiResponse -> {
                    assertTrue(apiResponse instanceof XmlRawResponse);
                    assertEquals(expectedXml, ((XmlRawResponse) apiResponse).getBody());
                })
                .verifyComplete();
    }

    @Test
    void testErrorFromApiIsPropagated() {
        String service = "PRICE";
        productMasterRest.apiConfig = mock(Object.class);
        productMasterRest.endpoints = Map.of(service, "/mock/rest/endpoint");

        when(apiExecutionService.execute(
                any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(Mono.error(new RuntimeException("API Error")));

        Mono<ResponseEntity<Object>> response = apiExecutionService.execute(
                productMasterRest.apiConfig,
                "/mock/rest/endpoint",
                HttpMethod.GET,
                null,
                Object.class,
                null,
                new LinkedMultiValueMap<>()
        );

        StepVerifier.create(ApiResponselwrap.wrap(response))
                .expectError()
                .verify();
    }

    @Test
    void testReactiveEnginePostProcessingWithJsonAndXml() {
        JsonRawResponse jsonRaw = new JsonRawResponse("{\"dataframe\": [{\"key\": \"value\"}]}");
        XmlRawResponse xmlRaw = new XmlRawResponse("<root><element>value</element></root>");

        List<ApiResponse> responses = List.of(jsonRaw, xmlRaw);

        List<Map<String, Object>> jsonParsed = List.of(Map.of("key", "value"));
        List<Map<String, Object>> xmlParsed = List.of(Map.of("element", "value"));

        try (MockedStatic<DataframeParser> dfParser = mockStatic(DataframeParser.class);
             MockedStatic<XMLParser> xmlParser = mockStatic(XMLParser.class)) {

            dfParser.when(() -> DataframeParser.parseDataframe(anyString()))
                    .thenReturn(jsonParsed);

            xmlParser.when(() -> XMLParser.parseXMLString(anyString()))
                    .thenReturn(xmlParsed);

            Function<ApiResponse, List<Map<String, Object>>> postProcessor = raw -> {
                try {
                    if (raw instanceof XmlRawResponse) {
                        return XMLParser.parseXMLString(((XmlRawResponse) raw).getBody());
                    } else if (raw instanceof JsonRawResponse) {
                        return DataframeParser.parseDataframe(((JsonRawResponse) raw).getBody());
                    } else {
                        return List.of();
                    }
                } catch (Exception e) {
                    return List.of();
                }
            };

            Mono<List<Map<String, Object>>> result = ReactiveEngine.process(
                    responses.stream().map(Mono::just).toList(),
                    postProcessor,
                    Schedulers.immediate()
            );

            StepVerifier.create(result)
                    .assertNext(list -> {
                        assertEquals(2, list.size());
                        assertTrue(list.containsAll(jsonParsed));
                        assertTrue(list.containsAll(xmlParsed));
                    })
                    .verifyComplete();
        }
    }
}
