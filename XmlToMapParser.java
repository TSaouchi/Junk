import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.util.*;

/**
 * Self-contained SAX-based parser to convert a structured XML into a nested Map<String, Object>
 */
public class XmlToMapParser {

    public static void main(String[] args) {
        try {
            File xmlFile = new File("your_large_file.xml");
            Map<String, Object> parsedMap = parseXml(xmlFile);

            // Optional: Print root keys
            System.out.println("Parsed top-level keys: " + parsedMap.keySet());

            // Optional: Pretty print (you can use Gson or Jackson for true JSON-like printing)
            printMap(parsedMap, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Object> parseXml(File xmlFile) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();

        PbObjectHandler handler = new PbObjectHandler();
        saxParser.parse(xmlFile, handler);
        return handler.getParsedData();
    }

    private static void printMap(Map<String, Object> map, int indent) {
        String prefix = " ".repeat(indent * 2);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            System.out.print(prefix + entry.getKey() + ": ");
            if (entry.getValue() instanceof Map<?, ?> subMap) {
                System.out.println();
                printMap((Map<String, Object>) subMap, indent + 1);
            } else if (entry.getValue() instanceof List<?> list) {
                System.out.println("[");
                for (Object item : list) {
                    if (item instanceof Map<?, ?> itemMap) {
                        printMap((Map<String, Object>) itemMap, indent + 2);
                    } else {
                        System.out.println(" ".repeat((indent + 2) * 2) + item);
                    }
                }
                System.out.println(prefix + "]");
            } else {
                System.out.println(entry.getValue());
            }
        }
    }

    // Inner class implementing SAX handler logic
    static class PbObjectHandler extends DefaultHandler {
        private final Deque<Map<String, Object>> objectStack = new ArrayDeque<>();
        private final Deque<List<Map<String, Object>>> listStack = new ArrayDeque<>();
        private final Deque<String> elementStack = new ArrayDeque<>();
        private StringBuilder currentText = new StringBuilder();
        private Map<String, Object> parsedData;
        private Map<String, String> currentAttributes;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            currentText = new StringBuilder();

            switch (qName) {
                case "PBOBJECT" -> {
                    Map<String, Object> newObj = new HashMap<>();
                    String name = attributes.getValue("NAME");

                    if (!objectStack.isEmpty()) {
                        if (name != null) {
                            objectStack.peek().put(name, newObj);
                        }
                    }
                    objectStack.push(newObj);
                }
                case "PBOBJECTLIST" -> {
                    List<Map<String, Object>> newList = new ArrayList<>();
                    String name = attributes.getValue("NAME");

                    if (!objectStack.isEmpty() && name != null) {
                        objectStack.peek().put(name, newList);
                    }

                    listStack.push(newList);
                }
                case "COLUMN" -> {
                    currentAttributes = new HashMap<>();
                    for (int i = 0; i < attributes.getLength(); i++) {
                        currentAttributes.put(attributes.getQName(i), attributes.getValue(i));
                    }
                }
            }

            elementStack.push(qName);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String currentElement = elementStack.pop();

            if (qName.equals("COLUMN")) {
                if (objectStack.isEmpty()) return;

                Map<String, Object> currentObject = objectStack.peek();
                String columnName = currentAttributes.get("NAME");
                String text = currentText.toString().trim();
                Object value = parseValue(currentAttributes.get("TYPE"), text);
                currentObject.put(columnName, value);

            } else if (qName.equals("PBOBJECT")) {
                Map<String, Object> obj = objectStack.pop();

                // If it's the root object
                if (objectStack.isEmpty()) {
                    parsedData = obj;
                } else if (!listStack.isEmpty()) {
                    List<Map<String, Object>> list = listStack.peek();
                    list.add(obj);
                }

            } else if (qName.equals("PBOBJECTLIST")) {
                listStack.pop();
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            currentText.append(ch, start, length);
        }

        public Map<String, Object> getParsedData() {
            return parsedData;
        }

        private Object parseValue(String type, String value) {
            if (value == null || value.equalsIgnoreCase("null") || value.isEmpty()) return null;

            try {
                return switch (type) {
                    case "Integer" -> Integer.parseInt(value);
                    case "Double" -> Double.parseDouble(value);
                    case "YesNo" -> value.equalsIgnoreCase("Yes");
                    case "Calendar" -> value; // You can use SimpleDateFormat to convert if needed
                    default -> value;
                };
            } catch (Exception e) {
                return value; // Fallback to raw string if parsing fails
            }
        }
    }
}
