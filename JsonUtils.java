package com.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public final class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private JsonUtils() {
        // Prevent instantiation
    }

    /**
     * Default: Convert Map<String, Object> to JsonNode
     */
    public static JsonNode mapToJsonNode(Map<String, Object> map) {
        return objectMapper.valueToTree(map);
    }

    /**
     * Default: Convert JsonNode to Map<String, Object>
     */
    public static Map<String, Object> jsonNodeToMap(JsonNode node) {
        return objectMapper.convertValue(node, new TypeReference<>() {});
    }

    // ==== Optional advanced methods ====

    /**
     * Generic: Convert JsonNode to any type
     */
    public static <T> T jsonNodeToObject(JsonNode node, Class<T> clazz) {
        return objectMapper.convertValue(node, clazz);
    }

    public static <T> T jsonNodeToObject(JsonNode node, TypeReference<T> typeRef) {
        return objectMapper.convertValue(node, typeRef);
    }

    /**
     * Convert Object to JsonNode
     */
    public static JsonNode objectToJsonNode(Object object) {
        return objectMapper.valueToTree(object);
    }

    /**
     * Convert Object to pretty JSON String
     */
    public static String objectToPrettyJson(Object object) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("JSON serialization failed", e);
        }
    }

    /**
     * Parse JSON String to Map<String, Object>
     */
    public static Map<String, Object> jsonStringToMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON string", e);
        }
    }

    /**
     * Pretty prints any object (Map, POJO, etc.) as JSON string.
     */
    public static String toPrettyJson(Object value) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
}

-------------------------------------------------------
  package com.example;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

public class JsonUtilsTest {

    // === Sample POJO for tests ===
    public record Person(String name, int age, List<String> skills) {}

    @Test
    @DisplayName("Should convert Map<String, Object> to JsonNode and back")
    void testMapToJsonNodeAndBack() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("name", "Alice");
        nested.put("age", 30);

        Map<String, Object> root = new HashMap<>();
        root.put("user", nested);
        root.put("active", true);

        JsonNode jsonNode = JsonUtils.mapToJsonNode(root);
        assertThat(jsonNode.get("user").get("name").asText()).isEqualTo("Alice");
        assertThat(jsonNode.get("active").asBoolean()).isTrue();

        Map<String, Object> resultMap = JsonUtils.jsonNodeToMap(jsonNode);
        assertThat(resultMap).containsKeys("user", "active");

        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = (Map<String, Object>) resultMap.get("user");

        assertThat(userMap)
            .isNotNull()
            .containsEntry("name", "Alice")
            .containsEntry("age", 30);
    }

    @Test
    @DisplayName("Should convert POJO to JsonNode and back")
    void testPojoToJsonNodeAndBack() {
        Person person = new Person("Bob", 40, List.of("Java", "Kotlin"));
        JsonNode jsonNode = JsonUtils.objectToJsonNode(person);

        assertThat(jsonNode.get("name").asText()).isEqualTo("Bob");
        assertThat(jsonNode.get("age").asInt()).isEqualTo(40);
        assertThat(jsonNode.get("skills").isArray()).isTrue();

        Person restored = JsonUtils.jsonNodeToObject(jsonNode, Person.class);
        assertThat(restored.name()).isEqualTo("Bob");
        assertThat(restored.age()).isEqualTo(40);
        assertThat(restored.skills()).containsExactly("Java", "Kotlin");
    }

    @Test
    @DisplayName("Should convert valid JSON string to Map")
    void testJsonStringToMap() {
        String json = "{\"language\": \"Java\", \"version\": 21}";
        Map<String, Object> map = JsonUtils.jsonStringToMap(json);

        assertThat(map).containsEntry("language", "Java");
        assertThat(map.get("version")).isEqualTo(21);
    }

    @Test
    @DisplayName("Should throw exception on invalid JSON string")
    void testInvalidJsonStringToMap() {
        String invalidJson = "{invalid json}";

        assertThatThrownBy(() -> JsonUtils.jsonStringToMap(invalidJson))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid JSON string");
    }

    @Test
    @DisplayName("Should pretty print complex object")
    void testPrettyPrint() {
        Person person = new Person("Charlie", 25, List.of("C++", "Python"));

        String prettyJson = JsonUtils.objectToPrettyJson(person);
        assertThat(prettyJson)
                .contains("\"name\" : \"Charlie\"")
                .contains("\"age\" : 25")
                .contains("\"skills\" : [ \"C++\", \"Python\" ]");
    }

    @Test
    @DisplayName("Should handle generic TypeReference conversion")
    void testGenericTypeConversion() {
        String json = "{\"name\": \"Dana\", \"roles\": [\"admin\", \"editor\"]}";
        JsonNode node = JsonUtils.objectToJsonNode(JsonUtils.jsonStringToMap(json));

        Map<String, Object> map = JsonUtils.jsonNodeToObject(node, new com.fasterxml.jackson.core.type.TypeReference<>() {});
        assertThat(map).containsKeys("name", "roles");
        assertThat(map.get("roles")).isInstanceOf(List.class);
    }
}
