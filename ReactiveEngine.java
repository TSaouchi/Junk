package com.example.reactive.reactiveprog;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
public class ReactiveEngine {

    public static <T, R> Mono<List<R>> process(
            List<Mono<T>> sources,
            Function<T, R> postProcessor,
            Scheduler scheduler,
            String pipelineId
    ) {
        final String piplineUUID = (pipelineId == null || pipelineId.isBlank())
            ? UUID.randomUUID().toString()
            : pipelineId;
        log.info("[{}] Received {} tasks for processing using [{}] scheduler", piplineUUID, sources.size(), scheduler);

        Instant start = Instant.now();
        List<Mono<R>> processedMonos = sources.stream()
            .map(mono -> mono
                .map(item -> {
                    log.info("[{}] Processing item: {} at {}", Thread.currentThread().getName(), item, Instant.now());
                    log.debug("[{}] Processing item: {} at {}", Thread.currentThread().getName(), item, Instant.now());
                    try {
                        R result = postProcessor.apply(item);
                        log.debug("Processed result: {}", result);
                        return result;
                    } catch (Exception e) {
                        log.error("Error during processing: {}", e.getMessage(), e);
                        throw e;
                    }
                })
                .subscribeOn(scheduler)
            )
            .toList();

        return Flux.fromIterable(processedMonos)
            .flatMap(Function.identity())
            .doOnSubscribe(sub -> {
                Instant now = Instant.now();
                log.info("Pipeline started at: {}", now);
            })
            .doOnNext(result -> log.debug("Emitted: {}", result))
            .doOnComplete(() -> log.info("All tasks processed"))
            .collectList()
            .doOnSuccess(results -> {
                Duration elapsed = Duration.between(start, Instant.now());
                log.info("[{}] Total processing time: {} ms - Collected {} processed results", piplineUUID, elapsed.toMillis(), results.size());
            });
    }

    /**
     * Default: parallel scheduler.
     */
    public static <T, R> Mono<List<R>> process(
            List<Mono<T>> sources,
            Function<T, R> postProcessor,
            String pipelineId
    ) {
        return process(sources, postProcessor, Schedulers.parallel(), pipelineId);
    }
    
    /**
     * Default: Pipeline ID random UUID.
     */
    public static <T, R> Mono<List<R>> process(
            List<Mono<T>> sources,
            Function<T, R> postProcessor,
            Scheduler scheduler
    ) {
        return process(sources, postProcessor, Schedulers.parallel(), null);
    }
}
