class GenericMapper {

    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    // ---- Core: JsonNode → Target ----
    public static <T> T create(JsonNode node, Class<T> targetClass) {
        if (node == null) return null;
        return mapper.convertValue(node, targetClass);
    }

    // ---- Overload: Object → Target ----
    public static <T> T create(Object source, Class<T> targetClass) {
        if (source == null) return null;
        JsonNode node = mapper.valueToTree(source);
        return create(node, targetClass);
    }

    // ---- Overload: Map<?,?> → Target ----
    public static <T> T create(Map<?, ?> map, Class<T> targetClass) {
        if (map == null) return null;
        JsonNode node = mapper.valueToTree(map);
        return create(node, targetClass);
    }

    // ---- Generic Aggregation ----
    public static <S, T> T createWithAggregation(
            S source,
            Class<T> targetClass,
            BiConsumer<S, T> aggregationRule
    ) {
        T target = create(source, targetClass);
        if (target != null && aggregationRule != null) {
            aggregationRule.accept(source, target);
        }
        return target;
    }
}
