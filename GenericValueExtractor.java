public class CoherenceFilters {

    /** Equal filter for nested fields */
    public static <T> Filter<T> eq(String fieldPath, Object value) {
        return Filters.equal(new GenericValueExtractor<>(fieldPath), value);
    }
}

public class GenericValueExtractor<T> implements ValueExtractor<T, Object>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String fieldPath;

    // Cache: Class -> FieldName -> Field
    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    public GenericValueExtractor(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    @Override
    public Object extract(T target) {
        try {
            Object current = target;
            for (String part : fieldPath.split("\\.")) {
                if (current == null) return null;

                // Check if part has index, e.g., reference[0]
                String fieldName = part;
                Integer index = null;
                if (part.contains("[") && part.endsWith("]")) {
                    int start = part.indexOf('[');
                    fieldName = part.substring(0, start);
                    index = Integer.parseInt(part.substring(start + 1, part.length() - 1));
                }

                current = getFieldValue(current, fieldName);

                if (index != null) {
                    if (current instanceof List<?> list) {
                        current = index < list.size() ? list.get(index) : null;
                    } else if (current != null && current.getClass().isArray()) {
                        Object[] array = (Object[]) current;
                        current = index < array.length ? array[index] : null;
                    } else {
                        current = null;
                    }
                }
            }
            return current;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract field: " + fieldPath, e);
        }
    }

    private Object getFieldValue(Object obj, String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Class<?> clazz = obj.getClass();
        Map<String, Field> classCache = FIELD_CACHE.computeIfAbsent(clazz, c -> new ConcurrentHashMap<>());

        Field field = classCache.computeIfAbsent(fieldName, f -> {
            try {
                Field fld = clazz.getDeclaredField(f);
                fld.setAccessible(true);
                return fld;
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        });

        return field.get(obj);
    }
}
