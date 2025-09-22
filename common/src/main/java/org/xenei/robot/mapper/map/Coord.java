package org.xenei.robot.mapper.map;

import org.apache.jena.arq.querybuilder.AskBuilder;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.Var;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.vocabulary.RDF;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xenei.robot.common.UnmodifiableCoordinate;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.utils.CoordUtils;
import org.xenei.robot.common.utils.RobutContext;
import org.xenei.robot.mapper.rdf.Namespace;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * The map coordinates for this implementation.
 */
public class Coord implements Map.MapCoordinate {

	record TargetData(Coord target, double distance, boolean indirect) {
	};

	private static final Logger LOG = LoggerFactory.getLogger(Coord.class);
	private static final String urnFmt = "java:org.xenei.robot.mapper.map.Coord:%s:%s";
	private static final Var visitedV = Var.alloc("visited");
	private static final Var distanceV = Var.alloc("distance");
	private static final Var indirectV = Var.alloc("indirect");
	private static final Var targetV = Var.alloc("target");

	private final MapImpl map;
	private final Resource urn;
	private final UnmodifiableCoordinate coord;
	private final Literal wkt;
	private final Geometry geometry;
	private boolean visited;
	private boolean exists;
	private CompletableFuture<?> future = CompletableFuture.completedFuture(null);
	/**
	 *
	 */
	private final HashMap<Coord, TargetData> targets = new HashMap<>();

	Coord(MapImpl map, Coordinate coordinate) {
		this.map = map;
		double multiplier = Math.pow(10, map.getContext().scaleInfo.decimalPlaces());
		long lX = (long) (coordinate.x * multiplier);
		long lY = (long) (coordinate.x * multiplier);
		coord = UnmodifiableCoordinate.make(coordinate);
		geometry = map.getContext().geometryUtils.asPoint(coordinate);
		wkt = map.getContext().graphGeomFactory.asWKT(geometry);
		urn = ModelFactory.createMemModelMaker().createDefaultModel().createResource(String.format(urnFmt, lX, lY));
		urn.addLiteral(Namespace.x, coord.x);
		urn.addLiteral(Namespace.y, coord.y);
		urn.addLiteral(Geo.AS_WKT_PROP, wkt);
		urn.addProperty(RDF.type, Namespace.Coord);
		visited = map.ask(new AskBuilder().addWhere(urn, Namespace.visited, visitedV));
		if (visited) {
			urn.addLiteral(Namespace.visited, visited);
		}
		exists = map.ask(askExists());
	}

	Geometry getGeometry() {
		return geometry;
	}

	MapImpl getMap() {
		return map;
	}

	CompletableFuture<?> removeTarget(Coord target) {
		target.removeTarget(target);
		return map.doUpdate(new UpdateRequest()
				.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn, Namespace.target, targetV)
						.addGraph(Namespace.PlanningModel, new SelectBuilder().addWhere(urn, Namespace.target, targetV)
								.addWhere(targetV, Namespace.point, target.wkt))
						.build()));
	}

	CompletableFuture<TargetData> addTarget(Coord target) {
		TargetData result = targets.get(target);
		if (result != null) {
			return CompletableFuture.completedFuture(result);
		}
		SelectBuilder sb = new SelectBuilder().addVar(distanceV).addVar(indirectV)
				.addWhere(urn, Namespace.target, targetV).addWhere(targetV, Namespace.point, target.wkt)
				.addOptional(targetV, Namespace.distance, distanceV)
				.addOptional(targetV, Namespace.isIndirect, indirectV);

		return map.exec(sb).thenApply(rs -> {
			Double distance = null;
			Boolean indirect = null;
			if (rs.hasNext()) {
				QuerySolution solution = rs.next();
				Literal literal = solution.getLiteral(distanceV.getVarName());
				distance = (literal == null) ? null : literal.getDouble();
				literal = solution.getLiteral(indirectV.getVarName());
				indirect = (literal == null) ? null : literal.getBoolean();
			}
			if (distance == null) {
				distance = distance(target.getCoordinate());
			}
			if (indirect == null) {
				indirect = !map.isClearPath(this, target);
			}
			TargetData td = new TargetData(target, distance, indirect);
			targets.put(target, td);
			return td;
		});
	}

	public Literal getWkt() {
		return wkt;
	}

	boolean isVisited() {
		return visited;
	}

	CompletableFuture<Coord> updateValue(Property property, Object value) {
		return map
				.doUpdate(new UpdateRequest()
						.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn, property, Namespace.o)
								.addGraph(Namespace.PlanningModel,
										new SelectBuilder().addWhere(urn, property, Namespace.o))
								.build())
						.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, urn, property, value).build()))
				.thenApply(n -> this);
	}

	CompletableFuture<Coord> setVisited() {
		if (!visited) {
			visited = true;
			urn.addLiteral(Namespace.visited, visited);
			return exists ? updateValue(Namespace.visited, true) : update();
		}
		return CompletableFuture.completedFuture(this);
	}

	@Override
	public UnmodifiableCoordinate getCoordinate() {
		return coord;
	}

	AskBuilder askExists() {
		return new AskBuilder().from(Namespace.UnionModel.getURI()).addWhere(urn, RDF.type, Namespace.Coord);
	}

	boolean exists() {
		return exists;
	}

	@Override
	public String toString() {
		return "Coord [" + CoordUtils.toString(coord, 1) + "]";
	}

	CompletableFuture<Coord> update() {
		UpdateRequest req = new UpdateRequest();
		req.add(new UpdateBuilder().addDelete(Namespace.PlanningModel, urn, Namespace.target, targetV)
				.addGraph(Namespace.PlanningModel, new SelectBuilder().addWhere(urn, Namespace.target, targetV))
				.build());
		org.apache.jena.rdf.model.Model model = ModelFactory.createDefaultModel();
		model.add(urn.getModel());
		for (TargetData target : targets.values()) {
			Resource targ = model.createResource();
			model.add(urn, Namespace.target, targ);
			targ.addLiteral(Namespace.point, target.target.wkt);
			targ.addLiteral(Namespace.distance, target.distance());
			targ.addLiteral(Namespace.isIndirect, target.indirect());
		}
		req.add(new UpdateBuilder().addInsert(Namespace.PlanningModel, model).build());
		return map.doUpdate(req).thenApply(rs -> this);
	}

	public boolean hasClearPath(Coord target) {
		RobutContext ctxt = map.getContext();
		Literal pathWkt = ctxt.graphGeomFactory.asWKTPath(ctxt.chassisInfo.radius, this.coord, target.coord);
		Var wkt = Var.alloc("wkt");

		boolean result = map.ask(new AskBuilder().from(Namespace.UnionModel.getURI()) //
				.addWhere(Namespace.s, RDF.type, Namespace.Obst) //
				.addWhere(Namespace.s, Geo.AS_WKT_PROP, wkt)
				.addFilter(map.exprF.eq(ctxt.graphGeomFactory.calcDistance(map.exprF, pathWkt, wkt), 0)));

		LOG.debug("checked clearView from {} to {}: {}", this, target, result);
		return result;
	}
}
