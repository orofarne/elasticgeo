/**
 * This file is hereby placed into the Public Domain. This means anyone is
 * free to do whatever they wish with this file.
 */
package mil.nga.giat.data.elasticsearch;

import java.util.logging.Level;
import java.util.logging.Logger;

import static mil.nga.giat.data.elasticsearch.ElasticLayerConfiguration.ANALYZED;
import static mil.nga.giat.data.elasticsearch.ElasticLayerConfiguration.DATE_FORMAT;
import static mil.nga.giat.data.elasticsearch.ElasticLayerConfiguration.FULL_NAME;
import static mil.nga.giat.data.elasticsearch.ElasticLayerConfiguration.GEOMETRY_TYPE;
import static mil.nga.giat.data.elasticsearch.ElasticLayerConfiguration.NESTED;
import mil.nga.giat.data.elasticsearch.ElasticAttribute.ElasticGeometryType;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Builds a feature type based on the attributes defined in the 
 * {@link ElasticLayerConfiguration}.
 *
 */
public class ElasticFeatureTypeBuilder extends SimpleFeatureTypeBuilder {

    private final static Logger LOGGER = Logger.getLogger(ElasticFeatureTypeBuilder.class.getName());

    private ElasticLayerConfiguration layerConfiguration;

    public ElasticFeatureTypeBuilder(ElasticLayerConfiguration layerConfiguration, Name name) {
        setName(name);
        this.layerConfiguration = layerConfiguration;
    }

    @Override
    public SimpleFeatureType buildFeatureType() {
        if (layerConfiguration != null) {
            String defaultGeometryName = null;
            for (ElasticAttribute attribute : layerConfiguration.getAttributes()) {
                if (attribute.isUse()) {
                    final String attributeName;
                    if (attribute.getUseShortName()) {
                        attributeName = attribute.getShortName();
                    } else {
                        attributeName = attribute.getName();
                    }

                    AttributeDescriptor att = null;
                    if (Geometry.class.isAssignableFrom(attribute.getType())) {
                        Integer srid = attribute.getSrid();
                        try {
                            if (srid != null) {
                                attributeBuilder.setCRS(CRS.decode("EPSG:" + srid));
                                attributeBuilder.setName(attributeName);
                                attributeBuilder.setBinding(attribute.getType());
                                att = attributeBuilder.buildDescriptor(attributeName,
                                        attributeBuilder.buildGeometryType());
                                
                                final ElasticGeometryType geometryType = attribute.getGeometryType();
                                att.getUserData().put(GEOMETRY_TYPE, geometryType);
                                if (attribute.isDefaultGeometry() != null
                                        && attribute.isDefaultGeometry()) {
                                    defaultGeometryName = attributeName;
                                }
                            }
                        } catch (Exception e) {
                            String msg = "Error occured determing srid for " + attribute.getName();
                            LOGGER.log(Level.WARNING, msg, e);
                        }
                    } else {
                        attributeBuilder.setName(attributeName);
                        attributeBuilder.setBinding(attribute.getType());
                        att = attributeBuilder.buildDescriptor(attributeName, 
                                attributeBuilder.buildType());
                    }
                    if (att != null && attribute.getDateFormat() != null) {
                        att.getUserData().put(DATE_FORMAT, attribute.getDateFormat());
                    }
                    if (att != null) {
                        att.getUserData().put(FULL_NAME, attribute.getName());
                        att.getUserData().put(ANALYZED, attribute.getAnalyzed());
                        att.getUserData().put(NESTED, attribute.isNested());
                        add(att);
                    }
                }
            }
            if (defaultGeometryName != null) {
                setDefaultGeometry(defaultGeometryName);
            }
        }
        return super.buildFeatureType();
    }

}
