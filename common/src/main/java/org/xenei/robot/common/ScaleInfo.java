package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.PrecisionModel;
import org.xenei.robot.common.utils.DoubleUtils;

public final class ScaleInfo {

    private static final double DEFAULT_RESOLUTION = 0.5;
    private static final double DEFAULT_SCALE = 1.0;

    public static final ScaleInfo DEFAULT = new ScaleInfo(DEFAULT_RESOLUTION, DEFAULT_SCALE);

    public static ScaleInfo.Builder builder() {
        return new Builder();
    }

    @FunctionalInterface
    public interface OpFunc {
        boolean test(double a, double b, double tolerance);
    }

    public enum OP {
        EQ((double a, double b, double tolerance) -> Math.abs(a - b) <= tolerance),
        LT((double a, double b, double tolerance) -> a + tolerance < b),
        GT((double a, double b, double tolerance) -> a  > b + tolerance),
        NE((double a, double b, double tolerance) -> Math.abs(a - b) > tolerance),
        LE((double a, double b, double tolerance) -> a + tolerance <= b),
        GE((double a, double b, double tolerance) -> a >= b + tolerance);

        private final OpFunc func;
        OP(OpFunc func) {
            this.func = func;
        }

        boolean exec(double a, double b, double tolerance) {
            return func.test(a, b, tolerance);
        }
    }

    /**
     * The smallest change recorded on a map.
     */
    private final double resolution;
    /**
     * How much the map is scaled from 1:1
     */
    private final double scale;
    /**
     * The number of decimal places in the calculations.
     */
    private final int decimalPlaces;
    /**
     * = 10 ^ decimalPlaces.  Values are calculated at value * truncationFactor
     * and then reduced to Values / truncationFactor and rounded at decimalPlaces.
     */
    private final double truncationFactor;
    /**
     * resolution * truncationFactor.
     * Used to force values into cells on a map.
     */
    private final int modulusFactor;
    /**
     * For graphing calculations.
     */
    private final PrecisionModel precisionModel;

    /**
     * Constructor.
     *
     * @param resolution
     *            the resolution of the map in meters.
     * @param scale
     *            the scale of the map in meters.
     */
    private ScaleInfo(double resolution, double scale) {
        this.scale = scale;
        this.resolution = resolution;
        this.decimalPlaces = (int) Math.ceil(Math.log10(1 / resolution));
        this.truncationFactor = Math.pow(10, decimalPlaces);
        this.modulusFactor = (int) (resolution * truncationFactor);
        this.precisionModel = new PrecisionModel(100 * truncationFactor);
    }

    @Override
    public String toString() {
        return String.format("ScaleInfo[r: %s, s:%s]", resolution, scale);
    }

    /**
     * Gets the resolution of this map.
     *
     * @return the resoluiton of the map.
     */
    public double getResolution() {
        return resolution;
    }

    /**
     * Determines if 2 doubles are equivalent within the resolution of the
     * scale.
     *
     * @param a
     *            the double.
     * @param b
     *            the double.
     * @return {@code true} if the values are the same within the resolution
     *         the scale.
     */
    public boolean compare(OP op, double a, double b) {
        return op.exec(a, b, resolution);
    }

    /**
     * Determines if 2 coordinates are equivalent within the resolution of the
     * scale.
     *
     * @param a
     *            the coordinate.
     * @param b
     *            the coordinate.
     * @return {@code true} if the values are the same within the resolution
     *         the scale.
     */
    public boolean compare(OP op, Coordinate a, Coordinate b) {
        if (op == OP.EQ) {
            return op.exec(a.x, b.x, resolution) && op.exec(a.y, b.y, resolution);
        } else {
            return op.exec(a.x, b.x, resolution) || op.exec(a.y, b.y, resolution);
        }
    }

    /**
     * Determines if 2 coordinates are equivalent within the resolution of the
     * scale.
     *
     * @param a
     *            the coordinate.
     * @param b
     *            the coordinate.
     * @return {@code true} if the values are the same within the resolution
     *         the scale.
     */
    public boolean compare(OP op, Location a, Location b) {
        if (op == OP.EQ) {
            return op.exec(a.getX(), b.getX(), resolution) && op.exec(a.getY(), b.getY(), resolution);
        } else {
            return op.exec(a.getX(), b.getX(), resolution) || op.exec(a.getY(), b.getY(), resolution);
        }
    }


    /**
     * Gets the number of decimal places in the display.
     *
     * @return the number of decimal places in the display.
     */
    public int decimalPlaces() {
        return decimalPlaces;
    }

    public PrecisionModel getPrecisionModel() {
        return precisionModel;
    }

    /**
     * Rounds the double to the number of specified decimal places.
     *
     * @param d
     *            the number to truncate.
     * @return The rounded value.
     */
    public double round(double d) {
        return DoubleUtils.round(d, decimalPlaces);
    }

    /**
     * Rounds the position coordinates and heading to the specified decimal places.
     *
     * @param pos
     *            the original position
     * @return the position with rounded positions.
     */
    public Position round(Position pos) {
        return pos.isInfinite()
                ? pos
                : Position.asPosition(new Coordinate(round(pos.getX()), round(pos.getY())), round(pos.heading()));
    }

    /**
     * Rounds the location coordinates to the specified decimal places.
     *
     * @param location
     *            the original location
     * @return the position with rounded positions.
     */
    public Location round(Location location) {
        return location.isInfinite() ? location : Location.asLocation(round(location.getCoordinate()));
    }

    /**
     * Rounds the coordinates to the specified decimal places.
     *
     * @param coord
     *            the original coordinate
     * @return the coordinate with rounded values.
     */
    public Coordinate round(Coordinate coord) {
        return new Coordinate(round(coord.getX()), round(coord.getY()));
    }

    /**
     * Puts the value within a cell on a map.
     *
     * @param value
     *            the value to scale.
     * @return the value the scaled value.
     */
    public double scale(double value) {
        long scaledValue = (long) Math.floor((Math.abs(value) * scale * truncationFactor) + (modulusFactor / 2.0));
        scaledValue -= scaledValue % modulusFactor;
        if (value < 0) {
            scaledValue *= -1;
        }
        return DoubleUtils.round(scaledValue / truncationFactor, decimalPlaces);
    }

    public Coordinate scale(Coordinate coordinate) {
        return new Coordinate(scale(coordinate.getX()), scale(coordinate.getY()));
    }

    public static class Builder {
        private double resolution = DEFAULT_RESOLUTION;
        private double scale = DEFAULT_SCALE;

        /**
         * Resolution of the sale in meters. (e.g. centimeter resolution would be 0.01);
         *
         * @param resolution
         *            the resolution of this scale.
         * @return the builder for chaining.
         */
        public Builder setResolution(double resolution) {
            this.resolution = resolution;
            return this;
        }

        public Builder setScale(double scale) {
            this.scale = scale;
            return this;
        }

        public ScaleInfo build() {
            return new ScaleInfo(this.resolution, this.scale);
        }
    }

}
