package org.xenei.robot.common;

/**
 * A matrix of double where x[a,b] == x[b,a]. Only half the matrix is kept in
 * memory.
 */
public class DoubleHalfMatrix {
    double[][] matrix;

    public DoubleHalfMatrix(int size) {
        matrix = new double[size][];
        for (int i = 0; i < size; i++) {
            matrix[i] = new double[size - i];
        }
    }

    public void set(int i, int j, double d) {
        int idx1 = Math.min(i, j);
        int idx2 = Math.max(i, j);
        idx2 -= idx1;
        matrix[idx1][idx2] = d;
    }

    public double get(int i, int j) {
        int idx1 = Math.min(i, j);
        int idx2 = Math.max(i, j);
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
