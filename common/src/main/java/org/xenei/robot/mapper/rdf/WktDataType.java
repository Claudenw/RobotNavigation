package org.xenei.robot.mapper.rdf;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.DatatypeFormatException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.geosparql.implementation.vocabulary.Geo;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WktDataType extends BaseDatatype {

    private final Map<String, Geometry> cache;

    private enum SupportedTypes {
        point(Point.class), linestring(LineString.class), linearring(LinearRing.class), polygon(
                Polygon.class), multipoint(MultiPoint.class), multilinestring(MultiLineString.class), multipolygon(
                        MultiPolygon.class), geometrycollection(GeometryCollection.class);

        final Class<?> supporting;

        SupportedTypes(Class<?> supporting) {
            this.supporting = supporting;
        }
    }

    public WktDataType() {
        super(URI);
        this.cache = Collections.synchronizedMap(new LRUMap<>(500));
        TypeMapper typeMapper = TypeMapper.getInstance();
        typeMapper.registerDatatype(this);
        for (SupportedTypes type : SupportedTypes.values()) {
            typeMapper.registerDatatype(new Wrapped(type));
        }
    }

    public void unregister() {
        TypeMapper typeMapper = TypeMapper.getInstance();
        typeMapper.unregisterDatatype(this);
        for (SupportedTypes type : SupportedTypes.values()) {
            typeMapper.unregisterDatatype(new Wrapped(type));
        }
    }

    static final Logger LOGGER = LoggerFactory.getLogger(WktDataType.class);

    /**
     * The default WKT type URI.
     */
    public static final String URI = Geo.WKT;// Namespace.URI + ":datatype:wktLiteral";

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
        if (geometry instanceof Geometry geom) {
            String result = geom.toText();
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
        if (wktTextSRS.srsURI != null) {
            LOGGER.warn("SRS specified in {} is ignored: {}", WktDataType.class.getSimpleName(), literalForm);
        }
        try {
            Geometry geometry = new WKTReader().read(wktTextSRS.wktText);
            cache.put(literalForm, geometry);
            return geometry;
        } catch (ParseException e) {
            throw new DatatypeFormatException(e.getMessage(), e);
        }
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

        @SuppressWarnings("unused")
        public String getWktText() {
            return wktText;
        }

        @SuppressWarnings("unused")
        public String getSrsURI() {
            return srsURI;
        }

    }

    private class Wrapped extends BaseDatatype {
        SupportedTypes type;

        Wrapped(SupportedTypes type) {
            super(Namespace.URI + ":SupportedTypes:" + type);
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
