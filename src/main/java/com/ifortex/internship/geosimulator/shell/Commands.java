package com.ifortex.internship.geosimulator.shell;

import com.ifortex.internship.geosimulator.service.GeoLocationSender;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

@ShellComponent
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Commands {

    GeoLocationSender geoLocationSender;

    @ShellMethod(key = "simulate-route", value = "Start paramedic geolocation simulation")
    public String simulateRoute(
        @ShellOption(value = {"-e", "--emergency-id"}, help = "Emergency ID") UUID emergencyId,
        @ShellOption(value = {"-p", "--paramedic"}, help = "Paramedic ID") UUID paramedicId,
        @ShellOption(value = {"--from-lat"}, help = "Start latitude") BigDecimal startLat,
        @ShellOption(value = {"--from-lng"}, help = "Start longitude") BigDecimal startLng,
        @ShellOption(value = {"--to-lat"}, help = "End latitude") BigDecimal endLat,
        @ShellOption(value = {"--to-lng"}, help = "End longitude") BigDecimal endLng,
        @ShellOption(value = {"-d", "--duration"}, help = "Duration in seconds") int durationInSeconds
    ) {
        if (durationInSeconds <= 0) return "Duration must be > 0 seconds";
        if (startLat.compareTo(BigDecimal.ZERO) == 0 || startLng.compareTo(BigDecimal.ZERO) == 0)
            return "Start coordinates must not be zero";
        if (endLat.compareTo(BigDecimal.ZERO) == 0 || endLng.compareTo(BigDecimal.ZERO) == 0)
            return "End coordinates must not be zero";
        if (startLat.equals(endLat) && startLng.equals(endLng))
            return "Start and end points must be different";

        geoLocationSender.startSimulation(emergencyId, paramedicId, startLat, startLng, endLat, endLng, durationInSeconds);
        return "Simulation started for paramedic: " + paramedicId + " (Emergency: " + emergencyId + ")";
    }


    @ShellMethod(value = "Stop paramedic simulation", key = "stop-route")
    public String stopSimulation(@ShellOption(value = {"-i", "--id"}) UUID paramedicId) {
        geoLocationSender.stopSimulation(paramedicId);
        return "Simulation stopped for paramedic: " + paramedicId;
    }

    @ShellMethod(value = "Stop all simulations", key = "stop-all")
    public String stopAllSimulations() {
        geoLocationSender.stopAll();
        return "All simulations stopped";
    }

    @ShellMethod(value = "List all active simulations", key = "list-routes")
    public String listActiveSimulations() {
        Set<UUID> ids = geoLocationSender.getActiveParamedicIds();
        return ids.isEmpty()
            ? "No active simulations"
            : "Active paramedics: " + ids;
    }

    @ShellMethod(value = "Check if simulation is running for paramedic", key = "status-route")
    public String checkSimulationStatus(@ShellOption(value = {"-i", "--id"}) UUID paramedicId) {
        boolean running = geoLocationSender.isSimulationRunning(paramedicId);
        return running
            ? "Simulation is active for paramedic: " + paramedicId
            : "No active simulation for paramedic: " + paramedicId;
    }

}
