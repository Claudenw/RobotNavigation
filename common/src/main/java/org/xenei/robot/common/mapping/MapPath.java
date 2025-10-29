package org.xenei.robot.common.mapping;

import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.common.GeometricObject;
import org.xenei.robot.common.planning.Segment;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class MapPath implements GeometricObject, MapObject {
    private final Map onMap;
    private final List<MapLocation> locations;
    private final Geometry geometry;

    MapPath(Map onMap, Stream<MapLocation> locationStream) {
        this.onMap = onMap;
        this.locations = new ArrayList<>();
        this.geometry = onMap.getContext().geometryUtils.asLine(
                locationStream.peek(locations::add));
        onMap.storage.addPath(locations);
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * Finds the segment that we are on.
     * @param startCoordinate the coordinate to start at.
     * @return the setment or null.
     */
    public Optional<Segment> getSegment(MapCoordinate startCoordinate) {
        for (int i = 0; i < locations.size() -1; i++) {
            if (locations.get(i).sameCoordinate(startCoordinate)) {
                MapLocation start = locations.get(i);
                MapLocation next = locations.get(i+1);
                double distance = start.distance(next);
                return Optional.of(new Segment(next, distance, distance));
            }
        }
        return Optional.empty();
    }

    @Override
    public Map getMap() {
        return onMap;
    }
}
