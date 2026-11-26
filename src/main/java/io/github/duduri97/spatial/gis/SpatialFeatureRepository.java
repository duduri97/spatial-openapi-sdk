package io.github.duduri97.spatial.gis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpatialFeatureRepository extends JpaRepository<SpatialFeature, Long> {

    @Query("""
            SELECT f FROM SpatialFeature f
            WHERE (:layerCode IS NULL OR f.layerCode = :layerCode)
              AND f.longitude BETWEEN :minLon AND :maxLon
              AND f.latitude BETWEEN :minLat AND :maxLat
            """)
    List<SpatialFeature> findInBoundingBox(
            @Param("layerCode") String layerCode,
            @Param("minLon") double minLon,
            @Param("minLat") double minLat,
            @Param("maxLon") double maxLon,
            @Param("maxLat") double maxLat
    );

    List<SpatialFeature> findByLayerCode(String layerCode);

    @Query("SELECT f.layerCode, COUNT(f) FROM SpatialFeature f GROUP BY f.layerCode ORDER BY f.layerCode")
    List<Object[]> countByLayer();
}
