package org.xenei.robot.common;

import java.util.Comparator;
import java.util.function.BiFunction;

public class HalfMatrix<T> {
    Object[][] matrix;

    public HalfMatrix(int size) {
        matrix = new Object[size][];
        for (int i = 0; i < size; i++) {
            matrix[i] = new Object[size - i];
        }
    }

    public void set(int i, int j, T d) {
        int idx1 = Math.min(i, j);
        int idx2 = Math.max(i, j);
        idx2 -= idx1;
        matrix[idx1][idx2] = d;
    }

    public T get(int i, int j) {
        int idx1 = Math.min(i, j);
        int idx2 = Math.max(i, j);
        idx2 -= idx1;
        return (T) matrix[idx1][idx2];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix[0].length; i++) {
            sb.append(String.format("%3d: ", i));
            for (int j = 0; j < matrix[0].length; j++) {
                sb.append(String.format("%s ", get(i, j)));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    public T[] reduction(Reducer<T> r) {
        Object[] result = new Object[matrix[0].length];
        for (int i = 0; i < result.length; i++) {
            result[i] = get(0, i);
            for (int j = 1; j < result.length; j++) {
                result[i] = r.apply((T) result[i], get(i, j));
            }
        }
        return (T[]) result;
    }

    @FunctionalInterface
    public interface Reducer<T> extends BiFunction<T, T, T>{};

    public Reducer<T> max(Comparator<T> comparator) {
        return (result, next) -> comparator.compare(result, next) > 0 ? result : next;
    }

    public Reducer<T> min(Comparator<T> comparator) {
        return (result, next) -> comparator.compare(result, next) <= 0 ? result : next;
    }

}
