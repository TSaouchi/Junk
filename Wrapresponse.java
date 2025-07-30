import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import lombok.extern.slf4j.Slf4j;

// -------- Sealed Interface & Records --------

public sealed interface RawResponse permits RawResponse.JsonRawResponse, RawResponse.XmlRawResponse {

    record JsonRawResponse(Map<String, Object> body) implements RawResponse {}

    record XmlRawResponse(String body) implements RawResponse {}

    static Mono<RawResponse> wrap(Mono<? extends ResponseEntity<?>> responseMono) {
        return responseMono.map(resp -> {
            Object body = resp.getBody();
            if (body instanceof String str) {
                return new XmlRawResponse(str);
            } else if (body instanceof Map<?, ?> map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> casted = (Map<String, Object>) map;
                return new JsonRawResponse(casted);
            } else {
                throw new IllegalArgumentException("Unsupported response body type: " + body);
            }
        });
    }
}

// -------- Usage Example with ReactiveEngine --------

@Slf4j
class RawResponsePipeline {

    public static void main(String[] args) {
        // Simulated responses (replace with real ones)
        Mono<ResponseEntity<?>> responseXML1 = Mono.just(ResponseEntity.ok("<xml><val>1</val></xml>"));
        Mono<ResponseEntity<?>> responseJSON1 = Mono.just(ResponseEntity.ok(Map.of("id", 1, "name", "Item 1")));
        Mono<ResponseEntity<?>> responseXML2 = Mono.just(ResponseEntity.ok("<xml><val>2</val></xml>"));
        Mono<ResponseEntity<?>> responseJSON2 = Mono.just(ResponseEntity.ok(Map.of("id", 2, "name", "Item 2")));

        // Step 1: Raw mixed responses
        List<Mono<? extends ResponseEntity<?>>> responses = List.of(
                responseXML1, responseJSON1, responseXML2, responseJSON2
        );

        // Step 2: Wrap into RawResponse
        List<Mono<RawResponse>> rawResponses = responses.stream()
                .map(RawResponse::wrap)
                .toList();

        // Step 3: PostProcessor
        Function<RawResponse, List<Map<String, Object>>> postProcessor = raw -> {
            try {
                if (raw instanceof RawResponse.XmlRawResponse xml) {
                    return parseXml(xml.body()); // replace with real XMLParser.parseXMLString(xml.body())
                } else if (raw instanceof RawResponse.JsonRawResponse json) {
                    return parseJson(json.body()); // replace with real DataframeParser.parseDataframe(json.body())
                } else {
                    return List.of();
                }
            } catch (Exception e) {
                log.error("Post-processing failed", e);
                return List.of();
            }
        };

        // Step 4: ReactiveEngine pipeline
        ReactiveEngine.process(rawResponses, postProcessor, Schedulers.parallel(), "mixed-pipeline")
                .subscribe(finalList -> System.out.println("Final result: " + finalList));
    }

    // Dummy parsers for testing
    static List<Map<String, Object>> parseXml(String xml) {
        return List.of(Map.of("parsedXml", xml));
    }

    static List<Map<String, Object>> parseJson(Map<String, Object> json) {
        return List.of(json);
    }
}
