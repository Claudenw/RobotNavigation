package org.xenei.robot.mapper.rdf.functions;

public class GeometryException extends RuntimeException {

    public GeometryException() {
        
    }

    public GeometryException(String message) {
        super(message);
    }

    public GeometryException(Throwable cause) {
        super(cause);
    }

    public GeometryException(String message, Throwable cause) {
        super(message, cause);
    }

    public GeometryException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
