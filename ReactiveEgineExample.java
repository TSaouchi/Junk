package com.example.reactive.reactiveprog;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class RunService {

    public static Mono<List<Map<String, Object>>> run(Scheduler scheduler){

        List<Mono<? extends ResponseEntity<?>>> mixedResponses = List.of(
            apiCall1ReturningMonoString(),
            apiCall2ReturningMonoMap(),
            apiCall3ReturningMonoPojo()
        );

        ObjectMapper mapper = new ObjectMapper();

        Function<Object, Map<String, Object>> postProcessor = obj -> {
            if (obj instanceof String xml) {
                return XMLParser.parseXMLString(xml);
            } else if (obj instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            } else {
                return mapper.convertValue(obj, new TypeReference<>() {});
            }
        };


        List<Mono<Object>> bodyMonos = mixedResponses.stream()
            .map(mono -> mono.map(response -> (Object) response.getBody()))
            .toList();

        Mono<List<Map<String, Object>>> result = ReactiveEngine.process(
            bodyMonos,
            postProcessor,
            scheduler //Schedulers.boundedElastic()
        );

        // Subscribe to see result (in real app, don't block)
        // result.subscribe(finalList -> {
        //     System.out.println("Final processed list: " + finalList);
        // });
        
        return result;
    }

    // Stubs for demo
    static Mono<ResponseEntity<String>> apiCall1ReturningMonoString() {
        return Mono.just(ResponseEntity.ok("<root><id>1</id></root>"))
               .delayElement(Duration.ofSeconds(2));
    }

    static Mono<ResponseEntity<Map<String, Object>>> apiCall2ReturningMonoMap() {
        return Mono.just(ResponseEntity.ok(Map.of("id", 2)));
    }

    static Mono<ResponseEntity<MyDto>> apiCall3ReturningMonoPojo() {
        return Mono.just(ResponseEntity.ok(new MyDto("3", "example")));
    }

    static class MyDto {
        public String id;
        public String name;
        public MyDto(String id, String name) { this.id = id; this.name = name; }
    }

    static class XMLParser {
        public static Map<String, Object> parseXMLString(String xml) {
            // Dummy impl
            return Map.of("xml", xml);
        }
    }
}



package com.example.reactive.reactiveprog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
public class ReactivePipelineController {

    @GetMapping("/process")
    public Mono<List<Map<String, Object>>> processResponses(
            @RequestParam(defaultValue = "elastic") String mode
    ) {
        Scheduler scheduler = mode.equalsIgnoreCase("parallel")
            ? Schedulers.parallel()
            : Schedulers.boundedElastic();
        return RunService.run(scheduler);
    }
}
