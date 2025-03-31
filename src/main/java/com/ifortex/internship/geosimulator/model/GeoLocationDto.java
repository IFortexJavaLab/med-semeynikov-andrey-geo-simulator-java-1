package com.ifortex.internship.geosimulator.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GeoLocationDto(
    UUID paramedicId,
    BigDecimal latitude,
    BigDecimal longitude,
    Instant timestamp
) {
}
