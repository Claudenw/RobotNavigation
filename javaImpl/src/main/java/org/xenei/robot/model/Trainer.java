//package org.xenei.robot.navigation;
//
//import java.util.concurrent.ThreadLocalRandom;
//import org.nd4j.linalg.cpu.nativecpu.NDArray;
//import org.nd4j.linalg.cpu.nativecpu.buffer.DoubleBuffer;
//import org.nd4j.linalg.api.ndarray.INDArray;
//
//import org.nd4j.linalg.factory.Nd4j;
//import org.xenei.robot.model.RobotModel;
//
//public class Trainer {
//
//    RobotModel model = new RobotModel();    
//    Rectangular target;
//    Position position;
//
//    Trainer() {
//        ThreadLocalRandom random = ThreadLocalRandom.current();
//        target = new Rectangular( random.nextDouble(500), random.nextDouble(500));
//        position = new Position();
//        while (position.distanceTo(target) <= 1.0) {
//            target = new Rectangular( random.nextDouble(500), random.nextDouble(500));
//        }
//        System.out.format("%s d:%s\n", position, position.distanceTo(target));
//    }
//    
//    public void train() {
//        
//        double result = 0.0;
//        do {
//            INDArray startValues = createStartValues();
//            INDArray expected = createExpected();
//            INDArray resultValues = model.learn(startValues, expected);
//            ControllerCommand cmd = readINDArray(resultValues);
//            Position next = position.nextPosition(cmd);
//            result = next.distanceTo(target);
//            double delta = result-position.distanceTo(target);
//            System.out.format("%s %s d:%s \u0394:%s\n", cmd, next, result, delta);
//            this.position = next;
//        } while (result > 1.0);
//    }
//    private INDArray createStartValues() {
//        double[] bufValues = { target.getX(), target.getY(), position.getX(), position.getY(),
//                position.getRadians()
//        };
//        return  new NDArray(new DoubleBuffer(bufValues));
//    }
//    
//    private ControllerCommand readINDArray(INDArray resultValues) {
//        return ControllerCommand.fromRadians(
//                DoubleUtils.valueOrZero(resultValues.getDouble(0)), 
//                DoubleUtils.valueOrZero(resultValues.getDouble(1)));
//    }
//    
//    private INDArray createExpected() {
//        double[] bufValues = { position.angleTo(target), position.distanceTo(target) };
//        return  new NDArray(new DoubleBuffer(bufValues));
//    }
//    
//    public static void main(String[] args) {
//        Trainer trainer = new Trainer();
//        trainer.train();
//    }
//}
//



