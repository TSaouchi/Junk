package com.example.reactive.reactiveprog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ReactiveEngineAndPipelineTest {

        // ---------------------- Tests for ReactiveEngine ----------------------

        @Test
        void testProcessAsChainSuccess() {
                List<Mono<ResponseEntity<String>>> sources = List.of(
                                Mono.just(ResponseEntity.ok("apple")),
                                Mono.just(ResponseEntity.ok("banana")));

                Mono<List<String>> result = ReactiveEngine.processAsChain(
                                sources,
                                resp -> List.of(resp.getBody().toUpperCase()),
                                Schedulers.parallel(),
                                "testChain",
                                Duration.ofSeconds(2));

                StepVerifier.create(result)
                                .assertNext(list -> {
                                        assertTrue(list.contains("APPLE"));
                                        assertTrue(list.contains("BANANA"));
                                })
                                .verifyComplete();
        }

        @Test
        void testProcessAsChainWithErrorRecovery() {
                List<Mono<ResponseEntity<String>>> sources = List.of(
                                Mono.error(new RuntimeException("fail")),
                                Mono.just(ResponseEntity.ok("pear")));

                Mono<List<String>> result = ReactiveEngine.processAsChain(
                                sources,
                                resp -> List.of(resp.getBody().toUpperCase()),
                                Schedulers.parallel(),
                                "testChainError",
                                Duration.ofSeconds(2));

                StepVerifier.create(result)
                                .assertNext(list -> {
                                        assertTrue(list.contains("PEAR")); // only second succeeds
                                        assertEquals(1, list.size());
                                })
                                .verifyComplete();
        }

        @Test
        @Timeout(5)
        void testProcessAsChainTimeout() {
                List<Mono<ResponseEntity<String>>> sources = List.of(
                                Mono.just(ResponseEntity.ok("slow"))
                                                .delayElement(Duration.ofSeconds(3)));

                Mono<List<String>> result = ReactiveEngine.processAsChain(
                                sources,
                                resp -> List.of(resp.getBody()),
                                Schedulers.parallel(),
                                "testTimeout",
                                Duration.ofMillis(500) // timeout too short
                );

                StepVerifier.create(result)
                                .expectError()
                                .verify();
        }

        @Test
        void testProcessAsStreamReactiveSuccess() {
        Flux<ResponseEntity<String>> flux = Flux.just(
                ResponseEntity.ok("x"),
                ResponseEntity.ok("y"));

        Flux<String> result = ReactiveEngine.processAsStreamReactive(
                flux,
                resp -> resp.getBody().toUpperCase(),
                Schedulers.parallel(),
                "testStream");

        StepVerifier.create(result.collectList())
                .assertNext(list -> {
                assertTrue(list.containsAll(List.of("X", "Y")));
                assertEquals(2, list.size());
                })
                .verifyComplete();
        }

        @Test
        void testProcessAsStreamReactiveWithError() {
                Flux<ResponseEntity<String>> flux = Flux.concat(
                                Flux.just(ResponseEntity.ok("z")),
                                Flux.error(new RuntimeException("boom")));

                Flux<String> result = ReactiveEngine.processAsStreamReactive(
                                flux,
                                resp -> resp.getBody(),
                                Schedulers.parallel(),
                                "testStreamError");

                StepVerifier.create(result)
                                .expectNext("z")
                                .verifyComplete(); // error recovered
        }

        // ---------------------- Tests for Multi-Stage Pipeline ----------------------

        @Test
        void testBatchPipelineTwoStages() {
                ReactivePipeline<String> pipeline = new ReactivePipeline<>();

                pipeline.addChainStage(list -> ReactiveEngine.processAsChain(
                                List.of(Mono.just(ResponseEntity.ok("a"))),
                                resp -> List.of(resp.getBody() + "1"),
                                Schedulers.parallel(),
                                "stage1",
                                Duration.ofSeconds(2)));

                pipeline.addChainStage(list -> ReactiveEngine.processAsChain(
                                List.of(Mono.just(ResponseEntity.ok("b"))),
                                resp -> List.of(resp.getBody() + "2"),
                                Schedulers.parallel(),
                                "stage2",
                                Duration.ofSeconds(2)));

                Mono<List<String>> result = pipeline.executeChain(List.of());

                StepVerifier.create(result)
                                .assertNext(finalList -> {
                                        assertTrue(finalList.contains("a1"));
                                        assertTrue(finalList.contains("b2"));
                                })
                                .verifyComplete();
        }

        @Test
        void testStreamPipelineTwoStages() {
                ReactivePipeline<List<String>> pipeline = new ReactivePipeline<>();

                // Stage 1: simple API call
                pipeline.addStreamStage(flux -> {
                        List<Mono<ResponseEntity<String>>> apiCalls = List.of(
                                        Mono.just(ResponseEntity.ok("apple,banana")));
                        Flux<ResponseEntity<String>> apiFlux = Flux.merge(apiCalls);

                        return ReactiveEngine.processAsStreamReactive(
                                        apiFlux,
                                        resp -> List.of(resp.getBody().split(",")),
                                        Schedulers.parallel(),
                                        "streamStage1");
                });

                // Stage 2: uppercase
                pipeline.addStreamStage(flux -> flux.map(list -> list.stream().map(String::toUpperCase).toList()));

                Flux<List<String>> result = pipeline.executeStream(Flux.just(List.of()));

                StepVerifier.create(result)
                                .assertNext(list -> {
                                        assertEquals(List.of("APPLE", "BANANA"), list);
                                })
                                .verifyComplete();
        }

        @Test
        void testEmptyStreamPipeline() {
                ReactivePipeline<String> pipeline = new ReactivePipeline<>();

                // Corrected: take flux as input and return an empty Flux<String>
                pipeline.addStreamStage(flux -> Flux.<String>empty());

                Flux<String> result = pipeline.executeStream(Flux.empty());

                StepVerifier.create(result)
                                .verifyComplete(); // nothing passes
        }

        @Test
        void testStreamPipelineErrorHandling() {
                ReactivePipeline<String> pipeline = new ReactivePipeline<>();

                pipeline.addStreamStage(flux -> flux.flatMap(item -> {
                        if (item.equals("bad")) {
                                return Flux.error(new RuntimeException("boom"));
                        }
                        return Flux.just(item + "_ok");
                }).onErrorResume(e -> Flux.just("recovered")));

                Flux<String> result = pipeline.executeStream(Flux.just("good", "bad"));

                StepVerifier.create(result)
                                .expectNext("good_ok", "recovered")
                                .verifyComplete();
        }
}
