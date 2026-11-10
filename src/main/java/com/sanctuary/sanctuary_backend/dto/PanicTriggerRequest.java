package com.sanctuary.sanctuary_backend.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Body for POST /api/panic/trigger. Deliberately has no rejecting validation: a panic must
 * never fail because of its location. lat/lng are read as raw JSON values so that even
 * "abc" or NaN can't make the body unreadable; anything that isn't a real, in-range
 * coordinate pair means "no location", and the alert still goes out.
 */
public record PanicTriggerRequest(JsonNode lat, JsonNode lng) {

    public static final PanicTriggerRequest NO_LOCATION = new PanicTriggerRequest(null, null);

    public boolean hasValidLocation() {
        Double la = number(lat);
        Double ln = number(lng);
        return la != null && ln != null
            && la >= -90 && la <= 90
            && ln >= -180 && ln <= 180;
    }

    /** Latitude if the location is valid, otherwise null. */
    public Double validLat() {
        return hasValidLocation() ? lat.doubleValue() : null;
    }

    /** Longitude if the location is valid, otherwise null. */
    public Double validLng() {
        return hasValidLocation() ? lng.doubleValue() : null;
    }

    private static Double number(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return null;
        }
        double value = node.doubleValue();
        return Double.isFinite(value) ? value : null;
    }

    // Never print coordinates if this is logged
    @Override
    public String toString() {
        return "PanicTriggerRequest[location=" + (hasValidLocation() ? "<hidden>" : "none") + "]";
    }
}
