package org.xenei.robot.mover;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.MapCoordinate;
import org.xenei.robot.common.Mover;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.mapping.MapPosition;
import org.xenei.robot.common.messages.Topic;
import org.xenei.robot.common.planning.Segment;
import org.xenei.robot.common.planning.TargetStack;
import org.xenei.robot.common.utils.RobutContext;

import java.util.concurrent.locks.Lock;

/**
 * On timer if targetselected if not clear view set stop. if targetselected-1 if
 * clear view set pause pop target stop
 */
public class ClearViewLogicModule implements Mover.LogicModule, Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ClearViewLogicModule.class);

    private final BaseMover mover;
    private final Map map;
    private final TargetStack targetStack;
    private final RobutContext ctxt;
    private final long sleepTime;
    private Lock lock;
    private final Topic<Mover.MotorState> motorStateTopic;

    public ClearViewLogicModule(Map map, TargetStack targetStack, RobutContext ctxt, BaseMover mover) {
        this.targetStack = targetStack;
        this.map = map;
        this.ctxt = ctxt;
        motorStateTopic = ctxt.bus.motor;
        this.sleepTime = (long) ctxt.chassisInfo.motorInfo.freq()
                * ctxt.chassisInfo.steps(ctxt.scaleInfo.getResolution());
        this.mover = mover;
        this.mover.register(this);
    }

    /**
     */
    @Override
    public void run() {
        MapCoordinate position = map.asMapCoordinate(mover.position());
        if (!map.isClearPath(position, targetStack.peek().getNextLocation())) {
            motorStateTopic.send(Mover.MotorState.STOP);
        } else {
            if (targetStack.size() > 1) {
                Segment prevSegment = targetStack.get(targetStack.size() - 2);
                if (map.isClearPath(position, prevSegment.getNextLocation())) {
                    motorStateTopic.send(Mover.MotorState.STOP);
                }
            }
        }
        mover.sleep(1);
    }

    @Override
    public void setLock(Lock lock) {
        this.lock = lock;
    }
}
