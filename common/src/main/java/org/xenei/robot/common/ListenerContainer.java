package org.xenei.robot.common;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public interface ListenerContainer {

    /**
     * Add a planner listener. The listener will be called when a planning move is
     * completed.
     * 
     * @param listener the listener to notify.
     */
    default void addListener(Runnable listener) {
        addListener(() -> {listener.run(); return null;});
    }
    /**
     * Add a planner listener. The listener will be called when a planning move is
     * completed.
     *
     * @param listener the listener to notify.
     */
    void addListener(Callable<Void> listener);

    /**
     * Notify listeners to reprocess data.
     */
    void notifyListeners();

    /**
     * A functional interface that defines a listener to update.
     */
    @FunctionalInterface
    interface Listener {
        void update();
    }
}
