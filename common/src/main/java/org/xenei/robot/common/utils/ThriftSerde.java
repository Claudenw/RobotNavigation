package org.xenei.robot.common.utils;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.messages.Serializer;

public interface ThriftSerde<T> {
    void serialize(T obj, TProtocol protocol) throws TException;
    T deserialize(TProtocol protocol) throws TException;
}
