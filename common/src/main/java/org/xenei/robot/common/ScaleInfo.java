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

    private ScaleInfo(double resolution, double scale) {
        this.scale = scale;
        this.resolution = resolution;
        this.decimalPlaces = (int) Math.ceil(Math.log10(1 / resolution));
        this.truncationFactor = Math.pow(10, decimalPlaces);
        this.modulusFactor = (int) (resolution * truncationFactor);
        this.precisionModel = new PrecisionModel( 100*truncationFactor);
    }

    public double getResolution() {
        return resolution;
    }

    public double getHalfResolution() {
        return resolution / 2;
    }

    public int decimalPlaces() {
        return decimalPlaces;
    }
    
    public PrecisionModel getPrecisionModel() {
        return precisionModel;
    }

    /**
     * Rounds the double to the number of specified decimal places.
     * @param d the number to truncate.
     * @return The truncated value.
     */
    public double precise(double d) {
        return DoubleUtils.round(d, decimalPlaces);
    }

    /**
     * Rounds the coordinates to the specified decimal places.
     * @param c the original coordinate
     * @return the coordinate with truncated positions.
     */
    public Coordinate precise(Coordinate c) {
        return new Coordinate(precise(c.getX()),precise(c.getY()));

    }

    /**
     * Puts the value within a cell on a map.
     * @param value
     * @return
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
         * @param resolution the resolution of this scale.
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
