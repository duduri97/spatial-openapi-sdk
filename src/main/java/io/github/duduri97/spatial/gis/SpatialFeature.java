package io.github.duduri97.spatial.gis;

import jakarta.persistence.*;

@Entity
@Table(name = "spatial_features", indexes = {
        @Index(name = "idx_spatial_layer", columnList = "layerCode"),
        @Index(name = "idx_spatial_bbox", columnList = "longitude, latitude")
})
public class SpatialFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String layerCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false)
    private double longitude;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double elevation;

    @Column(length = 500)
    private String description;

    protected SpatialFeature() {
    }

    public SpatialFeature(String layerCode, String name, String category,
                          double longitude, double latitude, double elevation, String description) {
        this.layerCode = layerCode;
        this.name = name;
        this.category = category;
        this.longitude = longitude;
        this.latitude = latitude;
        this.elevation = elevation;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getLayerCode() { return layerCode; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getLongitude() { return longitude; }
    public double getLatitude() { return latitude; }
    public double getElevation() { return elevation; }
    public String getDescription() { return description; }
}
