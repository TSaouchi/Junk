import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RequestDTOHandler {

    private final Map<String, List<String>> codeTypeToValues;

    public RequestDTOHandler(RequestContextDTO requestContext) {
        if (requestContext == null) throw new IllegalArgumentException("RequestContextDTO cannot be null");

        List<String> types = requestContext.getCodeTypeList();
        List<String> values = requestContext.getCodeValueList();

        if (types == null || values == null || types.size() != values.size())
            throw new IllegalArgumentException("Invalid DTO: codeTypeList and codeValueList must be non-null and of same size");

        this.codeTypeToValues = IntStream.range(0, types.size())
                .boxed()
                .collect(Collectors.groupingBy(
                        i -> types.get(i).toLowerCase(),
                        Collectors.mapping(values::get, Collectors.toUnmodifiableList())
                ));
    }

    public List<String> getCodeValuesByType(String codeType) {
        return codeTypeToValues.getOrDefault(codeType.toLowerCase(), Collections.emptyList());
    }

    public List<List<String>> sliceCodeValuesByType(String codeType, int chunkSize) {
        return sliceList(getCodeValuesByType(codeType), chunkSize);
    }

    public Map<String, List<List<String>>> sliceFilteredCodeValues(Predicate<String> filter, int chunkSize) {
        if (chunkSize <= 0) throw new IllegalArgumentException("chunkSize must be greater than 0");
        Objects.requireNonNull(filter, "Predicate filter cannot be null");

        return codeTypeToValues.entrySet().stream()
                .filter(entry -> filter.test(entry.getKey()))
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> sliceList(entry.getValue(), chunkSize)
                ));
    }

    public List<String> getCodeValuesByTypeExcluding(String excludedType) {
        String excludedKey = excludedType == null ? "" : excludedType.toLowerCase();

        return codeTypeToValues.entrySet().stream()
                .filter(entry -> !entry.getKey().equalsIgnoreCase(excludedKey))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toUnmodifiableList());
    }

    private static <T> List<List<T>> sliceList(List<T> list, int chunkSize) {
        if (list == null || chunkSize <= 0) throw new IllegalArgumentException();
        if (list.isEmpty()) return Collections.emptyList();

        int total = list.size();
        int numChunks = (int) Math.ceil((double) total / chunkSize);

        return IntStream.range(0, numChunks)
                .mapToObj(i -> list.subList(i * chunkSize, Math.min(total, (i + 1) * chunkSize)))
                .collect(Collectors.toUnmodifiableList());
    }
}
