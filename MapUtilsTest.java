package com.example.maputils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.*;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class MapUtilsTest {

    private Map<String, Object> testMap;
    private Map<String, Object> nestedMap;

    @BeforeEach
    void setUp() {
        testMap = new LinkedHashMap<>();
        testMap.put("a", 1);
        testMap.put("b", 2);
        testMap.put("c", 3);

        nestedMap = new LinkedHashMap<>();
        nestedMap.put("level1", Map.of("level2", Map.of("target", 123, "other", "value")));
        nestedMap.put("simple", "test");
        nestedMap.put("target2", 10);
    }

    @Test
    void testRenameKeys() {
        Map<String, String> rename = Map.of("a", "x", "b", "y");
        Map<String, Object> result = MapUtils.mapKeys(testMap, rename);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("x", 1);
        expected.put("y", 2);
        expected.put("c", 3);

        assertEquals(expected, result);
        // Original map should be unchanged
        assertTrue(testMap.containsKey("a"));
    }

    @Test
    void testRenameKeysRecursive() {
        Map<String, String> rename = Map.of("level2", "level2Renamed", "simple", "simpleRenamed");
        Map<String, Object> result = MapUtils.mapKeys(nestedMap, rename, true);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("level1", Map.of("level2Renamed", Map.of("target", 123, "other", "value")));
        expected.put("simpleRenamed", "test");
        expected.put("target2", 10);

        assertEquals(expected, result);
    }

    @Test
    void testTransformValues() {
        Function<Object, Object> doubler = val -> (Integer) val * 2;
        Map<String, Object> result = MapUtils.mapValues(testMap, doubler);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("a", 2);
        expected.put("b", 4);
        expected.put("c", 6);

        assertEquals(expected, result);
    }

    @Test
    void testTransformValuesRecursive() {
        Function<Object, Object> doubler = val -> {
            if (val instanceof Integer i)
                return i * 2;
            return val;
        };
        Map<String, Object> result = MapUtils.mapValues(nestedMap, doubler, true);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("level1", Map.of("level2", Map.of("target", 246, "other", "value")));
        expected.put("simple", "test");
        expected.put("target2", 20);

        assertEquals(expected, result);
    }

    @Test
    void testTransformValuesWithStringUppercase() {
        Map<String, Object> stringMap = Map.of("greeting", "hello", "name", "world");
        Map<String, Object> result = MapUtils.mapValues(stringMap,
                val -> val instanceof String ? ((String) val).toUpperCase() : val);

        assertEquals(Map.of("greeting", "HELLO", "name", "WORLD"), result);
    }

    @Test
    void testFindValueByKey() {
        Optional<Object> result = MapUtils.findValueByKey(nestedMap, "target");
        assertTrue(result.isPresent());
        assertEquals(123, result.get());

        Optional<Object> notFound = MapUtils.findValueByKey(nestedMap, "nonexistent");
        assertFalse(notFound.isPresent());
    }

    @Test
    void testFindValueByKeyTypeSafe() {
        Optional<Integer> result = MapUtils.findValueByKey(nestedMap, "target", Integer.class);
        assertTrue(result.isPresent());
        assertEquals(123, result.get());

        Optional<String> wrongType = MapUtils.findValueByKey(nestedMap, "target", String.class);
        assertFalse(wrongType.isPresent());
    }

    @Test
    void testContainsValueRecursive() {
        assertTrue(MapUtils.containsValueRecursive(nestedMap, 123));
        assertTrue(MapUtils.containsValueRecursive(nestedMap, "test"));
        assertTrue(MapUtils.containsValueRecursive(nestedMap, "value"));
        assertFalse(MapUtils.containsValueRecursive(nestedMap, 999));
    }

    @Test
    void testFlatten() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a", 1);
        input.put("b", Map.of("c", 2, "d", Map.of("e", 3)));

        Map<String, Object> result = MapUtils.flatten(input);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("a", 1);
        expected.put("b.c", 2);
        expected.put("b.d.e", 3);

        assertEquals(expected, result);
    }

    @Test
    void testFlattenWithCustomSeparator() {
        Map<String, Object> input = Map.of("a", Map.of("b", 1));
        Map<String, Object> result = MapUtils.flatten(input, "_");
        assertEquals(Map.of("a_b", 1), result);
    }

    @Test
    void testUnflatten() {
        Map<String, Object> flatMap = new LinkedHashMap<>();
        flatMap.put("a", 1);
        flatMap.put("b.c", 2);
        flatMap.put("b.d.e", 3);

        Map<String, Object> result = MapUtils.unflatten(flatMap);

        // Verify structure
        assertEquals(1, result.get("a"));
        assertTrue(result.get("b") instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> bLevel = (Map<String, Object>) result.get("b");
        assertEquals(2, bLevel.get("c"));
        assertTrue(bLevel.get("d") instanceof Map);
    }

    @Test
    void testFilterKeys() {
        Set<String> keysToKeep = Set.of("a", "c");
        Map<String, Object> result = MapUtils.filterKeys(testMap, keysToKeep);

        assertEquals(Map.of("a", 1, "c", 3), result);
    }

    @Test
    void testFilterByPredicate() {
        Map<String, Object> result = MapUtils.filterByPredicate(testMap,
                entry -> (Integer) entry.getValue() > 1);

        assertEquals(Map.of("b", 2, "c", 3), result);
    }

    @Test
    void testMergeMaps() {
        Map<String, Object> map1 = Map.of("a", 1, "b", 2);
        Map<String, Object> map2 = Map.of("b", 3, "c", 4);

        // Test default merge (last wins)
        Map<String, Object> result = MapUtils.mergeMaps(map1, map2);
        assertEquals(Map.of("a", 1, "b", 3, "c", 4), result);

        // Test custom merge function (sum values)
        Map<String, Object> sumResult = MapUtils.mergeMaps(map1, map2,
                (existing, replacement) -> (Integer) existing + (Integer) replacement);
        assertEquals(Map.of("a", 1, "b", 5, "c", 4), sumResult);
    }

    @Test
    void testMergeMapsWithNulls() {
        Map<String, Object> map1 = Map.of("a", 1);

        assertEquals(map1, MapUtils.mergeMaps(null, map1));
        assertEquals(map1, MapUtils.mergeMaps(map1, null));
        assertNull(MapUtils.mergeMaps(null, null));
    }

    @Test
    void testDeepCopySerializable() {
        // Create a serializable map
        LinkedHashMap<String, Object> original = new LinkedHashMap<>();
        original.put("a", 1);
        original.put("b", "test");
        original.put("c", Arrays.asList(1, 2, 3));

        LinkedHashMap<String, Object> copy = MapUtils.deepCopySerializable(original);

        assertEquals(original, copy);
        assertNotSame(original, copy);

        // Verify deep copy by modifying original
        original.put("d", 4);
        assertFalse(copy.containsKey("d"));
    }

    @Test
    void testDeepCopySerializableWithNull() {
        assertNull(MapUtils.deepCopySerializable(null));
    }

    @Test
    void testEmptyMaps() {
        Map<String, Object> empty = new LinkedHashMap<>();

        assertEquals(empty, MapUtils.mapKeys(empty, Map.of("a", "b")));
        assertEquals(empty, MapUtils.mapValues(empty, val -> val));
        assertEquals(empty, MapUtils.filterKeys(empty, Set.of("a")));
        assertEquals(empty, MapUtils.flatten(empty));
        assertEquals(empty, MapUtils.unflatten(empty));
        assertFalse(MapUtils.findValueByKey(empty, "key").isPresent());
        assertFalse(MapUtils.containsValueRecursive(empty, "value"));
    }

    @Test
    void testComplexNestedStructure() {
        Map<String, Object> complex = new LinkedHashMap<>();
        complex.put("users", Arrays.asList(
                Map.of("id", 1, "profile", Map.of("name", "Alice", "settings", Map.of("theme", "dark"))),
                Map.of("id", 2, "profile", Map.of("name", "Bob"))));

        // Test recursive search in lists
        Optional<Object> theme = MapUtils.findValueByKey(complex, "theme");
        assertTrue(theme.isPresent());
        assertEquals("dark", theme.get());

        // Test recursive value search in lists
        assertTrue(MapUtils.containsValueRecursive(complex, "Alice"));
        assertTrue(MapUtils.containsValueRecursive(complex, "dark"));
        assertFalse(MapUtils.containsValueRecursive(complex, "nonexistent"));
    }
}
