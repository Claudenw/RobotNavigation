package org.xenei.robot.common;

import org.locationtech.jts.geom.Geometry;

import java.util.UUID;

public interface Obstacle extends GeometricObject {

    static Obstacle asObstacle(UUID uuid, Geometry geom) {
        return new Obstacle() {
            public Geometry getGeometry() {
                return geom;
            }

            public UUID uuid() {
                return uuid;
            }

            @Override
            public String toString() {
                return ObstacleUtils.toString(this);
            }

            @Override
            public int hashCode() {
                return ObstacleUtils.hashCode(this);
            }

            @Override
            public boolean equals(Object obj) {
                return ObstacleUtils.equals(this, obj);
            }
        };
    }

    UUID uuid();

    final class ObstacleUtils {
        private ObstacleUtils() {}

        public static int hashCode(Obstacle obstacle) {
            return GeometricObject.hashCode(obstacle);
        }

        public static boolean equals(Obstacle obstacle, Object obj) {
            return obj instanceof Obstacle && GeometricObject.equals(obstacle, obj);
        }

        public static String toString(Obstacle obstacle) {
            return String.format("Obstacle[%s, %s]", obstacle.uuid(), obstacle.getGeometry());
        }
    }

}
