//package org.xenei.robot.mover;
//
//import org.xenei.robot.common.Position;
//import org.xenei.robot.common.mapping.MapCoordinate;
//import org.xenei.robot.common.Mover;
//import org.xenei.robot.common.planning.TargetStack;
//import org.xenei.robot.common.utils.RobutContext;
//
//import java.util.concurrent.locks.Lock;
//
///**
// * On timer if targetselected if not clear view set stop. if targetselected-1 if
// * clear view set pause pop target stop
// */
//public class ClearViewLogicModule implements Mover.LogicModule, Runnable {
//    private final BaseMover mover;
//    private final TargetStack targetStack;
//    private final long sleepTime;
//    private Lock lock;
//    private final RobutContext.MotorStateTopic motorStateTopic;
//
//    public ClearViewLogicModule(TargetStack targetStack, RobutContext ctxt, BaseMover mover) {
//        this.targetStack = targetStack;
//        motorStateTopic = ctxt.motorStateTopic;
//        this.sleepTime = (long) ctxt.chassisInfo.motorInfo.freq()
//                * ctxt.chassisInfo.steps(ctxt.scaleInfo.getResolution());
//        this.mover = mover;
//        this.mover.register(this);
//    }
//
//    /**
//     */
//    @Override
//    public void run() {
//        Position position = mover.position();
//        if (!targetStack.peek().clearPath(position)) {
//            motorStateTopic.send(Mover.MotorState.STOP);
//        } else {
//            if (targetStack.size() > 1) {
//                MapCoordinate prevTarget = targetStack.get(targetStack.size() - 2);
//                if (prevTarget.clearPath(position)) {
//                    motorStateTopic.send(Mover.MotorState.STOP);
//                }
//            }
//        }
//        mover.sleep(1);
//    }
//
//    @Override
//    public void setLock(Lock lock) {
//        this.lock = lock;
//    }
//}
