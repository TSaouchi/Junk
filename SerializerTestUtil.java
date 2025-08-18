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
