package org.xenei.robot.common;

import org.apache.commons.math3.util.Precision;
import org.locationtech.jts.geom.Coordinate;
import org.xenei.robot.common.utils.DoubleUtils;

public class ScaleInfo {

    private static double DEFAULT_RESOLUTION = 0.5;
    
    public static ScaleInfo DEFAULT = new ScaleInfo(DEFAULT_RESOLUTION);
    public static ScaleInfo.Builder builder() { return new Builder(); }
 

    private final double resolution ;
    private final int decimalPlaces;
    
    private ScaleInfo(double resolution) {
        this.resolution = resolution;
        this.decimalPlaces = (int) Math.ceil(Math.log10( 1/resolution ));
    }

    public double getResolution() {
        return resolution;
    }
    
    public int decimalPlaces() {
        return decimalPlaces;
    }
    
    public static class Builder {
        private double resolution = DEFAULT_RESOLUTION;
        
        
        /**
         * Resolution of the sale in meters.
         * (e.g. centimeter resolution would be 0.01);
         * @param resolution the resolution of this scale.
         * @return the builder for chaining.
         */
        public Builder setResolution(double resolution) {
            this.resolution = resolution;
            return this;
        }
        
        public ScaleInfo build() {
            return new ScaleInfo(this.resolution);
        }
    }
    
    public double scale(double value) {
        double l =  Math.floor(value / resolution);
        double x = DoubleUtils.truncate(l*resolution, decimalPlaces());
        return x;
    }
    
}
