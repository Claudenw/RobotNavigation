package org.xenei.robot.utils;

import org.xenei.robot.navigation.Coordinates;

public class ActiveMap {
    long[] map = new long[64];

    public static final int dim = 64;
    public static final int halfdim = 32;
    public static final int maxRange = (int) Math.round(Math.sqrt(dim * dim * 2));

    final int scale;

    private static int fitRange(long x) {
        return x > Integer.MAX_VALUE ? Integer.MAX_VALUE : (x < Integer.MIN_VALUE ? Integer.MIN_VALUE : (int) x);
    }

    public ActiveMap(int scale) {
        this.scale = scale;
    }

    public int scale() {
        return scale;
    }

    private Coord adjust(Coordinates location) {
        return new Coord(location.getX(), location.getY());
    }

    public void enable(Coordinates location) {
        Coord where = adjust(location);
        if (where.inRange())
            map[where.x] |= 1L << where.y;
    }

    public void disable(Coordinates location) {
        Coord where = adjust(location);
        if (where.inRange())
            map[where.x] &= ~(1L << where.y);
    }

    public boolean isEnabled(Coordinates location) {
        Coord where = adjust(location);
        if (where.inRange()) {
            return (map[where.x] & (1L << where.y)) != 0;
        }
        return false;
    }

    public void shift(Coordinates movement) {
        Coord shift = new Coord(movement);
        if (Math.abs(shift.x) >= dim || Math.abs(shift.y) >= dim) {
            map = new long[64];
            return;
        }
        if (shift.x < 0) {
            int limit = dim + shift.x;
            // shift the rows up.
            for (int i = 0; i < limit; i++) {
                map[i] = map[i - shift.x];
            }
            for (int i = limit; i < dim; i++) {
                map[i] = 0l;
            }
        }
        if (shift.x > 0) {
            // shift rows down
            for (int i = dim - 1; i >= shift.x; i--) {
                map[i] = map[i - 1];
            }
            for (int i = shift.x - 1; i >= 0; i--) {
                map[i] = 0;
            }
        }
        if (shift.y > 0) {
            for (int i = 0; i < dim; i++) {
                map[i] = map[i] << shift.y;
            }
        }

        if (shift.y < 0) {
            int s = shift.y * -1;
            for (int i = 0; i < dim; i++) {
                map[i] = map[i] >>> s;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = dim - 1; i >= 0; i--) {
            for (int j = 0; j < 64; j++) {
                sb.append((map[i] & (1L << j)) == 0 ? " " : "X");
            }
            sb.append("\n");
        }
        return sb.toString();

    }

    // a location in the map
    public class Coord {
        public final int x;
        public final int y;

        Coord(double x, double y) {
            this.x = fitRange((long) Math.ceil(x / scale) + halfdim);
            this.y = fitRange((long) Math.ceil(y / scale) + halfdim);
        }

        public Coord(Coordinates movement) {
            this.x = fitRange((long) Math.ceil(movement.getX() / scale));
            this.y = fitRange((long) Math.ceil(movement.getY() / scale));
        }

        public boolean inRange() {
            return x < dim && x >= 0 && y < dim && y >= 0;
        }

        @Override
        public String toString() {
            return String.format("Coord[%s,%s]", x, y);
        }

    }

}
