package com.example.reactive.reactiveprog;

import com.example.reactive.reactiveprog.MultiStagePipeline;
import com.example.reactive.reactiveprog.ReactiveEngine;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;

public class PipelineExample {

        public static void main(String[] args) {

                // ---------------- Batch Mode ----------------
                // MultiStagePipeline<String> batchPipeline = new MultiStagePipeline<>();

                // batchPipeline.addChainStage(list -> {
                //         List<Mono<ResponseEntity<String>>> apiCalls = List.of(
                //                         Mono.just(ResponseEntity.ok("   apple   ")),
                //                         Mono.just(ResponseEntity.ok("banana ")),
                //                         Mono.just(ResponseEntity.ok("   cherry")));

                //         return ReactiveEngine.processAsChain(
                //                 apiCalls,
                //                 ss -> {
                //                         String s = ss.getBody();
                //                         try {
                //                                 Thread.sleep(1000);
                //                         } catch (InterruptedException e) {
                //                         }
                //                         System.out.println("Stage 1 finished for " + s + " at "
                //                                         + Instant.now());
                //                         return List.of(s.trim());
                //                 },
                //                 Schedulers.parallel(),
                //                 "batchStage1",
                //                 Duration.ofSeconds(5));
                // });

                // batchPipeline.addChainStage(list -> {
                //         List<Mono<ResponseEntity<String>>> apiCalls = List.of(
                //                         Mono.just(ResponseEntity.ok("kiwi")),
                //                         Mono.just(ResponseEntity.ok("dragon")));
                        
                //                         return ReactiveEngine.processAsChain(
                //                         apiCalls,
                //                         ss -> {
                //                                 String s = ss.getBody();
                //                                 try {
                //                                         Thread.sleep(1000);
                //                                 } catch (InterruptedException e) {
                //                                 }
                //                                 System.out.println("Stage 2 finished for " + s + " at "
                //                                                 + Instant.now());
                //                                 return List.of(s.toUpperCase());
                //                         },
                //                         Schedulers.parallel(),
                //                         "batchStage2",
                //                         Duration.ofSeconds(5));
                //                 }
                //                         );

                // batchPipeline.executeChain(List.of())
                //         .subscribe(result -> System.out.println("Batch pipeline result: " + result));

                // ---------------- Fully Reactive Streaming Mode ----------------
                MultiStagePipeline<String> streamingPipeline = new MultiStagePipeline<>();

                streamingPipeline.addStreamStage(flux -> {
                        List<Mono<ResponseEntity<String>>> apiCalls = List.of(
                                        Mono.just(ResponseEntity.ok("   apple   ")),
                                        Mono.just(ResponseEntity.ok("banana ")),
                                        Mono.just(ResponseEntity.ok("   cherry")));

                        // Merge into a single Flux<ResponseEntity<String>>
                        Flux<ResponseEntity<String>> apiFlux = Flux.merge(apiCalls);
                        return ReactiveEngine.processAsStreamReactive(
                                        apiFlux,
                                        ss -> {
                                                String s = ss.getBody();
                                                try {   
                                                        if (s.equals("   apple   ")){
                                                                Thread.sleep(10000);        
                                                        }
                                                        Thread.sleep(1000);
                                                } catch (InterruptedException e) {
                                                }
                                                System.out.println("Stage 1 finished for " + s + " at "
                                                                + Instant.now());
                                                return s.trim();
                                        },
                                        Schedulers.parallel(),
                                        "streamStage1");

                });

                streamingPipeline.addStreamStage(flux -> ReactiveEngine.processAsStreamReactive(
                                flux,
                                resp -> {
                                        String body = resp;
                                        System.out.println("Stage 2 processing " + body + " at "
                                                        + Instant.now());

                                        return body.toUpperCase();
                                },
                                Schedulers.parallel(),
                                "streamStage2"));

                streamingPipeline.executeStream(Flux.never())
                                .collectList()
                                .subscribe(result -> System.out.println("Streaming pipeline result: " + result
                                .stream().map(item -> item.toLowerCase()).toList()
                                ));

        try {
            Thread.sleep(100000000); // let async work finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        }
}












package com.example.reactive.reactiveprog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class ReactiveEngine {

    private static final Logger log = LoggerFactory.getLogger(ReactiveEngine.class);

    /** Batch mode: aggregate results from multiple asynchronous tasks */
    public static <T, R> Mono<List<R>> processAsChain(
            List<Mono<T>> sources,
            Function<T, List<R>> postProcessor,
            Scheduler scheduler,
            String pipelineId,
            Duration timeout
    ) {
        final String pipelineUUID = (pipelineId == null || pipelineId.isBlank())
                ? UUID.randomUUID().toString() : pipelineId;
        final Instant start = Instant.now();

        List<Mono<List<R>>> processedMonos = sources.stream()
                .map(mono -> mono.map(item -> {
                    final Instant processingStart = Instant.now();
                    List<R> result = postProcessor.apply(item);
                    log.debug("[{}] Processed item {} in {} ms",
                            pipelineUUID, result, Duration.between(processingStart, Instant.now()).toMillis());
                    return result;
                }).onErrorResume(e -> {
                    log.warn("[{}] Error processing item: {}", pipelineUUID, e.getMessage());
                    return Mono.just(List.of());
                }).subscribeOn(scheduler))
                .toList();

        return Flux.fromIterable(processedMonos)
                .flatMap(Function.identity())
                .flatMap(Flux::fromIterable)
                .collectList()
                .doOnSuccess(results -> log.info("[{}] Pipeline finished {} results in {} ms",
                        pipelineUUID, results.size(), Duration.between(start, Instant.now()).toMillis()))
                .timeout(timeout)
                .doOnError(e -> log.error("[{}] Pipeline error: {}", pipelineUUID, e.getMessage(), e));
    }

    /** Fully reactive streaming mode without converting Flux -> List<Mono> */
    public static <T, R> Flux<R> processAsStreamReactive(
            Flux<T> source,
            Function<T, R> postProcessor,
            Scheduler scheduler,
            String pipelineId
    ) {
        final String pipelineUUID = (pipelineId == null || pipelineId.isBlank())
                ? UUID.randomUUID().toString() : pipelineId;

        return source.flatMap(item ->
                Mono.fromCallable(() -> postProcessor.apply(item))
                        .doOnNext(r -> log.debug("[{}] Processed item {}", pipelineUUID, r))
                        .subscribeOn(scheduler)
                        .onErrorResume(e -> {
                            log.warn("[{}] Skipping failed item: {}", pipelineUUID, e.getMessage());
                            return Mono.empty();
                        })
        );
    }
}















package com.example.reactive.reactiveprog;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MultiStagePipeline<T> {

    private final List<Function<List<T>, Mono<List<T>>>> batchStages = new ArrayList<>();
    private final List<Function<Flux<T>, Flux<T>>> streamStages = new ArrayList<>();

    public MultiStagePipeline<T> addChainStage(Function<List<T>, Mono<List<T>>> stage) {
        batchStages.add(stage);
        return this;
    }

    public MultiStagePipeline<T> addStreamStage(Function<Flux<T>, Flux<T>> stage) {
        streamStages.add(stage);
        return this;
    }

    /** Execute batch stages sequentially */
    public Mono<List<T>> executeChain(List<T> initialInput) {
        final List<T> aggregated = new ArrayList<>(initialInput);
        Mono<List<T>> current = Mono.just(initialInput);

        for (Function<List<T>, Mono<List<T>>> stage : batchStages) {
            current = current.flatMap(prev ->
                    stage.apply(prev).map(output -> {
                        aggregated.addAll(output);
                        return output;
                    })
            );
        }

        return current.thenReturn(aggregated);
    }

    /** Execute streaming stages fully reactively */
    public Flux<T> executeStream(Flux<T> source) {
        Flux<T> current = source;
        for (Function<Flux<T>, Flux<T>> stage : streamStages) {
            current = stage.apply(current);
        }
        return current;
    }
}
