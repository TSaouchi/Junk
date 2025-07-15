import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.text.StringEscapeUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class XmlProductParserRecursive {

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.err.println("Usage: java XmlProductParserRecursive <path-to-xml-file>");
            System.exit(1);
        }

        String xmlFile = args[0];
        List<Map<String, Object>> products = parseFile(xmlFile);

        // Print all products and nested fields
        for (Map<String, Object> product : products) {
            System.out.println(product);
        }
    }

    public static List<Map<String, Object>> parseFile(String xmlPath) throws IOException {
        XmlMapper xmlMapper = new XmlMapper();
        JsonNode root = xmlMapper.readTree(new File(xmlPath));

        Optional<String> embeddedXml = findReturnNode(root);
        if (embeddedXml.isEmpty()) {
            throw new RuntimeException("No <return> node with embedded XML found.");
        }

        String cleanXml = StringEscapeUtils.unescapeXml(embeddedXml.get());
        JsonNode embeddedRoot = xmlMapper.readTree(cleanXml);

        return extractProducts(embeddedRoot);
    }

    private static Optional<String> findReturnNode(JsonNode node) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if ("return".equalsIgnoreCase(entry.getKey())) {
                    return Optional.ofNullable(entry.getValue().asText());
                }
                Optional<String> found = findReturnNode(entry.getValue());
                if (found.isPresent()) return found;
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                Optional<String> found = findReturnNode(item);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
    }

    private static List<Map<String, Object>> extractProducts(JsonNode root) {
        List<Map<String, Object>> products = new ArrayList<>();
        JsonNode productList = root.path("ProductList").path("PBOBJECT");
        if (productList.isMissingNode()) return products;

        if (productList.isArray()) {
            for (JsonNode productNode : productList) {
                products.add(extractProductRecursive(productNode));
            }
        } else {
            products.add(extractProductRecursive(productList));
        }
        return products;
    }

    private static Map<String, Object> extractProductRecursive(JsonNode pbobject) {
        Map<String, Object> productMap = new LinkedHashMap<>();
        JsonNode columns = pbobject.path("COLUMN");

        if (columns.isArray()) {
            for (JsonNode col : columns) {
                String key = col.get("@NAME").asText();
                Object val = parseNodeValue(col);
                productMap.put(key, val);
            }
        } else if (columns.isObject()) {
            String key = columns.get("@NAME").asText();
            Object val = parseNodeValue(columns);
            productMap.put(key, val);
        }
        return productMap;
    }

    private static Object parseNodeValue(JsonNode node) {
        // If this node contains nested COLUMN(s), parse them recursively
        if (node.has("COLUMN")) {
            // Nested structure
            return extractProductRecursive(node);
        }

        // If node has other child objects (excluding attributes), parse them as map
        if (node.isObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String key = entry.getKey();
                if (!key.startsWith("@")) { // Skip attributes
                    map.put(key, parseNodeValue(entry.getValue()));
                }
            }
            if (!map.isEmpty()) {
                return map;
            }
        }

        // Otherwise return text content (or null if none)
        return node.textValue();
    }
}


<dependencies>
    <dependency>
        <groupId>com.fasterxml.jackson.dataformat</groupId>
        <artifactId>jackson-dataformat-xml</artifactId>
        <version>2.17.0</version>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.17.0</version>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-text</artifactId>
        <version>1.11.0</version>
    </dependency>
</dependencies>
