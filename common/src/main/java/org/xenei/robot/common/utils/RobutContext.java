package org.xenei.robot.common.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.locationtech.jts.geom.GeometryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.ChassisInfo;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.serialization.SerializationException;
import org.xenei.robot.common.serialization.SerializerDeserializer;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.sensor.distance.DistanceSensor;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.sensor.bump.BumpSensorModel;
import org.xenei.robot.mapper.rdf.GraphGeomFactory;
import org.xenei.robot.mapper.visualization.RemoteVisualization;

public final class RobutContext implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RobutContext.class);
    public final ChassisInfo chassisInfo;
    public final ScaleInfo scaleInfo;
    public final GeometryFactory geometryFactory;
    public final GeometryUtils geometryUtils;
    public final GraphGeomFactory graphGeomFactory;
    private final ForkJoinPool workScheduler = (ForkJoinPool) Executors.newWorkStealingPool();
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private final Options connectionOptions;
    public final double scaledRadius;
    private final Connection connection;
    private final CopyOnWriteArrayList<RobutContext.TopicRegistration> topicRegistrations = new CopyOnWriteArrayList<>();
    public final ByteTopic rawBumpSensorTopic;
    public final Topic<BumpSensorModel.SensorResult> bumpSensorTopic;
    public final Topic<DistanceSensor.Readings> distanceSensorTopic;
    public final ByteTopic motorStateTopic;
    public final Topic<Location> moveToTopic;
    public final String vizName;

    /**
     * Constructor
     *
     * @param builder The RobutContext builder.
     */
    private RobutContext(Builder builder) {
        this.scaleInfo = builder.scaleInfo;
        this.chassisInfo = builder.chassisInfo;
        final String id = builder.id;
        this.geometryFactory = new GeometryFactory(scaleInfo.getPrecisionModel());
        this.geometryUtils = new GeometryUtils(geometryFactory, scaleInfo);
        this.graphGeomFactory = new GraphGeomFactory(geometryUtils);
        this.vizName = id + ".remoteVisualization";
        this.scaledRadius = scaleInfo.scale(chassisInfo.radius);
        this.connectionOptions = builder.getConnectionOptions();
        try {
            this.connection = Nats.connect(connectionOptions);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        this.rawBumpSensorTopic = new ByteTopic(connection, id + ".sensor.bump.raw");
        this.bumpSensorTopic = new Topic<BumpSensorModel.SensorResult>(connection, id + ".sensor.bump.model", new BumpSensorModel.Serde());
        this.distanceSensorTopic = new Topic<DistanceSensor.Readings>(connection, id + ".sensor.distance", new DistanceSensor.Serde());
        this.motorStateTopic = new ByteTopic(connection, id + ".motor.state", Mover.MotorState::validateState);
        this.moveToTopic = new Topic<Location>(connection, id + ".moveTo", new Location.Serde());
//        String natsURL = System.getenv("NATS_URL");
//        if (natsURL == null) {
//            natsURL = "nats://127.0.0.1:4222";
//        }
//        Options options = new Options.Builder()
//                .server(natsURL)
//                .userInfo("local", "1UH6NBQ4RYZHXdZTLKrhOodYJmI6pmD2") // Set a user and plain text password
//                .connectionName("RobutContext:" + id)
//                .build();
//        try {
//            connection = Nats.connect(options);
//        } catch (IOException | InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }


    public static Builder builder() {
        return new Builder();
    }

    public void enableRemoteVisualization(Map map, Supplier<Solution> solutionSupplier,
                                          Supplier <? extends Position> positionSupplier, Supplier<? extends Location> targetSupplier) {
        RemoteVisualization remoteVisualization = new RemoteVisualization();
        scheduledExecutor.scheduleAtFixedRate(() -> remoteVisualization.draw(map, solutionSupplier, positionSupplier, targetSupplier), 0, 500, TimeUnit.MILLISECONDS);
    }
    /**
     * Creates a new connection to the same NATS server.
     * @return the new connection.
     */
    public Options getConnectionOptions() {
        return connectionOptions;
    }

    public void sendRemoteVizCommand(byte[] command) {
        connection.publish(vizName, command);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public void close() {
        scheduledExecutor.shutdown();
        workScheduler.shutdown();
        try {
            scheduledExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Unable to shutdown scheduled executor: {}", e.getMessage(), e);
            scheduledExecutor.shutdownNow();
        }
        try {
            workScheduler.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOGGER.error("Unable to shutdown work scheduler: {}", e.getMessage(), e);
            workScheduler.shutdownNow();
        }
        topicRegistrations.forEach(TopicRegistration::unsubscribe);
        try {
            connection.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Runnable> shutdownNow() {
        List<Runnable> r = new ArrayList<Runnable>();
        r.addAll(scheduledExecutor.shutdownNow());
        r.addAll(workScheduler.shutdownNow());
        return r;
    }

    public boolean isShutdown() {
        return scheduledExecutor.isShutdown() & workScheduler.isShutdown();
    }

    public boolean isTerminated() {
        return scheduledExecutor.isTerminated() & workScheduler.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return scheduledExecutor.awaitTermination(timeout, unit) & workScheduler.awaitTermination(timeout, unit);
    }

    public <T> CompletableFuture<T> submit(Callable<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        workScheduler.execute(() -> {
            try {
                future.complete(task.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        return future;
    }

    /**
     * Submits a collection of callables to the scheduler.
     *
     * @param callables the callables to execute.
     * @param <T>       the return type for the futures.
     * @return List of futures for the callables.
     */
    public <T> List<Future<T>> submit(Collection<? extends Callable<T>> callables) {
        return workScheduler.invokeAll(callables);
    }

    public CompletableFuture<?> submit(Runnable task) {
        return CompletableFuture.runAsync(task, workScheduler);
    }

    public boolean awaitQuiescence(long timeout, TimeUnit unit) {
        return workScheduler.awaitQuiescence(timeout, unit);
    }

    public String workerReport() {
        return String.format("Qs:%d Qt:%d At:%d Rt:%d", workScheduler.getQueuedSubmissionCount(),
                workScheduler.getQueuedTaskCount(), workScheduler.getActiveThreadCount(),
                workScheduler.getRunningThreadCount());
    }

    public class Visualizations implements Map.Visualization {

        private final CopyOnWriteArrayList<Map.Visualization> listeners = new CopyOnWriteArrayList<>();

        public void register(Map.Visualization p) {
            listeners.add(p);
        }

        public void unregister(Map.Visualization p) {
            listeners.remove(p);
        }

        public void redraw() {
            listeners.forEach(p -> submit(p::redraw));
        }
    }

    public final class TopicRegistration {
        private final Dispatcher dispatcher;

        private TopicRegistration(Dispatcher dispatcher, String topic) {
            this.dispatcher = dispatcher;
            dispatcher.subscribe(topic);
            topicRegistrations.add(this);
        }

        public void unsubscribe() {
            if (dispatcher.isActive()) {
                connection.closeDispatcher(dispatcher);
                topicRegistrations.remove(this);
            }
        }
    }

    /**
     * A topic on a connection.
     *
     * @param <T> the Class of object sent on the connection.
     */
    public final class Topic<T> {
        private final Connection connection;
        private final String topic;
        private final SerializerDeserializer<T> serde;

        private Topic(final Connection connection, final String topic, final SerializerDeserializer<T> serde) {
            this.connection = connection;
            this.topic = topic;
            this.serde = serde;
        }

        public void send(T value) {
            try {
                connection.publish(topic, serde.serialize(value));
            } catch (SerializationException e) {
                LOGGER.error("Unable to serialize {}: {}", value, e.getMessage(), e);
            }
        }

        public TopicRegistration listen(Consumer<T> listener) {
            Dispatcher dispatcher = connection.createDispatcher(msg -> { try {
                listener.accept(serde.deserialize(msg.getData()));
            } catch (SerializationException e) {
                LOGGER.error("Error while deserializing data: {}", e.getMessage(), e);
            }
            });
            return new TopicRegistration(dispatcher, topic);
        }
    }

    /**
     * A topic on a connection that reads/write single bytes
     */
    public final class ByteTopic {
        @FunctionalInterface
        interface Validator {
            void validate(byte value) throws SerializationException;
        }

        private final Connection connection;
        private final String topic;
        private final Validator validator;

        private ByteTopic(final Connection connection, final String topic, Validator validator) {
            this.connection = connection;
            this.topic = topic;
            this.validator = validator;
        }

        private ByteTopic(final Connection connection, final String topic) {
            this.connection = connection;
            this.topic = topic;
            validator = x -> {};        }

        public void send(byte value) {
            try {
                validator.validate(value);
                connection.publish(topic, new byte[]{value});
            } catch (SerializationException e) {
                LOGGER.error("Unable to serialize {}: {}", value, e.getMessage(), e);
            }
        }

        public TopicRegistration listen(IntConsumer listener) {
            Dispatcher dispatcher = connection.createDispatcher(msg -> {
                byte value = msg.getData()[0];
                try {
                    validator.validate(value);
                    listener.accept(value);
                } catch (SerializationException e) {
                    String errMsg = String.format("Error while deserializing '%s' (0x%02X): %s", value, value, e.getMessage());
                    LOGGER.error(errMsg, e);
                }
            });
            return new TopicRegistration(dispatcher, topic);
        }
    }

    public static class Builder {
        private String id = null;
        private ScaleInfo scaleInfo;
        private ChassisInfo chassisInfo;
        private Options.Builder options;

        public Builder() {
        }

        public RobutContext build() {
            validate();
            RobutContext ctxt = new RobutContext(this);
            id = null;
            return ctxt;
        }

        private void validate() {
            Objects.requireNonNull(scaleInfo, "scaleInfo");
            Objects.requireNonNull(chassisInfo, "chassisInfo");
            Objects.requireNonNull(options, "options");
            if (id == null) {
                id = UUID.randomUUID().toString();
            }
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder scaleInfo(ScaleInfo scaleInfo) {
            this.scaleInfo = scaleInfo;
            return this;
        }

        public Builder chassisInfo(ChassisInfo chassisInfo) {
            this.chassisInfo = chassisInfo;
            return this;
        }

        public Builder options(Options.Builder options) {
            this.options = options;
            if (id != null) {
                this.options.connectionName("RobutContext:" + id);
            }
            return this;
        }

        private Options getConnectionOptions() {
            return options == null ? new Options.Builder().server(Options.DEFAULT_URL).build() : options.build();
        }
    }
}
