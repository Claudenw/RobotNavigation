package org.xenei.robot.mapper;

public class DoubleHalfMatrix {
    double[][] matrix;

    DoubleHalfMatrix(int size) {
        matrix = new double[size][];
        for (int i = 0; i < size; i++) {
            matrix[i] = new double[size - i];
        }
    }

    public void set(int i, int j, double d) {
        int idx1 = i < j ? i : j;
        int idx2 = i < j ? j : i;
        idx2 -= idx1;
        matrix[idx1][idx2] = d;
    }

    public double get(int i, int j) {
        int idx1 = i < j ? i : j;
        int idx2 = i < j ? j : i;
        idx2 -= idx1;
        return matrix[idx1][idx2];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix[0].length; i++) {
            sb.append(String.format("%3d: ", i));
            for (int j = 0; j < matrix[0].length; j++) {
                sb.append(String.format("%5.2f ", get(i, j)));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
