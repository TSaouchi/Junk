package com.example.maputils;

// MapUtils.java
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Map utilities that complement standard Java libraries.
 */
public class MapUtils {

    /**
     * Rename keys using standard stream operations.
     */
    public static Map<String, Object> mapKeys(Map<String, Object> map, Map<String, String> keyMapping) {
        return mapKeys(map, keyMapping, false);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> mapKeys(Map<String, Object> map, Map<String, String> keyMapping,
            boolean recursive) {
        if (map == null)
            return null;

        if (!recursive) {
            // Just rename keys at top level, values unchanged
            return map.entrySet().stream()
                    .collect(Collectors.toMap(
                            entry -> keyMapping.getOrDefault(entry.getKey(), entry.getKey()),
                            Map.Entry::getValue,
                            (existing, replacement) -> replacement,
                            LinkedHashMap::new));
        }

        // Recursive rename keys in nested maps
        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> keyMapping.getOrDefault(entry.getKey(), entry.getKey()),
                        entry -> {
                            Object value = entry.getValue();
                            if (value instanceof Map<?, ?> nestedMap && isStringKeyMap(nestedMap)) {
                                return mapKeys((Map<String, Object>) nestedMap, keyMapping, true);
                            }
                            return value;
                        },
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new));
    }

    /**
     * Transform values using standard stream operations.
     */
    public static Map<String, Object> mapValues(Map<String, Object> map, Function<Object, Object> transformer) {
        return mapValues(map, transformer, false);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> mapValues(Map<String, Object> map, Function<Object, Object> transformer,
            boolean recursive) {
        if (map == null)
            return null;

        return map.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                            Object value = entry.getValue();

                            if (recursive && value instanceof Map<?, ?> subMap && isStringKeyMap(subMap)) {
                                return mapValues((Map<String, Object>) subMap, transformer, true);
                            }

                            return transformer.apply(value);
                        },
                        (existing, replacement) -> replacement,
                        LinkedHashMap::new));
    }

    /**
     * Filter by keys - uses standard stream filtering.
     */
    public static <K, V> Map<K, V> filterKeys(Map<K, V> map, Set<K> allowedKeys) {
        return map == null ? null
                : map.entrySet().stream()
                        .filter(entry -> allowedKeys.contains(entry.getKey()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (existing, replacement) -> replacement,
                                LinkedHashMap::new));
    }

    /**
     * Filter by predicate - uses standard stream filtering.
     */
    public static <K, V> Map<K, V> filterByPredicate(Map<K, V> map, Predicate<Map.Entry<K, V>> predicate) {
        return map == null ? null
                : map.entrySet().stream()
                        .filter(predicate)
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (existing, replacement) -> replacement,
                                LinkedHashMap::new));
    }

    /**
     * Merge maps using standard Map.merge() functionality.
     */
    public static <K, V> Map<K, V> mergeMaps(Map<K, V> map1, Map<K, V> map2, BinaryOperator<V> mergeFunction) {
        if (map1 == null)
            return map2 == null ? null : new LinkedHashMap<>(map2);
        if (map2 == null)
            return new LinkedHashMap<>(map1);

        Map<K, V> result = new LinkedHashMap<>(map1);
        map2.forEach((key, value) -> result.merge(key, value, mergeFunction));
        return result;
    }

    /**
     * Simple merge with "last wins" strategy.
     */
    public static <K, V> Map<K, V> mergeMaps(Map<K, V> map1, Map<K, V> map2) {
        return mergeMaps(map1, map2, (existing, replacement) -> replacement);
    }

    /**
     * Flatten nested maps - this is genuinely useful custom functionality.
     */
    public static Map<String, Object> flatten(Map<String, Object> map, String separator) {
        if (map == null)
            return null;

        Map<String, Object> result = new LinkedHashMap<>();
        flattenRecursive(map, "", separator, result);
        return result;
    }

    public static Map<String, Object> flatten(Map<String, Object> map) {
        return flatten(map, ".");
    }

    private static void flattenRecursive(Map<String, Object> map, String prefix,
            String separator, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            String fullKey = prefix.isEmpty() ? key : prefix + separator + key;

            if (value instanceof Map<?, ?> nestedMap && isStringKeyMap(nestedMap)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stringKeyMap = (Map<String, Object>) nestedMap;
                flattenRecursive(stringKeyMap, fullKey, separator, result);
            } else {
                result.put(fullKey, value);
            }
        }
    }

    /**
     * Unflatten a map - reverse of flatten operation.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> unflatten(Map<String, Object> flatMap, String separator) {
        if (flatMap == null)
            return null;

        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
            String[] keyParts = entry.getKey().split("\\Q" + separator + "\\E");
            Map<String, Object> current = result;

            for (int i = 0; i < keyParts.length - 1; i++) {
                current = (Map<String, Object>) current.computeIfAbsent(
                        keyParts[i], k -> new LinkedHashMap<String, Object>());
            }
            current.put(keyParts[keyParts.length - 1], entry.getValue());
        }

        return result;
    }

    public static Map<String, Object> unflatten(Map<String, Object> flatMap) {
        return unflatten(flatMap, ".");
    }

    /**
     * Recursive key search - custom functionality for nested structures.
     */
    public static Optional<Object> findValueByKey(Object data, String targetKey) {
        return Optional.ofNullable(findKeyRecursive(data, targetKey));
    }

    public static <T> Optional<T> findValueByKey(Object data, String targetKey, Class<T> expectedType) {
        Object result = findKeyRecursive(data, targetKey);
        if (result != null && expectedType.isInstance(result)) {
            return Optional.of(expectedType.cast(result));
        }
        return Optional.empty();
    }

    private static Object findKeyRecursive(Object data, String key) {
        if (data instanceof Map<?, ?> map) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
            for (Object value : map.values()) {
                Object found = findKeyRecursive(value, key);
                if (found != null)
                    return found;
            }
        } else if (data instanceof Collection<?> collection) {
            for (Object item : collection) {
                Object found = findKeyRecursive(item, key);
                if (found != null)
                    return found;
            }
        }
        return null;
    }

    /**
     * Recursive value search - extends standard contains() to nested structures.
     */
    public static boolean containsValueRecursive(Object data, Object target) {
        if (Objects.equals(data, target))
            return true;

        if (data instanceof Map<?, ?> map) {
            return map.values().stream().anyMatch(value -> containsValueRecursive(value, target));
        } else if (data instanceof Collection<?> collection) {
            return collection.stream().anyMatch(item -> containsValueRecursive(item, target));
        }
        return false;
    }

    private static boolean isStringKeyMap(Map<?, ?> map) {
        return map.keySet().stream().allMatch(key -> key instanceof String);
    }

    public static <T extends java.io.Serializable> T deepCopySerializable(T original) {
        if (original == null)
            return null;

        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            java.io.ObjectOutputStream out = new java.io.ObjectOutputStream(bos);
            out.writeObject(original);
            out.close();

            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(bos.toByteArray());
            java.io.ObjectInputStream in = new java.io.ObjectInputStream(bis);
            @SuppressWarnings("unchecked")
            T copy = (T) in.readObject();
            in.close();
            return copy;
        } catch (Exception e) {
            throw new RuntimeException("Deep copy failed", e);
        }
    }
}
