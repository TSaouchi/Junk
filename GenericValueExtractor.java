import com.tangosol.util.ValueExtractor;

import java.lang.reflect.Field;

public class GenericValueExtractor<T> implements ValueExtractor<T, Object> {
    private final String fieldPath;

    public GenericValueExtractor(String fieldPath) {
        this.fieldPath = fieldPath;
    }

    @Override
    public Object extract(T target) {
        try {
            Object current = target;
            for (String fieldName : fieldPath.split("\\.")) {
                if (current == null) return null;
                Field field = current.getClass().getDeclaredField(fieldName);
                field.setAccessible(true);
                current = field.get(current);
            }
            return current;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract field: " + fieldPath, e);
        }
    }
}
