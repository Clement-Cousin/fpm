package com.mappy.fpm.batches.tomtom.helpers;

import com.mappy.fpm.batches.tomtom.TomtomShapefile;
import com.mappy.fpm.batches.tomtom.dbf.names.NameProvider;
import com.mappy.fpm.batches.utils.Feature;
import com.mappy.fpm.batches.utils.GeometrySerializer;
import com.mappy.fpm.batches.utils.LongLineSplitter;
import com.neovisionaries.i18n.CountryCode;
import com.vividsolutions.jts.algorithm.Centroid;
import com.vividsolutions.jts.geom.*;
import org.jetbrains.annotations.NotNull;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.valueOf;
import static java.util.Optional.ofNullable;
import static org.openstreetmap.osmosis.core.domain.v0_6.EntityType.Node;
import static org.openstreetmap.osmosis.core.domain.v0_6.EntityType.Way;

public class BoundariesShapefile extends TomtomShapefile {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
    private final String adminLevel;
    private final String tomtomLevel;
    private final NameProvider nameProvider;

    protected BoundariesShapefile(String filename, int adminLevel, int tomtomLevel, NameProvider nameProvider) {
        super(filename);
        this.adminLevel = String.valueOf(adminLevel);
        this.tomtomLevel = String.valueOf(tomtomLevel);
        this.nameProvider = nameProvider;
        this.nameProvider.loadFromFile("___an.dbf", "NAME", false);
    }

    @Override
    public void serialize(GeometrySerializer serializer, Feature feature) {
        String name = feature.getString("NAME");
        Long extId = feature.getLong("ID");
        String order = feature.getString("ORDER0" + tomtomLevel);
        Optional<Long> population = ofNullable(feature.getLong("POP"));

        Map<String, String> tags = nameProvider.getAlternateNames(extId);
        tags.putAll(of(
                "ref:tomtom", String.valueOf(extId),
                "ref:INSEE", getInseeWithAlpha3(order)
        ));
        population.ifPresent(pop -> tags.put("population", valueOf(pop)));
        addRelations(serializer, feature, newArrayList(), name, tags);
    }

    @NotNull
    private String getInseeWithAlpha3(String alpha3) {
        String alpha32 = alpha3;
        if (CountryCode.getByCode(alpha3) == null) {
            alpha32 = alpha3.substring(0, alpha3.length() - 1);
        }
        return CountryCode.getByCode(alpha32) == null ? alpha3 : valueOf(CountryCode.getByCode(alpha32).getNumeric());
    }

    public void writeRelations(GeometrySerializer serializer, List<RelationMember> members, Map<String, String> tags) {
        tags.put("type", "boundary");
        serializer.writeRelation(members, tags);
    }

    public void addRelations(GeometrySerializer serializer, Feature feature, List<RelationMember> members, String name, Map<String, String> tags) {
        if (name != null) {
            Map<String, String> wayTags = newHashMap(of(
                    "name", name,
                    "boundary", "administrative",
                    "admin_level", adminLevel));
            Map<String, String> pointTags = newHashMap(tags);
            pointTags.put("name", name);
            MultiPolygon multiPolygon = feature.getMultiPolygon();
            addPointWithRoleLabel(serializer, members, pointTags, multiPolygon);
            for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
                Polygon polygon = (Polygon) multiPolygon.getGeometryN(i);
                for (int j=0; j < polygon.getNumInteriorRing(); j++){
                    for (Geometry geom : LongLineSplitter.split(polygon.getInteriorRingN(j), 100)) {
                        addRelationMember(serializer, members, wayTags, (LineString) geom, "inner");
                    }
                }
                for (Geometry geom : LongLineSplitter.split(polygon.getExteriorRing(), 100)) {
                    addRelationMember(serializer, members, wayTags, (LineString) geom, "outer");
                }
            }
            tags.putAll(wayTags);
            writeRelations(serializer, members, tags);
        }
    }

    private void addRelationMember(GeometrySerializer serializer, List<RelationMember> members, Map<String, String> wayTags, LineString geom, String memberRole) {
        Way way = serializer.write(geom, wayTags);
        members.add(new RelationMember(way.getId(), Way, memberRole));
    }

    private void addPointWithRoleLabel(GeometrySerializer serializer, List<RelationMember> members, Map<String, String> tags, MultiPolygon multiPolygon) {
        Coordinate centPt = Centroid.getCentroid(multiPolygon);
        Optional<Node> node = serializer.writePoint(GEOMETRY_FACTORY.createPoint(centPt), tags);
        node.ifPresent(nodeLabel -> members.add(new RelationMember(nodeLabel.getId(), Node, "label")));
    }
}
