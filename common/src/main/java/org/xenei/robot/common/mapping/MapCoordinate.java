package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.Location;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;

public class MapCoordinate implements MapObject, GeometricObject, Comparable<MapCoordinate>, Location {

    private final Map onMap;
    private final UnmodifiableCoordinate coordinate;

    /**
     * Coordinates that are on the map.
     * @param onMap
     * @param mapCoordinate
     */
    MapCoordinate(Map onMap, Coordinate mapCoordinate) {
        this.onMap = onMap;
        this.coordinate = UnmodifiableCoordinate.make(mapCoordinate);
    }

    public Map getMap() {
        return onMap;
    }

    public final RobutContext getContext() {
        return onMap.getContext();
    }

    public final UnmodifiableCoordinate getCoordinate() {
        return coordinate;
    }

    public final boolean sameCoordinates(Location other) {
        return getMap().getContext().scaleInfo.areEquivalent(this, other);
    }

    public final boolean sameCoordinates(Coordinate other) {
        return getMap().getContext().scaleInfo.areEquivalent(this.getCoordinate(), other);
    }

    public String toString() {
        return LocationUtils.toString(this);
    }

    @Override
    public Geometry getGeometry() {
        return onMap.getContext().geometryFactory.createPoint(getCoordinate());
    }

    public final MapCoordinate minus(Location location) {
        return new MapCoordinate(onMap, CoordUtils.minus(this.getCoordinate(), location.getCoordinate()));
    }

    public final MapCoordinate plus(Location location) {
        return new MapCoordinate(onMap, CoordUtils.plus(this.getCoordinate(), location.getCoordinate()));
    }

    public final MapCoordinate nextCoordinate(double heading, double scaledRange) {
        return nextCoordinate(new ThetaAndRange(heading, scaledRange));
    }

    public final MapCoordinate nextCoordinate(Location relativeLocation) {
        return this.plus(relativeLocation);
    }


    public MapCoordinate relativeLocation(Location absoluteLocation) {
        return getMap().asMapCoordinate(Location.LocationUtils.relativeLocation(this, getMap().asMapLocation(absoluteLocation)));
    }

    public final boolean clearPath(Location location) {
        return getMap().isClearPath(this, onMap.asMapLocation(location));
    }

    @Override
    public int compareTo(MapCoordinate other) {
        if (getMap().getContext().scaleInfo.areEquivalent(this, other)) {
            return 0;
        }
        return getCoordinate().compareTo(other.getCoordinate());
    }

}
