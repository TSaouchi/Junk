package com.example;

import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

enum Status { NOTFOUND, OK, FAILED }

@Data
public class ClientRequest {

    /** Immutable Row holding codeType, codeValue, resultName, and thread-safe status */
    @Data
    public static class Row {
        private final String codeType;
        private final String codeValue;
        private final String resultName;
        private final AtomicReference<Status> status;

        public Row(String codeType, String codeValue, String resultName, Status initialStatus) {
            this.codeType = codeType;
            this.codeValue = codeValue;
            this.resultName = resultName;
            this.status = new AtomicReference<>(initialStatus);
        }

        /** Thread-safe status update */
        public void updateStatus(Status newStatus) {
            status.set(newStatus);
        }

        /** Thread-safe status read */
        public Status getStatus() {
            return status.get();
        }
    }

    private final List<Row> rows; // maintains order for slicing
    private final Map<String, Row> rowsByCodeValue;  // fast lookup
    private final Map<String, Row> rowsByResultName; // fast lookup

    /** Constructor from parallel lists */
    public ClientRequest(List<String> codeTypeList,
                         List<String> codeValueList,
                         List<String> resultNameList,
                         Status initialStatus) {

        if (codeTypeList.size() != codeValueList.size() ||
            codeTypeList.size() != resultNameList.size()) {
            throw new IllegalArgumentException("All lists must be of the same size");
        }

        int n = codeTypeList.size();
        List<Row> tempRows = new ArrayList<>(n);
        Map<String, Row> codeValueMap = new ConcurrentHashMap<>();
        Map<String, Row> resultNameMap = new ConcurrentHashMap<>();

        for (int i = 0; i < n; i++) {
            Row row = new Row(codeTypeList.get(i), codeValueList.get(i),
                              resultNameList.get(i), initialStatus);
            tempRows.add(row);
            codeValueMap.put(row.getCodeValue(), row);
            resultNameMap.put(row.getResultName(), row);
        }

        this.rows = Collections.unmodifiableList(tempRows); // immutable list
        this.rowsByCodeValue = codeValueMap;
        this.rowsByResultName = resultNameMap;
    }

    /** Slice rows into chunks by index */
    public List<ClientRequest> slice(int chunkSize) {
        List<ClientRequest> slices = new ArrayList<>();
        int total = rows.size();
        for (int i = 0; i < total; i += chunkSize) {
            int end = Math.min(i + chunkSize, total);
            List<Row> subRows = rows.subList(i, end);
            slices.add(new ClientRequest(subRows));
        }
        return slices;
    }

    /** Private constructor for slices (shares same rows and maps) */
    private ClientRequest(List<Row> rows) {
        this.rows = Collections.unmodifiableList(rows);
        Map<String, Row> codeValueMap = new ConcurrentHashMap<>();
        Map<String, Row> resultNameMap = new ConcurrentHashMap<>();
        for (Row row : rows) {
            codeValueMap.put(row.getCodeValue(), row);
            resultNameMap.put(row.getResultName(), row);
        }
        this.rowsByCodeValue = codeValueMap;
        this.rowsByResultName = resultNameMap;
    }

    /** Stream slices by chunk size */
    public Stream<ClientRequest> streamSlices(int chunkSize) {
        int total = rows.size();
        return IntStream.range(0, (int) Math.ceil((double) total / chunkSize))
                .mapToObj(i -> {
                    int start = i * chunkSize;
                    int end = Math.min(start + chunkSize, total);
                    return new ClientRequest(rows.subList(start, end));
                });
    }

    /** Update status by codeValue key */
    public void updateStatusByCodeValue(String codeValueKey, Status newStatus) {
        Row row = rowsByCodeValue.get(codeValueKey);
        if (row != null) row.updateStatus(newStatus);
    }

    /** Update status by resultName key */
    public void updateStatusByResultName(String resultNameKey, Status newStatus) {
        Row row = rowsByResultName.get(resultNameKey);
        if (row != null) row.updateStatus(newStatus);
    }

    /** Return a snapshot of all statuses */
    public List<Status> getStatusSnapshot() {
        List<Status> snapshot = new ArrayList<>();
        for (Row row : rows) snapshot.add(row.getStatus());
        return Collections.unmodifiableList(snapshot);
    }

    /** Convenience: Return lists of codeType, codeValue, resultName for serialization */
    public List<String> getCodeTypes() {
        List<String> list = new ArrayList<>();
        for (Row row : rows) list.add(row.getCodeType());
        return Collections.unmodifiableList(list);
    }

    public List<String> getCodeValues() {
        List<String> list = new ArrayList<>();
        for (Row row : rows) list.add(row.getCodeValue());
        return Collections.unmodifiableList(list);
    }

    public List<String> getResultNames() {
        List<String> list = new ArrayList<>();
        for (Row row : rows) list.add(row.getResultName());
        return Collections.unmodifiableList(list);
    }
}
