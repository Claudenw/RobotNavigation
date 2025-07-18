package org.xenei.robot.common.messages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.DistanceSensor;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.ml.SensorLayer;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class Bus {

    public final Topic<SensorLayer> bump = new TopicImpl<SensorLayer>();
    public final Topic<DistanceSensor.Readings> distance = new TopicImpl<DistanceSensor.Readings>();
    public final Topic<Mover.MotorState> motor = new TopicImpl<Mover.MotorState>();
    public final Topic<Mover.MoveTo> moveTo = new TopicImpl<Mover.MoveTo>();


    private final RobutContext ctxt;
    public Bus(RobutContext ctxt) {
        this.ctxt = ctxt;
    }

    public class TopicImpl<T> implements Topic<T> {
        private Logger log;
        private final CopyOnWriteArrayList<Consumer<T>> listeners;

        public TopicImpl() {
            this.listeners = new CopyOnWriteArrayList<>();
        }

        public void register(Consumer<T> p) {
            listeners.add(p);
        }

        public void unregister(Consumer<T> p) {
            listeners.remove(p);
        }

        public void send(T message) {
            if (log == null) {
                log = LoggerFactory.getLogger(message.getClass());
            }
            if (log.isDebugEnabled()) {
                log.debug("{} event: {} sent to {} listeners", message.getClass().getSimpleName(), message, listeners.size());
            }
            listeners.forEach(p -> ctxt.submit(() -> p.accept(message)));
        }
    }
}
