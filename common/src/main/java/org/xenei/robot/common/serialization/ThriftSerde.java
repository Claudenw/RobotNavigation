package org.xenei.robot.common.serialization;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocol;

public interface ThriftSerde<T> {
    void serialize(T obj, TProtocol protocol) throws TException;
    T deserialize(TProtocol protocol) throws TException;
}
