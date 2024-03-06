package org.xenei.robot.common;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

public class ListenerContainerImpl implements ListenerContainer {
    private final Collection<Listener> listeners;
    

    public ListenerContainerImpl() {
        this.listeners = new CopyOnWriteArrayList<>();
    }

    @Override
    public void addListener(Listener listener) {
        this.listeners.add(listener);
    }

    @Override
    public void notifyListeners() {
        Collection<Listener> l = this.listeners;
        try {
            l.forEach(Listener::update);
        } finally {
        }
    }
}
