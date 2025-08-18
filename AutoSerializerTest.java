public class EmployeeSerializerTest {

    @Test
    void testRoundTripSerialization() throws IOException {
        // Setup POF context
        SimplePofContext context = new SimplePofContext();
        // No custom serializer needed since Employee implements PortableObject
        context.registerUserType(1000, Employee.class);

        Employee original = new Employee(1, "Alice");

        // Serialize
        byte[] data;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            context.serialize(original, baos);
            data = baos.toByteArray();
        }

        // Deserialize
        Employee deserialized;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            deserialized = context.deserialize(bais);
        }

        assertEquals(original, deserialized, "Round-trip serialization failed");
    }
}
