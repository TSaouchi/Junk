package com.example.maputils;

import java.io.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;

public final class MapUtils {

    private MapUtils() {}

    public static Map<String, Object> mapKeys(Map<String, Object> map, Map<String, String> keyMapping) {
        return mapKeys(map, keyMapping, false);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> mapKeys(Map<String, Object> map, Map<String, String> keyMapping, boolean recursive) {
        if (map == null) return null;

        Map<String, Object> result = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String newKey = keyMapping.getOrDefault(entry.getKey(), entry.getKey());
            Object value = entry.getValue();
            if (recursive && value instanceof Map<?, ?> nested && isStringKeyMap(nested)) {
                result.put(newKey, mapKeys((Map<String, Object>) nested, keyMapping, true));
            } else {
                result.put(newKey, value);
            }
        }
        return result;
    }

    public static Map<String, Object> mapValues(Map<String, Object> map, Function<Object, Object> transformer) {
        return mapValues(map, transformer, false);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> mapValues(Map<String, Object> map, Function<Object, Object> transformer, boolean recursive) {
        if (map == null) return null;

        Map<String, Object> result = new LinkedHashMap<>(map.size());
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (recursive && value instanceof Map<?, ?> subMap && isStringKeyMap(subMap)) {
                result.put(entry.getKey(), mapValues((Map<String, Object>) subMap, transformer, true));
            } else {
                result.put(entry.getKey(), transformer.apply(value));
            }
        }
        return result;
    }

    public static <K, V> Map<K, V> filterKeys(Map<K, V> map, Set<K> allowedKeys) {
        if (map == null) return null;

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (allowedKeys.contains(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static <K, V> Map<K, V> filterByPredicate(Map<K, V> map, Predicate<Map.Entry<K, V>> predicate) {
        if (map == null) return null;

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (predicate.test(entry)) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    public static <K, V> Map<K, V> mergeMaps(Map<K, V> map1, Map<K, V> map2, BinaryOperator<V> mergeFunction) {
        if (map1 == null) return map2 == null ? null : new LinkedHashMap<>(map2);
        if (map2 == null) return new LinkedHashMap<>(map1);

        Map<K, V> result = new LinkedHashMap<>(map1);
        for (Map.Entry<K, V> entry : map2.entrySet()) {
            result.merge(entry.getKey(), entry.getValue(), mergeFunction);
        }
        return result;
    }

    public static <K, V> Map<K, V> mergeMaps(Map<K, V> map1, Map<K, V> map2) {
        return mergeMaps(map1, map2, (existing, replacement) -> replacement);
    }

    public static Map<String, Object> flatten(Map<String, Object> map) {
        return flatten(map, ".");
    }

    public static Map<String, Object> flatten(Map<String, Object> map, String separator) {
        if (map == null) return null;
        Map<String, Object> result = new LinkedHashMap<>();
        flattenRecursive(map, "", separator, result);
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void flattenRecursive(Map<String, Object> map, String prefix, String separator, Map<String, Object> result) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String fullKey = prefix.isEmpty() ? entry.getKey() : prefix + separator + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> nested && isStringKeyMap(nested)) {
                flattenRecursive((Map<String, Object>) nested, fullKey, separator, result);
            } else {
                result.put(fullKey, value);
            }
        }
    }

    public static Map<String, Object> unflatten(Map<String, Object> flatMap) {
        return unflatten(flatMap, ".");
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> unflatten(Map<String, Object> flatMap, String separator) {
        if (flatMap == null) return null;

        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
            String[] keys = entry.getKey().split(Pattern.quote(separator));
            Map<String, Object> current = result;
            for (int i = 0; i < keys.length - 1; i++) {
                current = (Map<String, Object>) current.computeIfAbsent(keys[i], k -> new LinkedHashMap<>());
            }
            current.put(keys[keys.length - 1], entry.getValue());
        }
        return result;
    }

    public static Optional<Object> findValueByKey(Object data, String targetKey) {
        return Optional.ofNullable(findKeyRecursive(data, targetKey));
    }

    public static <T> Optional<T> findValueByKey(Object data, String targetKey, Class<T> type) {
        Object result = findKeyRecursive(data, targetKey);
        return (type.isInstance(result)) ? Optional.of(type.cast(result)) : Optional.empty();
    }

    private static Object findKeyRecursive(Object data, String key) {
        if (data instanceof Map<?, ?> map) {
            if (map.containsKey(key)) return map.get(key);
            for (Object val : map.values()) {
                Object found = findKeyRecursive(val, key);
                if (found != null) return found;
            }
        } else if (data instanceof Collection<?> collection) {
            for (Object item : collection) {
                Object found = findKeyRecursive(item, key);
                if (found != null) return found;
            }
        }
        return null;
    }

    public static boolean containsValueRecursive(Object data, Object target) {
        if (Objects.equals(data, target)) return true;

        if (data instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                if (containsValueRecursive(value, target)) return true;
            }
        } else if (data instanceof Collection<?> collection) {
            for (Object item : collection) {
                if (containsValueRecursive(item, target)) return true;
            }
        }
        return false;
    }

    private static boolean isStringKeyMap(Map<?, ?> map) {
        for (Object key : map.keySet()) {
            if (!(key instanceof String)) return false;
        }
        return true;
    }

    public static <T extends Serializable> T deepCopySerializable(T original) {
        if (original == null) return null;

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(bos)) {

            out.writeObject(original);
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
                 ObjectInputStream in = new ObjectInputStream(bis)) {
                @SuppressWarnings("unchecked")
                T copy = (T) in.readObject();
                return copy;
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Deep copy failed", e);
        }
    }
}
