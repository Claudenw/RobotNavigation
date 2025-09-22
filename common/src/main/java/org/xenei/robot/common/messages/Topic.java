package org.xenei.robot.common.messages;

import java.util.function.Consumer;

public interface Topic<T> {

	void register(Consumer<T> p);

	void unregister(Consumer<T> p);

	void send(T message);
}
