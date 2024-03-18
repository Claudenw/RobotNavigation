package org.xenei.robot.rpi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.locationtech.jts.geom.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.Compass;
import org.xenei.robot.common.ScaleInfo;
import org.xenei.robot.rpi.testUtils.CoordinateUtils;
import org.xenei.robot.rpi.testUtils.TestChassisInfo;
import org.xenei.robot.common.utils.AngleUtils;
import org.xenei.robot.common.utils.DoubleUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.rpi.RpiMover.StepMonitor;
import org.xenei.robot.rpi.drivers.Motor;
import org.xenei.robot.rpi.drivers.Motor.SteppingStatus;
import org.xenei.robot.rpi.drivers.ULN2003.SteppingStatusImpl;

public class RpiMoverTest {

    private static final Logger LOG = LoggerFactory.getLogger(RpiMoverTest.class);
    RpiMover underTest;

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("setHeadingParameters")
    public void setHeadingTest(String name, double radiusFactor, double angle ) throws InterruptedException {
        TestingCompass compass = new TestingCompass(0);
        
        RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);
        TestingMotor left = new TestingMotor(1,TestChassisInfo.DEFAULT.radius*radiusFactor, compass);
        TestingMotor right = new TestingMotor(-1,TestChassisInfo.DEFAULT.radius*radiusFactor, compass);
        Coordinate coords = new Coordinate( 0,0 );
        underTest = new RpiMover(ctxt, compass, coords, left, right );
        
        underTest.setHeading(0);
        assertEquals( 0, underTest.position().getHeading());
        underTest.setHeading(angle);
        assertTrue(DoubleUtils.eq( angle, underTest.position().getHeading(), 0.01));
        CoordinateUtils.assertEquivalent(coords, underTest.position().getCoordinate(), 0.01);
        assertEquals(2, left.runCount);
        assertEquals(2, right.runCount);
        if (radiusFactor < 1.0)
        {
            assertTrue(underTest.getHeadingFactor() > TestChassisInfo.DEFAULT.radius);
        }
        if (radiusFactor > 1.0) {
            assertTrue(underTest.getHeadingFactor() < TestChassisInfo.DEFAULT.radius);
        }
    }
    
    public static Stream<Arguments> setHeadingParameters() {
        List<Arguments> lst = new ArrayList<>();
        
        lst.add(Arguments.of("UnderShoot-Left", 1.5, AngleUtils.RADIANS_45));
        lst.add(Arguments.of("UnderShoot-Right", 1.5, -AngleUtils.RADIANS_90));
        lst.add(Arguments.of("OverShoot-Left", .5, AngleUtils.RADIANS_45));
        lst.add(Arguments.of("OverShoot-Right", .5, -AngleUtils.RADIANS_90));
        return lst.stream();
    }

    @Test
    public void setHeadingTest_Zero() throws InterruptedException {
        TestingCompass compass = new TestingCompass(0);
        
        RobutContext ctxt = new RobutContext(ScaleInfo.DEFAULT, TestChassisInfo.DEFAULT);
        TestingMotor left = new TestingMotor(1,TestChassisInfo.DEFAULT.radius, compass);
        TestingMotor right = new TestingMotor(-1,TestChassisInfo.DEFAULT.radius, compass);
        Coordinate coords = new Coordinate( 0,0 );
        underTest = new RpiMover(ctxt, compass, coords, left, right );
        
        underTest.setHeading(0);
        assertEquals( 0, underTest.position().getHeading());
        assertTrue(DoubleUtils.eq( 0, underTest.position().getHeading(), 0.01));
        assertEquals(coords, underTest.position().getCoordinate());
        assertEquals(0, left.runCount);
        assertEquals(0, right.runCount);
        assertEquals(TestChassisInfo.DEFAULT.radius, underTest.getHeadingFactor());
    }
    
    class TestingCompass implements Compass {
        double heading;

        TestingCompass(double heading) {
            this.heading = heading;
        }
        @Override
        public double heading() {
            return heading;
        }

        @Override
        public double instantHeading() {
            return heading;
        }
        
        public synchronized void increment(double value) {
            heading += value;
        }
    }
    
    class TestingMotor implements Motor {
        double angleFactor;
        TestingCompass compass;
        int positiveAngleFactor;
        int runCount = 0;
        
        public TestingMotor(int positiveAngleFactor, double angleFactor, TestingCompass compass) {
            this.angleFactor = angleFactor;
            this.compass = compass;
            this.positiveAngleFactor = positiveAngleFactor;
        }

        @Override
        public void close() throws Exception {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean active() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public SteppingStatus prepareRun(int steps, int rpm) {
            long msPerStep = 50;
            int mySteps = (int) Math.round(steps * (angleFactor/TestChassisInfo.DEFAULT.radius));
            ++runCount;
            SteppingStatusImpl result = new SteppingStatusImpl(positiveAngleFactor, mySteps, msPerStep);
            
            LOG.debug("Preparing task {} steps:{} rpm:{}", this, mySteps, rpm);

            return result;
        }

        @Override
        public double stepsPerRotation() {
            return 360;
        }

        @Override
        public void stop() {
            // TODO Auto-generated method stub
            
        }
        private int limit(int value, int min, int max) {
            return (value < min) ? min : (value > max) ? max : value;
        }
        
        public class SteppingStatusImpl implements Motor.SteppingStatus {
            private volatile int  count;
            private final int initialCounter;
            private final boolean fwd;
            private final long msPerStep;
            private final int positiveAngleFactor;
            
            SteppingStatusImpl(int positiveAngleFactor, int steps, long msPerStep) {
                initialCounter = Math.abs(limit(steps, -32768, 32767));
                count = initialCounter;
                fwd = steps >= 0;
                this.msPerStep = msPerStep;
                this.positiveAngleFactor = positiveAngleFactor;
                LOG.debug("SteppingStatus created for %s steps", count);
            }

            @Override
            public SteppingStatusImpl call() throws InterruptedException {
                    Thread.sleep(msPerStep);
                    compass.increment(positiveAngleFactor*Math.toRadians(0.5)*(fwd ? count : -count));
                count = 0;
                LOG.info("SteppingStatus complete.  count:{} initial counter:{}", count, initialCounter);
                return this;
            }
            
            public boolean isRunning() {
                return count > 0;
            }
            
            /**
             * Gets the number of steps taken in a forward direction.
             * @return the number of steps taken, negative for reverse travel.
             */
            public int fwdSteps() {
                return (initialCounter-count) * (fwd ? 1 : -1);
            }
            
            public double fwdRotation() {
                return fwdSteps() / stepsPerRotation(); 
            }

            @Override
            public String toString() {
                return String.format( "SteppingStatus %s steps:%s rotation:%s", this.hashCode(), fwdSteps(), fwdRotation());
            }
        }
        
    }

}
