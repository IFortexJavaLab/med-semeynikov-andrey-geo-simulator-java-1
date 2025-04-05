package com.ifortex.internship.geosimulator.service;

import com.ifortex.internship.geosimulator.model.GeoLocationDto;
import com.ifortex.internship.geosimulator.util.LocationGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GeoLocationSender {

    LocationGenerator locationGenerator;
    WebSocketClientService webSocketClientService;

    @Value("${app.websocket.destination}")
    String destination;

    private final Map<UUID, Disposable> activeSimulations = new ConcurrentHashMap<>();

    public void startSimulation(UUID emergencyId,
                                UUID paramedicId,
                                BigDecimal startLat, BigDecimal startLng,
                                BigDecimal endLat, BigDecimal endLng,
                                int durationInSeconds) {

        if (activeSimulations.containsKey(paramedicId)) {
            log.error("Simulation already running for paramedic {}", paramedicId);
            return;
        }

        StompSession session = webSocketClientService.getSession();
        Flux<GeoLocationDto> locationStream = locationGenerator.generateLocationStream(
            emergencyId, paramedicId, startLat, startLng, endLat, endLng, durationInSeconds
        );

        Disposable disposable = locationStream.subscribe(location -> {
            try {
                session.send(destination, new GeoLocationDto(
                    emergencyId,
                    location.paramedicId(),
                    location.latitude(),
                    location.longitude(),
                    location.timestamp()
                ));
            } catch (Exception e) {
                log.error("Failed to send location for paramedic {}: {}", paramedicId, e.getMessage());
            }
        }, error -> log.error("Simulation error for paramedic {}: {}", paramedicId, error.getMessage()), () -> {
            log.info("Simulation completed for paramedic {}", paramedicId);
            activeSimulations.remove(paramedicId);
        });

        activeSimulations.put(paramedicId, disposable);
    }

    public boolean isSimulationRunning(UUID paramedicId) {
        return activeSimulations.containsKey(paramedicId);
    }

    public void stopSimulation(UUID paramedicId) {
        Disposable disposable = activeSimulations.remove(paramedicId);
        if (disposable != null) {
            disposable.dispose();
            log.info("Simulation stopped for paramedic {}", paramedicId);
        }
    }

    public void stopAll() {
        activeSimulations.values().forEach(Disposable::dispose);
        activeSimulations.clear();
        log.info("All simulations stopped");
    }

    public Set<UUID> getActiveParamedicIds() {
        return Set.copyOf(activeSimulations.keySet());
    }

    private Consumer<GeoLocationDto> createLocationSender(StompSession session, UUID paramedicId) {
        return location -> {
            try {
                session.send(destination, location);
                log.trace("Sent location for {}: {}", paramedicId, location);
            } catch (Exception e) {
                log.error("Failed to send location for {}: {}", paramedicId, e.getMessage());
            }
        };
    }
}