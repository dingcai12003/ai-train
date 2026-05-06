package org.jetlinks.pro.device.cockpit.dto;

import java.util.List;

public record DeviceCockpitSummary(
    Metrics metrics,
    List<Location> locations,
    List<Category> categories,
    List<AlarmLevel> alarmLevels,
    List<TrendPoint> onlineTrend,
    List<ProductOverview> productOverview
) {

    public record Metrics(
        long totalDevices,
        long onlineDevices,
        long offlineDevices,
        long totalProducts,
        long activeAlarms,
        long locations
    ) {
    }

    public record Location(
        String id,
        String deviceId,
        String name,
        double longitude,
        double latitude
    ) {
    }

    public record Category(
        String id,
        String name,
        long count
    ) {
    }

    public record AlarmLevel(
        String level,
        long count
    ) {
    }

    public record TrendPoint(
        String time,
        long online,
        long offline
    ) {
    }

    public record ProductOverview(
        String productId,
        String productName,
        String productType,
        long deviceCount
    ) {
    }
}
