import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class EmployeeSerializerTest {

    @Test
    void testRoundTripSerialization() throws IOException {
        // Create PofContext and register serializer
        SimplePofContext context = new SimplePofContext();
        context.registerUserType(1000, Employee.class, new EmployeeSerializer());

        Employee original = new Employee(1, "Alice");

        // Serialize
        byte[] serializedData;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PofWriter writer = context.createPofWriter(baos); // <-- use OutputStream directly
            writer.writeObject(0, original);
            writer.flush();
            serializedData = baos.toByteArray();
        }

        // Deserialize
        Employee deserialized;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(serializedData)) {
            PofReader reader = context.createPofReader(bais); // <-- use InputStream directly
            deserialized = (Employee) reader.readObject(0);
        }

        assertEquals(original, deserialized, "Round-trip serialization failed");
    }
}
