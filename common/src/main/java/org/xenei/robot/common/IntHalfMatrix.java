package org.xenei.robot.common;

/**
 * A matrix of int where x[a,b] == x[b,a]. Only half the matrix is kept in
 * memory.
 */

public class IntHalfMatrix {
    private final int[][] matrix;

    public IntHalfMatrix(int size) {
        matrix = new int[size][];
        for (int i = 0; i < size; i++) {
            matrix[i] = new int[size - i];
        }
    }

    public int size() {
        return matrix[0].length;
    }

    public void set(int i, int j, int value) {
        int idx1 = Math.min(i, j);
        int idx2 = Math.max(i, j);
        idx2 -= idx1;
        matrix[idx1][idx2] = value;
    }

    /**
     * Increment the count for intersection i,j
     *
     * @param i
     *            the first index.
     * @param j
     *            the second index
     * @return this IntHalfMatrix.
     */
    public IntHalfMatrix increment(int i, int j) {
        int idx1 = Math.min(i, j);
        int idx2 = Math.max(i, j);
        idx2 -= idx1;
        matrix[idx1][idx2]++;
        return this;
    }

    /**
     * Decrement the count for intersection i,j
     *
     * @param i
     *            the first index
     * @param j
     *            the second index
     * @return this IntHalfMatrix.
     */
    public IntHalfMatrix decrement(int i, int j) {
        int idx1 = Math.min(i, j);
        int idx2 = Math.max(i, j);
        idx2 -= idx1;
        matrix[idx1][idx2]--;
        return this;
    }

    public void reset(Modifier f) {
        for (int[] row : matrix) {
            for (int i = 0; i < row.length; i++) {
                row[i] = f.mod(row[i]);
            }
        }
    }

    public int get(int i, int j) {
        int idx1 = Math.min(i, j);
        int idx2 = Math.max(i, j);
        idx2 -= idx1;
        return matrix[idx1][idx2];
    }

    public int[] reduction(Reducer r) {
        int[] result = new int[matrix[0].length];
        for (int i = 0; i < result.length; i++) {
            for (int j = 0; j < result.length; j++) {
                result[i] = r.apply(result[i], get(i, j));
            }
        }
        return result;
    }

    @FunctionalInterface
    public interface Reducer {
        int apply(int previousValue, int arg);
    }

    static Reducer plus = Integer::sum;
    static Reducer min = Math::min;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix[0].length; i++) {
            sb.append(String.format("%3d: ", i));
            for (int j = 0; j < matrix[0].length; j++) {
                sb.append(String.format("%3d ", get(i, j)));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    @FunctionalInterface
    public interface Modifier {
        int mod(int a);
    }

    static int arrayReducer(int[] values, Reducer r) {
        int result = 0;
        for (int value : values) {
            result = r.apply(result, value);
        }
        return result;
    }
}
