package org.xenei.robot.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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

import org.apache.commons.collections4.map.LRUMap;
import org.apache.jena.riot.RIOT;
import org.apache.jena.sparql.util.Symbol;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.xenei.robot.common.ChassisInfo;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.messages.Bus;
import org.xenei.robot.mapper.GraphGeomFactory;
import org.xenei.robot.mapper.rdf.Namespace;

public class RobutContext {
    public static final Symbol symbol = Symbol.create(RobutContext.class.getName());
    public final ChassisInfo chassisInfo;
    public final ScaleInfo scaleInfo;
    public final GeometryFactory geometryFactory;
    public final GeometryUtils geometryUtils;
    public final GraphGeomFactory graphGeomFactory;
    public final java.util.Map<String, Geometry> cache = Collections.synchronizedMap(new LRUMap<String, Geometry>(500));
    private final ForkJoinPool workScheduler = (ForkJoinPool) Executors.newWorkStealingPool();
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    public final Bus bus;
    public final Visualizations visualizations;

    /**
     * Constructor
     * 
     * @param scaleInfo Info about the scaling of the map.
     * @param chassisInfo Info about the chassis of the robot.
     */
    public RobutContext(ScaleInfo scaleInfo, ChassisInfo chassisInfo) {
        RIOT.getContext().put(symbol, this);
        this.scaleInfo = scaleInfo;
        this.chassisInfo = chassisInfo;
        this.geometryFactory = new GeometryFactory(scaleInfo.getPrecisionModel());
        this.geometryUtils = new GeometryUtils(this);
        this.graphGeomFactory = new GraphGeomFactory(geometryUtils);
        this.bus = new Bus(this);
        this.visualizations = new Visualizations();

        Namespace.init(this);
    }

    public double getScaledRadius() {
        return chassisInfo.radius + scaleInfo.getResolution();
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return scheduledExecutor.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public void shutdown() {
        scheduledExecutor.shutdown();
        workScheduler.shutdown();
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
     * @param callables the callables to execute.
     * @return List of futures for the callables.
     * @param <T> the return type for the futures.
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
        return String.format( "Qs:%d Qt:%d At:%d Rt:%d", workScheduler.getQueuedSubmissionCount(), workScheduler.getQueuedTaskCount(),
                workScheduler.getActiveThreadCount(), workScheduler.getRunningThreadCount());
    }



    public interface Processor<T> extends Consumer<T> {
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
    };

}
