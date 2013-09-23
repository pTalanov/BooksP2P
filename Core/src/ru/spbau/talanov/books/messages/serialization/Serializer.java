package ru.spbau.talanov.books.messages.serialization;

import org.jetbrains.annotations.NotNull;
import ru.spbau.talanov.books.messages.Message;

import java.io.*;
import java.nio.ByteBuffer;

public final class Serializer {

    @NotNull
    public static byte[] serialize(@NotNull Message message) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(baos);
        objectOutputStream.writeObject(message);
        return baos.toByteArray();
    }

    @NotNull
    public static Message deserialize(@NotNull ByteBuffer buffer) throws IOException {
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = new ObjectInputStream(bais);
        Object object;
        try {
            object = objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
        if (object instanceof Message) {
            return (Message) object;
        }
        throw new IllegalStateException("Object of wrong type " + object.getClass());
    }
}
