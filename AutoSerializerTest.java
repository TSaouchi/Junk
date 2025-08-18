import com.tangosol.io.pof.SimplePofContext;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PofReader;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class EmployeeSerializerTest {

    @Test
    void testRoundTripSerialization() throws IOException {
        // Create a SimplePofContext and register the serializer
        SimplePofContext context = new SimplePofContext();
        context.registerUserType(1000, Employee.class, new EmployeeSerializer());

        Employee original = new Employee(1, "Alice");

        // Serialize using try-with-resources to ensure streams are closed
        byte[] serializedData;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(baos)) {

            PofWriter writer = context.createPofWriter(dos);
            writer.writeObject(0, original);
            writer.flush();
            serializedData = baos.toByteArray();
        }

        // Deserialize
        Employee deserialized;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(serializedData);
             DataInputStream dis = new DataInputStream(bais)) {

            PofReader reader = context.createPofReader(dis);
            deserialized = (Employee) reader.readObject(0);
        }

        assertEquals(original, deserialized, "Round-trip serialization failed");
    }
}
