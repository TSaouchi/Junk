import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class MultiStageReactivePipeline<T> {

    private final List<Function<List<T>, Mono<List<T>>>> stages = new ArrayList<>();

    public MultiStageReactivePipeline<T> addStage(Function<List<T>, Mono<List<T>>> stageFn) {
        stages.add(stageFn);
        return this;
    }

    public Mono<List<T>> execute(List<T> initialInput) {
        List<T> aggregated = new ArrayList<>(initialInput);

        Mono<List<T>> current = Mono.just(initialInput);

        for (Function<List<T>, Mono<List<T>>> stage : stages) {
            current = current.flatMap(previousOutput ->
                stage.apply(previousOutput)
                    .map(newOutput -> {
                        aggregated.addAll(newOutput);
                        return newOutput; // feed into next stage
                    })
            );
        }

        return current.thenReturn(aggregated);
    }
}
