package com.example;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ClientRequestTest {

    private static final int ROWS = 1000;
    private static final int THREADS = 20;
    private static final int ITERATIONS = 500;

    @Test
    void testRandomConcurrentUpdates() throws InterruptedException, ExecutionException {
        // Generate large dataset
        List<String> codeType = new ArrayList<>();
        List<String> codeValue = new ArrayList<>();
        List<String> resultName = new ArrayList<>();

        for (int i = 0; i < ROWS; i++) {
            codeType.add("T" + i);
            codeValue.add("V" + i);
            resultName.add("R" + i);
        }

        ClientRequest request = new ClientRequest(codeType, codeValue, resultName, Status.NOTFOUND);

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        List<Callable<Void>> tasks = new ArrayList<>();
        Random random = new Random();

        // Create random update tasks
        for (int i = 0; i < ITERATIONS; i++) {
            tasks.add(() -> {
                int idx = random.nextInt(ROWS);
                Status newStatus = Status.values()[random.nextInt(Status.values().length)];
                if (random.nextBoolean()) {
                    request.updateStatusByCodeValue("V" + idx, newStatus);
                } else {
                    request.updateStatusByResultName("R" + idx, newStatus);
                }
                return null;
            });
        }

        // Execute all tasks concurrently
        executor.invokeAll(tasks);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        // Verify all statuses are valid (should be one of the enum values)
        for (Status s : request.getStatusSnapshot()) {
            assertNotNull(s);
            assertTrue(s == Status.NOTFOUND || s == Status.OK || s == Status.FAILED);
        }
    }

    @Test
    void testSlicesAndConcurrentUpdates() throws InterruptedException {
        // Small dataset for slice testing
        List<String> codeType = List.of("T1","T2","T3","T4","T5","T6");
        List<String> codeValue = List.of("V1","V2","V3","V4","V5","V6");
        List<String> resultName = List.of("R1","R2","R3","R4","R5","R6");

        ClientRequest parent = new ClientRequest(codeType, codeValue, resultName, Status.NOTFOUND);

        List<ClientRequest> slices = parent.slice(2);

        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.submit(() -> slices.get(0).updateStatusByCodeValue("V1", Status.OK));
        executor.submit(() -> slices.get(1).updateStatusByResultName("R4", Status.FAILED));
        executor.submit(() -> slices.get(2).updateStatusByCodeValue("V6", Status.OK));

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Ensure parent reflects updates from slices
        List<Status> expected = List.of(Status.OK, Status.NOTFOUND, Status.NOTFOUND, Status.FAILED, Status.NOTFOUND, Status.OK);
        assertEquals(expected, parent.getStatusSnapshot());
    }

    @Test
    void testStressWithStreamSlices() throws InterruptedException {
        int totalRows = 200;
        List<String> codeType = new ArrayList<>();
        List<String> codeValue = new ArrayList<>();
        List<String> resultName = new ArrayList<>();
        for (int i = 0; i < totalRows; i++) {
            codeType.add("T" + i);
            codeValue.add("V" + i);
            resultName.add("R" + i);
        }

        ClientRequest request = new ClientRequest(codeType, codeValue, resultName, Status.NOTFOUND);

        ExecutorService executor = Executors.newFixedThreadPool(10);
        List<Runnable> tasks = new ArrayList<>();
        Random random = new Random();

        // Randomly update statuses in streamed slices
        request.streamSlices(20).forEach(slice -> {
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> {
                    int idx = random.nextInt(slice.getCodeValues().size());
                    Status newStatus = Status.values()[random.nextInt(Status.values().length)];
                    slice.updateStatusByCodeValue(slice.getCodeValues().get(idx), newStatus);
                });
            }
        });

        for (Runnable task : tasks) executor.submit(task);
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        // All statuses should still be valid enum values
        for (Status s : request.getStatusSnapshot()) {
            assertNotNull(s);
            assertTrue(s == Status.NOTFOUND || s == Status.OK || s == Status.FAILED);
        }
    }
}
