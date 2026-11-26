package io.github.duduri97.spatial.gis;

import io.github.duduri97.spatial.apikey.ApiKey;
import io.github.duduri97.spatial.apikey.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds public-coordinate synthetic spatial features and a demo API key on startup.
 */
@Component
public class SpatialDataInitializer implements CommandLineRunner {

    private final SpatialFeatureRepository featureRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final String demoApiKey;

    public SpatialDataInitializer(
            SpatialFeatureRepository featureRepository,
            ApiKeyRepository apiKeyRepository,
            @Value("${spatial.demo.api-key:demo-spatial-key-2026}") String demoApiKey) {
        this.featureRepository = featureRepository;
        this.apiKeyRepository = apiKeyRepository;
        this.demoApiKey = demoApiKey;
    }

    @Override
    public void run(String... args) {
        if (apiKeyRepository.findByKeyValue(demoApiKey).isEmpty()) {
            apiKeyRepository.save(new ApiKey(
                    demoApiKey, "SDK Demo Playground", "duduri97", ApiKey.Status.APPROVED, 10_000
            ));
        }

        if (featureRepository.count() == 0) {
            featureRepository.saveAll(List.of(
                    new SpatialFeature("LANDMARK", "Seoul City Hall", "CIVIC", 126.9780, 37.5665, 32.0, "Civic plaza and public transit hub"),
                    new SpatialFeature("LANDMARK", "Gwanghwamun Gate", "HERITAGE", 126.9769, 37.5759, 38.5, "Historic main gate of Gyeongbokgung"),
                    new SpatialFeature("LANDMARK", "N Seoul Tower", "OBSERVATORY", 126.9882, 37.5512, 479.7, "Namsan peak observatory landmark"),
                    new SpatialFeature("LANDMARK", "Dongdaemun Design Plaza", "CULTURE", 127.0095, 37.5663, 28.0, "Urban design and exhibition complex"),
                    new SpatialFeature("LANDMARK", "Lotte World Tower", "SKYSCRAPER", 127.1025, 37.5125, 555.0, "High-rise observatory in Jamsil"),
                    new SpatialFeature("TRANSIT", "Seoul Station", "RAIL", 126.9707, 37.5547, 25.0, "Major KTX and metro interchange"),
                    new SpatialFeature("TRANSIT", "Gangnam Station", "METRO", 127.0276, 37.4979, 22.0, "High-volume metro transfer hub"),
                    new SpatialFeature("TRANSIT", "Yeouido Station", "METRO", 126.9242, 37.5216, 15.0, "Financial district metro hub"),
                    new SpatialFeature("TRANSIT", "Hongik Univ. Station", "METRO", 126.9245, 37.5572, 20.0, "Airport Railroad and Line 2 hub"),
                    new SpatialFeature("PARK", "Yeouido Hangang Park", "WATERFRONT", 126.9341, 37.5284, 11.0, "Han River riverside public park"),
                    new SpatialFeature("PARK", "Seoul Forest", "FOREST", 127.0375, 37.5444, 18.0, "Urban ecological forest park"),
                    new SpatialFeature("PARK", "Olympic Park", "SPORTS", 127.1215, 37.5207, 24.0, "Large urban green space and arena"),
                    new SpatialFeature("SENSOR", "AQ-Station-Jongno", "AIR_QUALITY", 126.9830, 37.5720, 40.0, "Synthetic urban air quality node"),
                    new SpatialFeature("SENSOR", "AQ-Station-Gangnam", "AIR_QUALITY", 127.0470, 37.5172, 35.0, "Synthetic urban air quality node"),
                    new SpatialFeature("SENSOR", "AQ-Station-Mapo", "AIR_QUALITY", 126.9056, 37.5562, 29.0, "Synthetic urban air quality node")
            ));
        }
    }
}
