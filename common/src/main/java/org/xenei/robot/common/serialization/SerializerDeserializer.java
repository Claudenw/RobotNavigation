package org.xenei.robot.common.serialization;

/**
 * A serializer / deserializer pair.
 *
 * @param <T> the class that is serialized / deserialized.
 */
public interface SerializerDeserializer<T> {
    /**
     * Serialize T to byte[]
     *
     * @param obj the object to serialize.
     * @return the serialized byte buffer.
     */
    byte[] serialize(T obj) throws SerializationException;

    /**
     * Deserialize a byte buffer into T
     *
     * @param bytes the byte buffer to deserialize from.
     * @return the T extracted from the buffer.
     */
    T deserialize(byte[] bytes) throws SerializationException;
}
