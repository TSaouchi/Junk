pipeline.addStage(stage2Results -> {
    return Flux.fromIterable(stage2Results)
        .index() // provides (index, ServiceData)
        .map(tuple -> {
            long idx = tuple.getT1() + 1; // 1-based index
            ServiceData serviceData = tuple.getT2();

            String prefix = "prefix" + idx;
            Map<String, Object> flattened = new HashMap<>();

            // recursive flatten function with prefix
            Function<Object, Map<String,Object>> flattenWithPrefix = obj -> {
                if (obj == null) return Map.of();
                Map<String,Object> flatMap = new HashMap<>();
                Map<String,Object> map = objectMapper.convertValue(obj, new TypeReference<Map<String,Object>>() {});
                for (Map.Entry<String,Object> entry : map.entrySet()) {
                    if (entry.getValue() == null) continue;
                    if (entry.getValue() instanceof Map || entry.getValue() instanceof List) {
                        flatMap.putAll(flattenWithPrefix.apply(entry.getValue()));
                    } else {
                        flatMap.put(prefix + "." + entry.getKey(), entry.getValue());
                    }
                }
                return flatMap;
            };

            // flatten base, asset, reference
            flattened.putAll(flattenWithPrefix.apply(serviceData.getBase()));
            flattened.putAll(flattenWithPrefix.apply(serviceData.getAsset()));

            if (serviceData.getReference() != null) {
                for (Object ref : serviceData.getReference()) {
                    flattened.putAll(flattenWithPrefix.apply(ref));
                }
            }

            return flattened;
        })
        .reduce(new HashMap<String,Object>(), (acc, map) -> {
            acc.putAll(map); // merge all individual maps into a single map
            return acc;
        }); // Mono<Map<String,Object>>
});
