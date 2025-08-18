src/main/java/com/toto/myproject/model/Employee.java
src/main/java/com/toto/myproject/serializer/EmployeeSerializer.java
src/main/resources/pof-config.xml
src/test/java/com/toto/myproject/util/SerializerTestUtil.java
src/test/java/com/toto/myproject/AutoSerializerTest.java
src/test/java/com/toto/myproject/PofConfigValidationTest.java

package com.toto.myproject.util;

import com.tangosol.io.pof.PofSerializer;
import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofBufferReader;
import com.tangosol.io.pof.PofBufferWriter;

import java.io.*;

public class SerializerTestUtil {

    private static final ConfigurablePofContext context =
            new ConfigurablePofContext("pof-config.xml"); // must exist in src/main/resources

    @SuppressWarnings("unchecked")
    public static <T> T serializeAndDeserialize(T obj, PofSerializer<T> serializer) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        PofBufferWriter writer = new PofBufferWriter(context, dos);
        serializer.serialize(writer, obj);
        writer.flush();

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        PofBufferReader reader = new PofBufferReader(context, dis);

        return serializer.deserialize(reader);
    }
}


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
    void testRoundTripSerializationForAllModels() throws Exception {
        Reflections modelReflections = new Reflections("com.toto.myproject.model");
        Set<Class<?>> modelClasses = modelReflections.getSubTypesOf(Object.class);

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

            Object modelInstance = createInstance(modelClass);

            @SuppressWarnings("unchecked")
            PofSerializer<Object> serializer =
                    (PofSerializer<Object>) serializerClass.getDeclaredConstructor().newInstance();

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
            for (Constructor<?> ctor : modelClass.getDeclaredConstructors()) {
                if (ctor.getParameterCount() > 0) {
                    Object[] args = new Object[ctor.getParameterCount()];
                    for (int i = 0; i < args.length; i++) {
                        args[i] = dummyValue(ctor.getParameterTypes()[i]);
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


package com.toto.myproject;

import com.tangosol.io.pof.ConfigurablePofContext;
import com.tangosol.io.pof.PofSerializer;
import org.junit.jupiter.api.Test;
import org.reflections.Reflections;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PofConfigValidationTest {

    private final ConfigurablePofContext context =
            new ConfigurablePofContext("pof-config.xml"); // must be in src/main/resources

    @Test
    void testAllModelsAreInPofConfig() {
        Reflections modelReflections = new Reflections("com.toto.myproject.model");
        Set<Class<?>> modelClasses = modelReflections.getSubTypesOf(Object.class);

        Set<Integer> seenTypeIds = new HashSet<>();

        for (Class<?> modelClass : modelClasses) {
            int typeId = context.getUserTypeIdentifier(modelClass);

            assertTrue(typeId >= 0,
                    "Model " + modelClass.getName() + " is missing from pof-config.xml");

            // Ensure uniqueness
            assertTrue(seenTypeIds.add(typeId),
                    "Duplicate type-id " + typeId + " found in pof-config.xml");
        }
    }

    @Test
    void testAllSerializersImplementPofSerializer() {
        Reflections serializerReflections = new Reflections("com.toto.myproject.serializer");
        Set<Class<? extends PofSerializer>> serializerClasses =
                serializerReflections.getSubTypesOf(PofSerializer.class);

        assertFalse(serializerClasses.isEmpty(), "No serializers found in package");

        for (Class<? extends PofSerializer> serializerClass : serializerClasses) {
            assertTrue(PofSerializer.class.isAssignableFrom(serializerClass),
                    serializerClass.getName() + " must implement PofSerializer");
        }
    }
}
