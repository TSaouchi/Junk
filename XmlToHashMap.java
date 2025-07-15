import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class XmlToHashMap {

    public static void main(String[] args) {
        try {
            // 1. Load and parse XML file to JsonNode
            XmlMapper xmlMapper = new XmlMapper();
            JsonNode root = xmlMapper.readTree(new File("path/to/your/file.xml"));

            // 2. Convert JsonNode to Map
            Map<String, Object> resultMap = jsonNodeToMap(root);

            // 3. Output result
            System.out.println(resultMap);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Recursive method to convert JsonNode to HashMap
    private static Map<String, Object> jsonNodeToMap(JsonNode node) {
        Map<String, Object> result = new HashMap<>();

        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();

            if (value.isObject()) {
                result.put(entry.getKey(), jsonNodeToMap(value)); // Recursive call
            } else if (value.isArray()) {
                List<Object> list = new ArrayList<>();
                for (JsonNode arrayElement : value) {
                    if (arrayElement.isObject()) {
                        list.add(jsonNodeToMap(arrayElement));
                    } else {
                        list.add(arrayElement.asText());
                    }
                }
                result.put(entry.getKey(), list);
            } else {
                result.put(entry.getKey(), value.asText());
            }
        }

        return result;
    }
}
