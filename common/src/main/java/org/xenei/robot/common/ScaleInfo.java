package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.PrecisionModel;
import org.xenei.robot.common.utils.DoubleUtils;

public final class ScaleInfo {

    private static double DEFAULT_RESOLUTION = 0.5;
    private static double DEFAULT_SCALE = 1.0;

    public static final ScaleInfo DEFAULT = new ScaleInfo(DEFAULT_RESOLUTION, DEFAULT_SCALE);

    public static ScaleInfo.Builder builder() {
        return new Builder();
    }

    private final double resolution;
    private final double scale;
    private final int decimalPlaces;
    private final double truncationFactor;
    private final int modulusFactor;
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

    /**
     * Gets the resolution of this map.
     *
     * @return the resoluiton of the map.
     */
    public double getResolution() {
        return resolution;
    }

    /**
     * Determines if 2 coordinates are equivalent within the resolution of the
     * scale.
     *
     * @param a
     *            the first coordinate.
     * @param b
     *            the second coordinate.
     * @return {@code true} if the coordinates are the same within the resolution
     *         the scale.
     */
    public boolean areEquivalent(FrontsCoordinate a, FrontsCoordinate b) {
        return a.equals2D(b, resolution);
    }

    @Deprecated
    public double getHalfResolution() {
        return resolution / 2;
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
    public <P extends PositionI<?, P>> P round(P pos) {
        return pos.isInfinite()
                ? pos
                : pos.buildPosition(new Coordinate(round(pos.getX()), round(pos.getY())), round(pos.getHeading()));
    }

    /**
     * Rounds the location coordinates to the specified decimal places.
     *
     * @param location
     *            the original location
     * @return the position with rounded positions.
     */
    public <L extends LocationI<L>> L round(L location) {
        return location.isInfinite() ? location : location.buildLocation(round(location.getCoordinate()));
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
