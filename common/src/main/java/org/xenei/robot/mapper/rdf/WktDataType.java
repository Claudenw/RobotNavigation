package org.xenei.robot.mapper.rdf;

import java.util.Arrays;
import java.util.Map;

import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.geosparql.implementation.datatype.WKTDatatype;
import org.apache.jena.geosparql.implementation.jts.CoordinateSequenceDimensions;
import org.apache.jena.geosparql.implementation.jts.CustomCoordinateSequence;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WktDataType extends BaseDatatype {

    private GeometryFactory factory;
    private Map<String, Geometry> cache;

    private enum SupportedTypes {
        point(Point.class), linestring(LineString.class), linearring(LinearRing.class), polygon(Polygon.class),
        multipoint(MultiPoint.class), multilinestring(MultiLineString.class), multipolygon(MultiPolygon.class),
        geometrycollection(GeometryCollection.class);

        Class<?> supporting;
        
        SupportedTypes(Class<?> supporting) {
            this.supporting = supporting;
        }
    };

    public WktDataType(GeometryFactory factory, Map<String, Geometry> cache) {
        super(URI);
        this.factory = factory;
        this.cache = cache;
        TypeMapper typeMapper = TypeMapper.getInstance();
        typeMapper.registerDatatype(this);
        for (SupportedTypes type : SupportedTypes.values()) {
            typeMapper.registerDatatype( new Wrapped(type));
        }
    }

    static final Logger LOGGER = LoggerFactory.getLogger(WKTDatatype.class);

    /**
     * The default WKT type URI.
     */
    public static final String URI = Geo.WKT;// Namespace.URI + ":datatype:wktLiteral";

    /**
     * A static instance of WKTDatatype.
     */
    static WktDataType INSTANCE;

    /**
     * Returns the java class which is used to represent value instances of this
     * datatype.
     */
    @Override
    public Class<?> getJavaClass() {
        return Geometry.class;
    }

    @Override
    public String unparse(Object geometry) {
        if (geometry instanceof Geometry) {
            Geometry geom = (Geometry) geometry;
            String result = geom.toText();
            String other = geom.toString();
            if (cache != null) {
                cache.put(result, geom);
            }
            return result;
        } 
        throw new DatatypeFormatException(
                "Object to unparse " + WktDataType.class.getSimpleName() + " is not a Geometry: " + geometry);
    }

    @Override
    public Geometry parse(String literalForm) throws DatatypeFormatException {
        if (cache != null) {
            Geometry geom = cache.get(literalForm);
            if (geom != null) {
                return geom;
            }
        }

        WKTTextSRS wktTextSRS = new WKTTextSRS(literalForm);

        String wktText = wktTextSRS.wktText;
        String goemetryType = "point";
        String dimension = "";
        String coordinates = null;

        if (!wktText.equals("")) {

            wktText = wktText.trim();
            wktText = wktText.toLowerCase();

            String[] parts = wktText.split("\\(", 2);

            String remainder;
            if (parts.length == 1) { // Check for "empty" keyword and remove.
                remainder = parts[0].replace("empty", "").trim();
            } else {
                int coordinatesStart = wktText.indexOf("(");
                coordinates = wktText.substring(coordinatesStart);
                remainder = parts[0].trim();
            }

            int firstSpace = remainder.indexOf(" ");

            if (firstSpace != -1) {
                goemetryType = remainder.substring(0, firstSpace);
                dimension = remainder.substring(firstSpace + 1);
            } else {
                goemetryType = remainder;
            }
        }

        if (wktTextSRS.srsURI != null) {
            LOGGER.warn("SRS specified in " + WktDataType.class.getSimpleName() + " is ignored: " + literalForm);
        }
        if (coordinates == null) {
            throw new DatatypeFormatException("coordinates must be specified in " + WktDataType.class.getSimpleName());
        }
        try {
            Geometry geom = buildGeometry(SupportedTypes.valueOf(goemetryType), coordinates, convertDimensionString(dimension));
            if (cache != null) {
                cache.put(literalForm, geom);
            }
            return geom;
        } catch (IllegalArgumentException e) {
            throw new DatatypeFormatException("Geometry type not supported: " + goemetryType);
        }
        
    }

    private static CoordinateSequenceDimensions convertDimensionString(String dimensionsString) {

        CoordinateSequenceDimensions dims;
        switch (dimensionsString) {
        case "zm":
            dims = CoordinateSequenceDimensions.XYZM;
            break;
        case "z":
            dims = CoordinateSequenceDimensions.XYZ;
            break;
        case "m":
            dims = CoordinateSequenceDimensions.XYM;
            break;
        default:
            dims = CoordinateSequenceDimensions.XY;
            break;
        }
        return dims;
    }

    private Geometry buildGeometry(SupportedTypes geometryType, String coordinates, CoordinateSequenceDimensions dims)
            throws DatatypeFormatException {

        try {
            Geometry geo = null;
            switch (geometryType) {
            case point:
                CustomCoordinateSequence pointSequence = new CustomCoordinateSequence(dims, clean(coordinates));
                geo = factory.createPoint(pointSequence);
                break;
            case linestring:
                CustomCoordinateSequence lineSequence = new CustomCoordinateSequence(dims, clean(coordinates));
                geo = factory.createLineString(lineSequence);
                break;
            case linearring:
                CustomCoordinateSequence linearSequence = new CustomCoordinateSequence(dims, clean(coordinates));
                geo = factory.createLinearRing(linearSequence);
                break;
            case polygon:
                geo = buildPolygon(dims, coordinates);
                break;
            case multipoint:
                CustomCoordinateSequence multiPointSequence = new CustomCoordinateSequence(dims, clean(coordinates));
                geo = factory.createMultiPoint(multiPointSequence);
                break;
            case multilinestring:
                geo = buildMultiLineString(dims, coordinates);
                break;
            case multipolygon:
                geo = buildMultiPolygon(dims, coordinates);
                break;
            case geometrycollection:
                geo = buildGeometryCollection(coordinates);
                break;
            }
            return geo;
        } catch (ArrayIndexOutOfBoundsException ex) {
            throw new DatatypeFormatException("Build WKT Geometry Exception - Type: " + geometryType + ", Coordinates: "
                    + coordinates + ". " + ex.getMessage());
        }
    }

    private String clean(String unclean) {
        return unclean.replace(")", "").replace("(", "").trim();
    }

    private Geometry buildMultiLineString(CoordinateSequenceDimensions dims, String coordinates) {

        if (coordinates.isEmpty()) {
            return factory.createMultiLineString(new LineString[0]);
        }

        String[] splitCoordinates = splitCoordinates(coordinates);
        LineString[] lineStrings = splitLineStrings(dims, splitCoordinates);
        return factory.createMultiLineString(lineStrings);
    }

    private Geometry buildMultiPolygon(CoordinateSequenceDimensions dims, String coordinates) {

        if (coordinates.isEmpty()) {
            return factory.createMultiPolygon(new Polygon[0]);
        }

        String trimmed = coordinates.replace(")) ,", ")),");
        String[] multiCoordinates = trimmed.split("\\)\\),");
        Polygon[] polygons = new Polygon[multiCoordinates.length];
        for (int i = 0; i < multiCoordinates.length; i++) {
            polygons[i] = buildPolygon(dims, multiCoordinates[i]);
        }

        return factory.createMultiPolygon(polygons);
    }

    private Polygon buildPolygon(CoordinateSequenceDimensions dims, String coordinates) {

        Polygon polygon;

        String[] splitCoordinates = splitCoordinates(coordinates);
        if (splitCoordinates.length == 1) { // Polygon without holes.
            CustomCoordinateSequence shellSequence = new CustomCoordinateSequence(dims, clean(coordinates));
            polygon = factory.createPolygon(shellSequence);
        } else { // Polygon with holes
            String shellCoordinates = splitCoordinates[0];

            CustomCoordinateSequence shellSequence = new CustomCoordinateSequence(dims, clean(shellCoordinates));
            LinearRing shellLinearRing = factory.createLinearRing(shellSequence);

            String[] splitHoleCoordinates = Arrays.copyOfRange(splitCoordinates, 1, splitCoordinates.length);
            LinearRing[] holesLinearRing = splitLinearRings(dims, splitHoleCoordinates);

            polygon = factory.createPolygon(shellLinearRing, holesLinearRing);

        }
        return polygon;
    }

    private Geometry buildGeometryCollection(String coordinates) throws DatatypeFormatException {

        if (coordinates.isEmpty()) {
            return factory.createGeometryCollection(new Geometry[0]);
        }

        // Split coordinates
        String tidied = coordinates.substring(1, coordinates.length() - 1);
        tidied = tidied.replaceAll("[\\ ]?,[\\ ]?", ","); // Remove spaces around commas
        String[] partCoordinates = tidied.split("\\),(?=[^\\(])"); // Split whenever there is a ), but not ),(

        Geometry[] geometries = new Geometry[partCoordinates.length];

        for (int i = 0; i < partCoordinates.length; i++) {
            geometries[i] = parse(partCoordinates[i]);
        }
        return factory.createGeometryCollection(geometries);
    }

    private String[] splitCoordinates(String coordinates) {

        String trimmed = coordinates.replace(") ,", "),");
        return trimmed.split("\\),");

    }

    private LineString[] splitLineStrings(CoordinateSequenceDimensions dims, String[] splitCoordinates) {

        LineString[] lineStrings = new LineString[splitCoordinates.length];

        for (int i = 0; i < splitCoordinates.length; i++) {
            CustomCoordinateSequence sequence = new CustomCoordinateSequence(dims, clean(splitCoordinates[i]));
            LineString lineString = factory.createLineString(sequence);
            lineStrings[i] = lineString;
        }

        return lineStrings;

    }

    private LinearRing[] splitLinearRings(CoordinateSequenceDimensions dims, String[] splitCoordinates) {

        LinearRing[] linearRings = new LinearRing[splitCoordinates.length];

        for (int i = 0; i < splitCoordinates.length; i++) {
            CustomCoordinateSequence sequence = new CustomCoordinateSequence(dims, clean(splitCoordinates[i]));
            LinearRing linearRing = factory.createLinearRing(sequence);
            linearRings[i] = linearRing;
        }

        return linearRings;

    }

    private static class WKTTextSRS {

        private final String wktText;
        private final String srsURI;

        public WKTTextSRS(String wktLiteral) {
            int startSRS = wktLiteral.indexOf("<");
            int endSRS = wktLiteral.indexOf(">");

            // Check that both chevrons are located and extract SRS_URI name, otherwise
            // default.
            if (startSRS != -1 && endSRS != -1) {
                srsURI = wktLiteral.substring(startSRS + 1, endSRS);
                wktText = wktLiteral.substring(endSRS + 1);
            } else {
                srsURI = null;
                wktText = wktLiteral;
            }
        }

        public String getWktText() {
            return wktText;
        }

        public String getSrsURI() {
            return srsURI;
        }

    }
    
    private class Wrapped extends BaseDatatype {
        SupportedTypes type;
        
        Wrapped(SupportedTypes type) {
            super(Namespace.URI+":SupportedTypes:" + type);
            this.type = type;
        }
          @Override
          public RDFDatatype normalizeSubType(Object value, RDFDatatype dt) {
              return WktDataType.this;
          }
          @Override
        public Class<?> getJavaClass() {
            return type.supporting;
        }

        @Override
          public String unparse(Object geometry) {
              return WktDataType.this.unparse(geometry);
          }
          @Override
          public Geometry parse(String literalForm) throws DatatypeFormatException {
              return WktDataType.this.parse(literalForm);
          }
      }
}
