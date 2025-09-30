// package org.xenei.robot.common.testUtils;
//
// import java.util.UUID;
//
// import org.apache.commons.lang3.NotImplementedException;
// import org.apache.jena.rdf.model.Literal;
// import org.apache.jena.rdf.model.Resource;
// import org.locationtech.jts.geom.Geometry;
// import org.xenei.robot.common.mapping.Map;
//
// public class MockObstacleFactory {
//
// private MockObstacleFactory() {
// }
//
// public static Map.Obstacle from(Literal wkt) {
// return new Map.Obstacle() {
//
// @Override
// public Literal wkt() {
// return wkt;
// }
//
// @Override
// public Geometry geometry() {
// throw new NotImplementedException();
// }
//
// @Override
// public UUID uuid() {
// throw new NotImplementedException();
// }
//
// @Override
// public Resource rdf() {
// throw new NotImplementedException();
// }
//
// @Override
// public int hashCode() {
// return Obstacle.hashCode(this);
// }
//
// @Override
// public boolean equals(Object o) {
// return Obstacle.equalsImpl(this, o);
// }
//
// };
//
// }
//
// }
