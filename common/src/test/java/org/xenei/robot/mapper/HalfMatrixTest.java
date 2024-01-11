package org.xenei.robot.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class HalfMatrixTest {

    @Test
    public void TestDoubleHalfMatrix() {
        DoubleHalfMatrix m = new DoubleHalfMatrix(5);
        for (int i=0;i<5;i++) {
            for (int j=i; j<5; j++) {
                m.set(i, j, 1.0*i*j);
            }
        }
        
        for (int i=0;i<5;i++) {
            for (int j=0;j<5;j++) {
                assertEquals( 1.0*i*j, m.get(i, j));
            }
        }
    }

}
