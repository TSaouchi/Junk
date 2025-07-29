package com.example.reactive.reactiveprog;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ReactiveEngineTest {

    @Test
    void testSimpleStringTransformation() {
        List<Mono<String>> sources = List.of(
                Mono.just("one"),
                Mono.just("two"),
                Mono.just("three")
        );

        Function<String, Integer> lengthMapper = String::length;

        StepVerifier.create(ReactiveEngine.process(sources, lengthMapper, Schedulers.boundedElastic(), "pipeline-1"))
                .expectNextMatches(resultList ->
                        resultList.size() == 3 &&
                        resultList.contains(3) && resultList.contains(5))
                .verifyComplete();
    }

    @Test
    void testMixedObjectToMapTransformation() {
        record MyDto(String id, int value) {}

        List<Mono<Object>> sources = List.of(
                Mono.just("some-string"),
                Mono.just(Map.of("key", "value")),
                Mono.just(new MyDto("id-123", 42))
        );

        Function<Object, Map<String, Object>> transformer = input -> {
            if (input instanceof String str)
                return Map.of("parsed", str.toUpperCase());
            else if (input instanceof Map<?, ?> map)
                return (Map<String, Object>) map;
            else
                return Map.of("id", ((MyDto) input).id(), "value", ((MyDto) input).value());
        };

        StepVerifier.create(ReactiveEngine.process(sources, transformer, "pipeline-mixed"))
                .expectNextMatches(resultList -> {
                    assertEquals(3, resultList.size());
                    return resultList.stream().anyMatch(map -> map.containsKey("parsed")) &&
                           resultList.stream().anyMatch(map -> map.containsKey("key")) &&
                           resultList.stream().anyMatch(map -> map.containsKey("id"));
                })
                .verifyComplete();
    }

    @Test
    void testPipelineIdFallbackToUUID() {
        List<Mono<String>> sources = List.of(Mono.just("x"));

        StepVerifier.create(ReactiveEngine.process(sources, str -> str + "-done", Schedulers.parallel(), null))
                .expectNextMatches(list -> list.size() == 1 && list.get(0).equals("x-done"))
                .verifyComplete();
    }

    @Test
    void testErrorPropagation() {
        List<Mono<String>> sources = List.of(
                Mono.just("ok"),
                Mono.just("fail")
        );

        Function<String, String> processor = str -> {
            if (str.equals("fail")) {
                throw new RuntimeException("Intentional failure");
            }
            return str.toUpperCase();
        };

        StepVerifier.create(ReactiveEngine.process(sources, processor, Schedulers.boundedElastic(), "pipeline-error"))
                .expectErrorMatches(err -> err instanceof RuntimeException &&
                        err.getMessage().equals("Intentional failure"))
                .verify();
    }

    @Test
    void testEmptySourceList() {
        List<Mono<String>> sources = List.of();
        Function<String, String> identity = s -> s;

        StepVerifier.create(ReactiveEngine.process(sources, identity, Schedulers.parallel(), "empty-pipeline"))
                .expectNextMatches(List::isEmpty)
                .verifyComplete();
    }
}
