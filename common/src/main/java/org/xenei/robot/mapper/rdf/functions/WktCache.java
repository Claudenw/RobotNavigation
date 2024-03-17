package org.xenei.robot.mapper.rdf.functions;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.jena.datatypes.TypeMapper;
import org.locationtech.jts.geom.Geometry;
import org.xenei.robot.mapper.rdf.WktDataType;

public class WktCache extends LinkedHashMap<String, Geometry> {

    static final LinkedHashMap<String, Geometry> INSTANCE = new WktCache(250);
    private int maxSize;
    private WktDataType dataType = (WktDataType) TypeMapper.getInstance().getTypeByClass(Geometry.class);

    public WktCache(int maxSize) {
        super(maxSize, 0.75f, true); // access order deletion;
        this.maxSize = maxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > maxSize;
    }

    @Override
    public Geometry get(Object key) {
        Geometry result = super.get(key);
        if (result == null) {
            String keyS = key.toString();
            result = dataType.parse(keyS);
            super.put(keyS, result);
        }
        return result;
    }
}
