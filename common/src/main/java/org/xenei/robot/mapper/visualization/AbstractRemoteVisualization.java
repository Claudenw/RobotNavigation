package org.xenei.robot.mapper.visualization;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.Subscription;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

public abstract class AbstractRemoteVisualization implements AutoCloseable, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteVisualization.class);

    private final Options connectionOptions;
    private final String remoteTopic;
    private final RemoteVisualization visualization;
    private boolean running;

    public AbstractRemoteVisualization(Options connectionOptions, final String remoteTopic) {
        this.connectionOptions = connectionOptions;
        this.remoteTopic = remoteTopic;
        this.visualization = new RemoteVisualization();
    }

    public final void run() {
        if (running) {
            LOG.error("Already running");

        } else {
            running = true;
            try (Connection connection = Nats.connect(connectionOptions)) {
                Subscription subscription = connection.subscribe(remoteTopic);
                while (running) {
                    try {
                        Message msg = subscription.nextMessage(Duration.ofMinutes(2));
                        if (msg != null) {
                            List<RemoteVisualization.DrawingCommand> cmds = visualization.deserialize(msg.getData());
                            draw(cmds);
                        }
                    } catch (InterruptedException e) {
                        // expected just loop
                    }
                }
            } catch (IOException e) {
                LOG.error("Aborting.  Error opening Connection: {}", e.getMessage(), e);
            } catch (InterruptedException e) {
                LOG.error("Aborting.  Interrupted: {}", e.getMessage(), e);
            } catch (TTransportException e) {
                LOG.error("Aborting.  Error reading data: {}", e.getMessage(), e);
            }
        }
    }

    public void close() {
        running = false;
    }

    protected abstract void draw(final List<RemoteVisualization.DrawingCommand> cmds);
}
