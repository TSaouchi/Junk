// src/test/java/com/toto/myproject/AutoSerializerTest.java
package com.toto.myproject;

import com.tangosol.io.pof.PofSerializer;
import com.toto.myproject.util.SerializerTestUtil;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AutoSerializerTest {

    @Test
    void testAllModelsHaveMatchingSerializers() throws Exception {
        Reflections modelReflections = new Reflections("com.toto.myproject.model");
        Reflections serializerReflections = new Reflections("com.toto.myproject.serializer");

        Set<Class<?>> modelClasses = modelReflections.getTypesAnnotatedWith(lombok.Data.class, true);
        if (modelClasses.isEmpty()) {
            modelClasses = modelReflections.getSubTypesOf(Object.class); // fallback
        }

        for (Class<?> modelClass : modelClasses) {
            String serializerName = "com.toto.myproject.serializer." + modelClass.getSimpleName() + "Serializer";

            Class<?> serializerClass;
            try {
                serializerClass = Class.forName(serializerName);
            } catch (ClassNotFoundException e) {
                fail("Missing serializer for model: " + modelClass.getName());
                continue;
            }

            assertTrue(PofSerializer.class.isAssignableFrom(serializerClass),
                    serializerName + " must implement PofSerializer");

            // Instantiate model
            Object modelInstance = createInstance(modelClass);

            // Instantiate serializer
            @SuppressWarnings("unchecked")
            PofSerializer<Object> serializer =
                    (PofSerializer<Object>) serializerClass.getDeclaredConstructor().newInstance();

            // Test round-trip
            Object deserialized = SerializerTestUtil.serializeAndDeserialize(modelInstance, serializer);

            assertNotNull(deserialized, "Deserialized object should not be null for " + modelClass.getName());
            assertEquals(modelInstance, deserialized,
                    "Round-trip serialization mismatch for " + modelClass.getName());
        }
    }

    private Object createInstance(Class<?> modelClass) throws Exception {
        try {
            Constructor<?> ctor = modelClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (NoSuchMethodException e) {
            // Try to find an all-args constructor and fill with dummy values
            for (Constructor<?> ctor : modelClass.getDeclaredConstructors()) {
                if (ctor.getParameterCount() > 0) {
                    Object[] args = new Object[ctor.getParameterCount()];
                    for (int i = 0; i < args.length; i++) {
                        Class<?> type = ctor.getParameterTypes()[i];
                        args[i] = dummyValue(type);
                    }
                    ctor.setAccessible(true);
                    return ctor.newInstance(args);
                }
            }
            throw new RuntimeException("No suitable constructor found for " + modelClass.getName());
        }
    }

    private Object dummyValue(Class<?> type) {
        if (type == int.class || type == Integer.class) return 42;
        if (type == long.class || type == Long.class) return 123L;
        if (type == boolean.class || type == Boolean.class) return true;
        if (type == double.class || type == Double.class) return 3.14;
        if (type == String.class) return "test";
        return null;
    }
}

<!-- JUnit 5 -->
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
  </dependency>

  <!-- Reflections for classpath scanning -->
  <dependency>
    <groupId>org.reflections</groupId>
    <artifactId>reflections</artifactId>
    <version>0.10.2</version>
    <scope>test</scope>
  </dependency>

// src/test/java/com/toto/myproject/util/SerializerTestUtil.java
package com.toto.myproject.util;

import com.tangosol.io.pof.*;
import com.tangosol.io.pof.reflect.SimplePofContext;

import java.io.*;

public class SerializerTestUtil {
    @SuppressWarnings("unchecked")
    public static <T> T serializeAndDeserialize(T obj, PofSerializer<T> serializer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        PofContext context = new SimplePofContext();
        PofBufferWriter writer = new PofBufferWriter(context, dos);
        serializer.serialize(writer, obj);
        writer.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        PofBufferReader reader = new PofBufferReader(context, dis);

        return serializer.deserialize(reader);
    }
}
