@Override
public void run(String... args) {
    MultiStageReactivePipeline<Map<String, Object>> pipeline = new MultiStageReactivePipeline<>();

    // ---------- Stage 1 ----------
    pipeline.addStage(ignored -> {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("internal_security_id", "1");
        params.add("biz_date", "20250722");
        params.add("region", "Europe");

        Mono<ResponseEntity<Object>> response1 = apiExecutionService.execute(
            productMasterRest.apiConfig,
            productMasterRest.endpoints.get("PRICE"),
            HttpMethod.GET,
            null,
            Object.class,
            null,
            params
        );

        List<Mono<ApiResponse>> wrapped = List.of(ApiResponseWrap.wrap(response1));

        return ReactiveEngine.process(wrapped, postProcessor, "STAGE-1");
    });

    // ---------- Stage 2 ----------
    pipeline.addStage(stage1Results -> {
        Mono<List<Mono<ApiResponse>>> stage2RawResponses = Flux.fromIterable(stage1Results)
            .map(record -> {
                MultiValueMap<String, String> dynamicParams = new LinkedMultiValueMap<>();
                dynamicParams.add("internal_security_id", record.get("id").toString());
                dynamicParams.add("biz_date", "20250722");
                dynamicParams.add("region", "Europe");

                return apiExecutionService.execute(
                    productMasterRest.apiConfig,
                    productMasterRest.endpoints.get("PRICE"),
                    HttpMethod.GET,
                    null,
                    Object.class,
                    null,
                    dynamicParams
                );
            })
            .map(ApiResponseWrap::wrap)
            .collectList();

        return stage2RawResponses.flatMap(
            responses -> ReactiveEngine.process(responses, postProcessor, "STAGE-2")
        );
    });

    // ---------- Execute ----------
    pipeline.execute(List.of())
        .subscribe(finalResult -> {
            System.out.println("✅ Aggregated result size = " + finalResult.size());
            finalResult.forEach(System.out::println);
        });
}
