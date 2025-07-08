package org.xenei.robot.common;

import org.xenei.robot.common.utils.RobutContext;


import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public interface Listeners<T> {

    void addListener(Consumer<T> listener);

    void removeListener(Consumer<T> listener);

    class ListenersImpl<T> implements Listeners<T> {
        private final RobutContext ctxt;
        private final CopyOnWriteArrayList<Consumer<T>> listeners;

        public ListenersImpl(RobutContext ctxt) {
            this.ctxt = ctxt;
            this.listeners = new CopyOnWriteArrayList<>();
        }

        public void addListener(Consumer<T> listener) {
            listeners.add(listener);
        }

        public void removeListener(Consumer<T> listener) {
            listeners.remove(listener);
        }

        protected void trigger(T state) {
            listeners.forEach(l -> ctxt.submit(() -> l.accept(state)));
        }
    }

    class ListenersWithPublicTrigger<T> extends ListenersImpl<T> {
        public ListenersWithPublicTrigger(RobutContext ctxt) {
            super(ctxt);
        }
        public void trigger(T state) {
            super.trigger(state);
        }
    }
}
