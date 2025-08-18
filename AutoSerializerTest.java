// src/test/java/com/toto/myproject/serializer/EmployeeSerializerTest.java
package com.toto.myproject.serializer;

import com.toto.myproject.model.Employee;
import com.tangosol.io.pof.*;
import com.tangosol.io.WriteBuffer;
import com.tangosol.io.ReadBuffer;
import com.tangosol.io.nio.ByteArrayWriteBuffer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;

public class EmployeeSerializerTest {

    @Test
    void testRoundTripSerialization() throws IOException {
        // Create a SimplePofContext and register the serializer
        SimplePofContext context = new SimplePofContext();
        context.registerUserType(1000, Employee.class, new EmployeeSerializer());
        
        Employee original = new Employee("Alice", 25);
        
        // Serialize
        WriteBuffer writeBuffer = new ByteArrayWriteBuffer();
        WriteBuffer.BufferOutput out = writeBuffer.getBufferOutput();
        PofBufferWriter writer = new PofBufferWriter(out, context);
        writer.writeObject(0, original);
        
        // Deserialize
        ReadBuffer readBuffer = writeBuffer.getReadBuffer();
        ReadBuffer.BufferInput in = readBuffer.getBufferInput();
        PofBufferReader reader = new PofBufferReader(in, context);
        Employee deserialized = (Employee) reader.readObject(0);
        
        // Verify
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getAge(), deserialized.getAge());
        assertEquals(original, deserialized, "Round-trip serialization failed");
    }

    @Test
    void testRoundTripWithNullName() throws IOException {
        SimplePofContext context = new SimplePofContext();
        context.registerUserType(1000, Employee.class, new EmployeeSerializer());
        
        Employee original = new Employee(null, 30);
        
        // Serialize
        WriteBuffer writeBuffer = new ByteArrayWriteBuffer();
        WriteBuffer.BufferOutput out = writeBuffer.getBufferOutput();
        PofBufferWriter writer = new PofBufferWriter(out, context);
        writer.writeObject(0, original);
        
        // Deserialize
        ReadBuffer readBuffer = writeBuffer.getReadBuffer();
        ReadBuffer.BufferInput in = readBuffer.getBufferInput();
        PofBufferReader reader = new PofBufferReader(in, context);
        Employee deserialized = (Employee) reader.readObject(0);
        
        // Verify
        assertNull(deserialized.getName());
        assertEquals(original.getAge(), deserialized.getAge());
        assertEquals(original, deserialized);
    }

    @Test
    void testRoundTripWithZeroAge() throws IOException {
        SimplePofContext context = new SimplePofContext();
        context.registerUserType(1000, Employee.class, new EmployeeSerializer());
        
        Employee original = new Employee("Bob", 0);
        
        // Serialize
        WriteBuffer writeBuffer = new ByteArrayWriteBuffer();
        WriteBuffer.BufferOutput out = writeBuffer.getBufferOutput();
        PofBufferWriter writer = new PofBufferWriter(out, context);
        writer.writeObject(0, original);
        
        // Deserialize
        ReadBuffer readBuffer = writeBuffer.getReadBuffer();
        ReadBuffer.BufferInput in = readBuffer.getBufferInput();
        PofBufferReader reader = new PofBufferReader(in, context);
        Employee deserialized = (Employee) reader.readObject(0);
        
        // Verify
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getAge(), deserialized.getAge());
        assertEquals(original, deserialized);
    }

    @Test
    void testDirectSerializerMethods() throws IOException {
        EmployeeSerializer serializer = new EmployeeSerializer();
        SimplePofContext context = new SimplePofContext();
        
        Employee original = new Employee("Charlie", 35);
        
        // Test serialize method directly
        WriteBuffer writeBuffer = new ByteArrayWriteBuffer();
        WriteBuffer.BufferOutput out = writeBuffer.getBufferOutput();
        PofBufferWriter writer = new PofBufferWriter(out, context);
        
        serializer.serialize(writer, original);
        
        // Test deserialize method directly
        ReadBuffer readBuffer = writeBuffer.getReadBuffer();
        ReadBuffer.BufferInput in = readBuffer.getBufferInput();
        PofBufferReader reader = new PofBufferReader(in, context);
        
        Employee deserialized = serializer.deserialize(reader);
        
        // Verify
        assertEquals(original.getName(), deserialized.getName());
        assertEquals(original.getAge(), deserialized.getAge());
        assertEquals(original, deserialized);
    }
}
