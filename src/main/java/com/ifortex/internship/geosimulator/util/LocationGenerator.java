package com.ifortex.internship.geosimulator.util;

import com.ifortex.internship.geosimulator.model.GeoLocationDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocationGenerator {

    @Value("${app.geo.simulation.emit-interval}")
    private Duration emitInterval;

    public Flux<GeoLocationDto> generateLocationStream(
        UUID emergencyId,
        UUID paramedicId,
        BigDecimal startLat, BigDecimal startLng,
        BigDecimal endLat, BigDecimal endLng,
        int durationInSeconds
    ) {
        if (durationInSeconds <= 0) {
            throw new IllegalArgumentException("Duration must be greater than zero");
        }

        BigDecimal steps = BigDecimal.valueOf(durationInSeconds);
        BigDecimal latStep = endLat.subtract(startLat).divide(steps, 10, RoundingMode.HALF_UP);
        BigDecimal lngStep = endLng.subtract(startLng).divide(steps, 10, RoundingMode.HALF_UP);

        return Flux
            .interval(emitInterval)
            .take(durationInSeconds + 1L)
            .map(second -> {
                BigDecimal stepIndex = BigDecimal.valueOf(second);
                BigDecimal currentLat = startLat.add(latStep.multiply(stepIndex));
                BigDecimal currentLng = startLng.add(lngStep.multiply(stepIndex));

                return new GeoLocationDto(
                    emergencyId,
                    paramedicId,
                    currentLat,
                    currentLng,
                    Instant.now()
                );
            });
    }
}
