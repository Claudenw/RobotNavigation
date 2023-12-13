package org.xenei.robot.common;

import org.locationtech.jts.geom.Coordinate;

public class ScaleInfo {

    public static double M_SCALE = 1.0;
    public static double CM_SCALE = 1.0 / 100;

    private static double DEFAULT_BUFFER = .5;
    private static double DEFAULT_RESOLUTION = 0.001;
    
    public static ScaleInfo DEFAULT = new ScaleInfo(M_SCALE, DEFAULT_BUFFER, DEFAULT_RESOLUTION);
    
    private double scale;
    private double buffer ;
    private double resolution ;
    
    private ScaleInfo(double scale, double buffer, double resolution) {
        this.scale = scale;
        this.buffer = buffer;
        this.resolution = resolution;
    }
    
    public double getScale() {
        return scale;
    }
    public double getBuffer() {
        return buffer;
    }
    public double getResolution() {
        return resolution;
    }
    
    public int decimalPlaces() {
        return (int) Math.log10( 1/getResolution() );
    }
    
    public double getTolerance() {
        return resolution+buffer;
    }
    
    public static class Builder {
        private double scale = CM_SCALE;
        private double buffer = DEFAULT_BUFFER;
        private double resolution = DEFAULT_RESOLUTION;
        
        public Builder setScale(double scale) {
            this.scale = scale;
            return this;
        }
        public Builder setBuffer(double buffer) {
            this.buffer = buffer;
            return this;
        }
        
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
            return new ScaleInfo(this.scale, this.buffer, this.resolution);
        }
    }
    
}
