package org.xenei.robot.common;

import org.xenei.robot.common.utils.RobutContext;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

public class ListenerContainerImpl implements ListenerContainer {
    private final Collection<Callable<Void>> listeners;
    private final RobutContext context;

    public ListenerContainerImpl(RobutContext ctxt) {
        this.listeners = new CopyOnWriteArrayList<>();
        this.context = ctxt;
    }

    @Override
    public void addListener(Callable<Void> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void notifyListeners() {
        context.submit(listeners);
    }
}
