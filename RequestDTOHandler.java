import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class RequestDTOHandler {

    private final Map<String, List<String>> codeTypeToValues;

    public RequestDTOHandler(RequestContextDTO requestContext) {
        if (requestContext == null) throw new IllegalArgumentException();
        List<String> types = requestContext.getCodeTypeList();
        List<String> values = requestContext.getCodeValueList();
        if (types == null || values == null || types.size() != values.size())
            throw new IllegalArgumentException();
        this.codeTypeToValues = IntStream.range(0, types.size())
                .boxed()
                .collect(Collectors.groupingBy(
                        i -> types.get(i).toLowerCase(),
                        Collectors.mapping(values::get, Collectors.toUnmodifiableList())
                ));
    }

    public Stream<String> getCodeValuesByType(String codeType) {
        return codeTypeToValues.getOrDefault(codeType.toLowerCase(), Collections.emptyList()).stream();
    }

    public Stream<List<String>> sliceCodeValuesByType(String codeType, int chunkSize) {
        return slice(codeTypeToValues.getOrDefault(codeType.toLowerCase(), Collections.emptyList()), chunkSize);
    }

    public Stream<Map.Entry<String, List<String>>> getFilteredCodeValues(Predicate<String> filter) {
        Objects.requireNonNull(filter);
        return codeTypeToValues.entrySet().stream().filter(e -> filter.test(e.getKey()));
    }

    public Stream<List<String>> sliceFilteredCodeValues(Predicate<String> filter, int chunkSize) {
        Objects.requireNonNull(filter);
        return codeTypeToValues.entrySet().stream()
                .filter(e -> filter.test(e.getKey()))
                .flatMap(e -> slice(e.getValue(), chunkSize));
    }

    public Stream<String> getCodeValuesByTypeExcluding(String excludedType) {
        String excludedKey = excludedType == null ? "" : excludedType.toLowerCase();
        return codeTypeToValues.entrySet().stream()
                .filter(e -> !e.getKey().equalsIgnoreCase(excludedKey))
                .flatMap(e -> e.getValue().stream());
    }

    private static <T> Stream<List<T>> slice(List<T> list, int chunkSize) {
        if (list == null || chunkSize <= 0) throw new IllegalArgumentException();
        if (list.isEmpty()) return Stream.empty();
        int total = list.size();
        int numChunks = (int) Math.ceil((double) total / chunkSize);
        return IntStream.range(0, numChunks)
                .mapToObj(i -> list.subList(i * chunkSize, Math.min(total, (i + 1) * chunkSize)));
    }
}
