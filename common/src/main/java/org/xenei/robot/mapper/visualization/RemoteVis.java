package org.xenei.robot.mapper.visualization;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.xenei.robot.common.Position;
import org.xenei.robot.common.mapping.Map;
import org.xenei.robot.common.planning.Solution;
import org.xenei.robot.common.utils.GeometryUtils;

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class RemoteVis {
    private final HttpServer server;
    private final Map map;
    private final RemoteVizLib vizLib;
    private final Supplier<Solution> solutionSupplier;
    private final Supplier<Position> positionSupplier;
    private final Supplier<Coordinate> targetSupplier;
    private final int scale = 100;

    public RemoteVis(Map.VisualizationInitializer initializer) throws IOException {
        this(initializer.map(), initializer.solutionSupplier(), initializer.positionSupplier(), initializer.targetSupplier());
    }

    public RemoteVis(Map map, Supplier<Solution> solutionSupplier, Supplier<Position> positionSupplier,
              Supplier<Coordinate> targetSupplier) throws IOException {
        this.map = map;
        this.solutionSupplier = solutionSupplier;
        this.positionSupplier = positionSupplier;
        this.targetSupplier = targetSupplier;
        vizLib = new RemoteVizLib(700, 700, map.getContext().geometryUtils);

        // setup and run the server
        Executor executor = new Executor() {
            @Override
            public void execute(Runnable command) {
                map.getContext().submit(command);
            }
        };
        server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0);
        server.createContext("/map", new  MapHandler());
        server.setExecutor(executor);
        server.start();
    }

    class MapHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParamValue = null;
            if ("GET".equals(httpExchange.getRequestMethod())) {
                handleGetRequest(httpExchange);

            }
            handle400Error(httpExchange);
        }

        private void handle400Error(HttpExchange httpExchange) throws IOException {
            httpExchange.sendResponseHeaders(400, 0);
        }

        private void handleGetRequest(HttpExchange httpExchange) throws IOException {
            StringBuilder xmlBuilder = new StringBuilder();
            xmlBuilder.append(
                    String.format("<!DOCTYPE html>\n<html><body><svg width=\"%d\" height=\"%d\" xmlns=\"http://www.w3.org/2000/svg\">",
                            vizLib.xDim, vizLib.yDim));
            vizLib.draw(map, solutionSupplier, positionSupplier, targetSupplier)
                    .forEach(xmlBuilder::append);
            xmlBuilder.append("</svg></body></html>");
            byte[] result = xmlBuilder.toString().getBytes(StandardCharsets.UTF_8);
           // httpExchange.getResponseHeaders().add("Content-Type", "test/html");  // "image/svg+xml"
            httpExchange.sendResponseHeaders(200, result.length);
            try (OutputStream outputStream = httpExchange.getResponseBody()) {
                outputStream.write(result);
                outputStream.flush();
            }
        }
    }

    abstract class RemoteDrawingCommand extends VisualizationLibrary.AbstractDrawingCommand {
        protected final String colorCode;
        protected final Geometry geom;
        private final BoundingBox boundingBox;

        RemoteDrawingCommand(BoundingBox boundingBox, Geometry geom, Color color) {
            super(geom, color);
            this.geom = geom;
            colorCode = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
            this.boundingBox = boundingBox;
        }

        protected String points() {
            Coordinate[] coords = geom.getCoordinates();
            return String.format("points=\"%s\"", Arrays.stream(coords)
                    .map(boundingBox::bound)
                    .map(c -> String.format("%f,%f", c.getX(), c.getY()))
                    .collect(Collectors.joining(" ")));
        }
    }

    class RemoteVizLib extends VisualizationLibrary<RemoteDrawingCommand>{
        private final int xDim;
        private final int yDim;
        private BoundingBox boundingBox;

        protected RemoteVizLib(int xDim, int yDim, GeometryUtils geometryUtils) {
            super(geometryUtils);
            this.xDim = xDim;
            this.yDim = yDim;
        }

        @Override
        public List<RemoteDrawingCommand> draw(Map map, Supplier<Solution> solutionSupplier, Supplier<Position> positionSupplier, Supplier<Coordinate> targetSupplier) {
            boundingBox = new BoundingBox(xDim, yDim, positionSupplier.get(), map.getContext().geometryUtils);
            List<RemoteDrawingCommand> result = super.draw(map, solutionSupplier, positionSupplier, targetSupplier);
            result.add(drawLine(boundingBox.polygon, Color.BLACK));
            return result;
        }

        @Override
        protected RemoteDrawingCommand drawPoint(Point geom, Color color) {
            return new RemoteDrawingCommand(boundingBox, geom, color) {
                @Override
                public String toString() {
                    if (boundingBox.polygon.intersects(geom)) {
                        Coordinate c = boundingBox.bound(geom.getCoordinate());
                        return String.format("<circle r=\"%f\" cx=\"%f\" cy=\"%f\" stroke=\"%s\" stroke-width=\"3\" fill=\"%s\" />",
                                map.getContext().chassisInfo.radius, c.getX(), c.getY(), colorCode, colorCode);
                    }
                    return "";
                }
            };
        }

        @Override
        protected RemoteDrawingCommand drawPolygon(Polygon geom, Color color) {
            return new RemoteDrawingCommand(boundingBox, geom, color) {
                @Override
                public String toString() {
                    if (boundingBox.polygon.intersects(geom)) {
                        return String.format("<polygon %s style=\"fill:%s;stroke:%s\" />",
                                points(), colorCode, colorCode);
                    }
                    return "";
                }
            };
        }

        @Override
        protected RemoteDrawingCommand drawLine(Geometry geom, Color color) {
            return new RemoteDrawingCommand(boundingBox, geom, color) {
                @Override
                public String toString() {
                    if (boundingBox.polygon.intersects(geom)) {
                        return String.format("<polyline %s style=\"fill:none;stroke:%s;stroke-width=3;\" />",
                                points(), colorCode);
                    }
                    return "";
                }
            };
        }

        @Override
        protected RemoteDrawingCommand drawString(Geometry geom, Color color) {
            // <text x="75" y="60" font-size="30" text-anchor="middle" fill="red">SVG</text>
            return new RemoteDrawingCommand(boundingBox, geom, color) {
                @Override
                public String toString() {
                    if (boundingBox.polygon.intersects(geom)) {
                        Coordinate c = boundingBox.bound(geom.getCoordinate());
                        return String.format("<text x=\"%f\" y=\"%f\" fill=\"%s\">%s</text>",
                                c.getX(), c.getY(), colorCode, geom.getClass().getSimpleName());
                    }
                    return "";
                }
            };
        }
    }

    private class BoundingBox {
        Polygon polygon;
        double maxX;
        double maxY;

        BoundingBox(int xDim, int yDim, Position position, GeometryUtils geometryUtils) {
            double dimX = 1.0 * xDim / scale;
            double dimY = 1.0 * yDim / scale;
            double halfX = dimX / 2;
            double halfY = dimY / 2;
            double minx = position.getX() - halfX;
            double miny = position.getY() - halfY;
            polygon = geometryUtils.asPolygon(new CoordinateXY(minx, miny),
                    new CoordinateXY(minx+dimX, miny),
                    new CoordinateXY(minx+dimX, miny+dimY),
                    new CoordinateXY(minx, miny+dimY),
                    new CoordinateXY(minx, miny));
            maxX = position.getX() + halfX * scale;
            maxY = position.getY() + halfY * scale;
        }

        Coordinate bound(Coordinate coord) {
            double x = maxX - coord.getX() * scale;
            double y = maxY - coord.getY() * scale;
            return new CoordinateXY(x, y);
        }
    }
}
