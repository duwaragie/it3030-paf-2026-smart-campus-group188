package com.smartcampus.api.config;

import com.smartcampus.api.model.ShuttleRoute;
import com.smartcampus.api.repository.ShuttleRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ShuttleRouteDataInitializer implements CommandLineRunner {

    private final ShuttleRouteRepository shuttleRouteRepository;

    @Override
    public void run(String... args) throws Exception {
        if (shuttleRouteRepository.count() == 0) {
            // Leave polyline empty so the client renders a straight line between origin and destination.
            // Populate these with overview_polyline.points from Google Directions API for road-following curves.
            String polyKollupitiya = "";
            String polyPanadura = "";
            String polyNegombo = "";

            List<ShuttleRoute> routes = Arrays.asList(
                ShuttleRoute.builder()
                    .name("Kollupitiya - Malabe Express")
                    .originName("Kollupitiya")
                    .destinationName("Malabe Campus")
                    .originLat(6.9100)
                    .originLng(79.8500)
                    .destLat(6.9147)
                    .destLng(79.9723)
                    .polyline(polyKollupitiya)
                    .color("#3b82f6") // Blue
                    .active(true)
                    .build(),
                    
                ShuttleRoute.builder()
                    .name("Panadura - Malabe Direct")
                    .originName("Panadura")
                    .destinationName("Malabe Campus")
                    .originLat(6.7132)
                    .originLng(79.9026)
                    .destLat(6.9147)
                    .destLng(79.9723)
                    .polyline(polyPanadura)
                    .color("#ef4444") // Red
                    .active(true)
                    .build(),
                    
                ShuttleRoute.builder()
                    .name("Negombo - Malabe Route")
                    .originName("Negombo")
                    .destinationName("Malabe Campus")
                    .originLat(7.2084)
                    .originLng(79.8380)
                    .destLat(6.9147)
                    .destLng(79.9723)
                    .polyline(polyNegombo)
                    .color("#10b981") // Green
                    .active(true)
                    .build()
            );

            shuttleRouteRepository.saveAll(routes);
            System.out.println("Seeded 3 shuttle routes successfully.");
        }
    }
}
