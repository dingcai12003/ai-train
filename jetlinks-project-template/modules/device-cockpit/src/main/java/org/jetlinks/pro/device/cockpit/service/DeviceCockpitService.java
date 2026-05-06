package org.jetlinks.pro.device.cockpit.service;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.core.param.TermType;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.core.command.CommandSupport;
import org.jetlinks.core.metadata.types.GeoPoint;
import org.jetlinks.pro.command.CommandSupportManagerProviders;
import org.jetlinks.pro.command.InternalSdkServices;
import org.jetlinks.pro.command.rule.RuleCommandServices;
import org.jetlinks.pro.device.cockpit.dto.DeviceCockpitSummary;
import org.jetlinks.pro.geo.GeoObject;
import org.jetlinks.sdk.server.SdkServices;
import org.jetlinks.sdk.server.commons.cmd.CountCommand;
import org.jetlinks.sdk.server.commons.cmd.QueryListCommand;
import org.jetlinks.sdk.server.device.DeviceCommandSupportTypes;
import org.jetlinks.sdk.server.device.DeviceInfo;
import org.jetlinks.sdk.server.device.DeviceState;
import org.jetlinks.sdk.server.device.DeviceType;
import org.jetlinks.sdk.server.device.ProductInfo;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeviceCockpitService {

    private static final int DEFAULT_LIST_SIZE = 2000;
    private static final int PRODUCT_OVERVIEW_SIZE = 200;
    private static final int LOCATION_SIZE = 500;
    private static final String PRODUCT_TYPE_ERROR = "error.device_cockpit.unsupported_product_type";

    public Mono<DeviceCockpitSummary> summary(String productType) {
        ProductTypeScope scope = ProductTypeScope.of(productType);

        Mono<Integer> totalDevices = count(deviceSupport(), query(scope.deviceType(), null, null));
        Mono<Integer> onlineDevices = count(deviceSupport(), query(scope.deviceType(), "state", DeviceState.online.getValue()));
        Mono<Integer> offlineDevices = count(deviceSupport(), query(scope.deviceType(), "state", DeviceState.offline.getValue()));
        Mono<Integer> totalProducts = count(productSupport(), query(scope.deviceType(), null, null));
        Mono<Integer> activeAlarms = count(alarmRecordSupport(), query(scope.deviceType(), null, null));
        Mono<List<ProductInfo>> products = productList(scope).cache();
        Mono<List<DeviceCockpitSummary.Location>> locations = locations(scope).collectList();
        Mono<List<DeviceCockpitSummary.AlarmLevel>> alarmLevels = alarmLevels(scope).collectList();
        Mono<List<DeviceCockpitSummary.Category>> categories = products.map(this::categories);
        Mono<List<DeviceCockpitSummary.ProductOverview>> productOverview = products.flatMapMany(Flux::fromIterable)
            .take(PRODUCT_OVERVIEW_SIZE)
            .flatMap(product -> count(deviceSupport(), query(scope.deviceType(), "productId", product.getId()))
                .map(count -> new DeviceCockpitSummary.ProductOverview(
                    product.getId(),
                    product.getName(),
                    product.getDeviceType() == null ? "" : product.getDeviceType().getValue(),
                    count
                )))
            .collectList();

        return Mono.zip(totalDevices, onlineDevices, offlineDevices, totalProducts, activeAlarms, locations)
            .flatMap(tuple -> Mono.zip(
                categories,
                alarmLevels,
                onlineTrend(tuple.getT2(), tuple.getT3()),
                productOverview
            ).map(detail -> new DeviceCockpitSummary(
                new DeviceCockpitSummary.Metrics(
                    tuple.getT1(),
                    tuple.getT2(),
                    tuple.getT3(),
                    tuple.getT4(),
                    tuple.getT5(),
                    tuple.getT6().size()
                ),
                tuple.getT6(),
                detail.getT1(),
                detail.getT2(),
                detail.getT3(),
                detail.getT4()
            )));
    }

    private Mono<CommandSupport> deviceSupport() {
        return commandSupport(SdkServices.deviceService, DeviceCommandSupportTypes.device);
    }

    private Mono<CommandSupport> productSupport() {
        return commandSupport(SdkServices.deviceService, DeviceCommandSupportTypes.product);
    }

    private Mono<CommandSupport> alarmRecordSupport() {
        return commandSupport(
            InternalSdkServices.ruleService,
            RuleCommandServices.alarmRecordService
        );
    }

    private Mono<CommandSupport> geoObjectSupport() {
        return commandSupport(InternalSdkServices.geoObjectService);
    }

    private Mono<CommandSupport> commandSupport(String serviceId, String supportId) {
        return CommandSupportManagerProviders.getCommandSupport(serviceId, supportId)
                                            .switchIfEmpty(Mono.error(() -> new IllegalStateException(
                                                "Command support not available: " + serviceId + "/" + supportId
                                            )));
    }

    private Mono<CommandSupport> commandSupport(String serviceId) {
        return CommandSupportManagerProviders.getCommandSupport(serviceId)
                                            .switchIfEmpty(Mono.error(() -> new IllegalStateException(
                                                "Command support not available: " + serviceId
                                            )));
    }

    private Mono<Integer> count(Mono<CommandSupport> support, QueryParamEntity query) {
        return support.flatMap(commandSupport -> commandSupport.execute(new CountCommand().withQueryParam(query)));
    }

    private <T> Flux<T> queryList(Mono<CommandSupport> support, QueryListCommand<T> command) {
        return support.flatMapMany(commandSupport -> commandSupport.execute(command));
    }

    private Mono<List<ProductInfo>> productList(ProductTypeScope scope) {
        return queryList(
            productSupport(),
            QueryListCommand.of(ProductInfo.class).withQueryParam(query(scope.deviceType(), null, null).doPaging(0, DEFAULT_LIST_SIZE))
        ).collectList();
    }

    private Flux<DeviceCockpitSummary.Location> locations(ProductTypeScope scope) {
        Mono<Set<String>> deviceIds = queryList(
            deviceSupport(),
            QueryListCommand.of(DeviceInfo.class).withQueryParam(query(scope.deviceType(), null, null).doPaging(0, DEFAULT_LIST_SIZE))
        ).map(DeviceInfo::getId).filter(Objects::nonNull).collect(Collectors.toSet());

        return deviceIds.flatMapMany(ids -> queryList(
                geoObjectSupport(),
                QueryListCommand.of(GeoObject.class).withQueryParam(geoQuery(ids).doPaging(0, LOCATION_SIZE))
            ))
            .map(this::toLocation)
            .filter(Objects::nonNull);
    }

    private Flux<DeviceCockpitSummary.AlarmLevel> alarmLevels(ProductTypeScope scope) {
        return queryList(
            alarmRecordSupport(),
            QueryListCommand.of(Map.class).withQueryParam(query(scope.deviceType(), null, null).doPaging(0, DEFAULT_LIST_SIZE))
        )
            .map(this::alarmLevel)
            .filter(Objects::nonNull)
            .collectMultimap(Function.identity())
            .flatMapMany(map -> Flux.fromIterable(map.entrySet()))
            .map(entry -> new DeviceCockpitSummary.AlarmLevel(entry.getKey(), entry.getValue().size()))
            .sort(Comparator.comparing(DeviceCockpitSummary.AlarmLevel::level));
    }

    private Mono<List<DeviceCockpitSummary.TrendPoint>> onlineTrend(long online, long offline) {
        return Mono.just(List.of(new DeviceCockpitSummary.TrendPoint(LocalDate.now().toString(), online, offline)));
    }

    private List<DeviceCockpitSummary.Category> categories(List<ProductInfo> products) {
        Map<String, Long> counts = products.stream()
            .collect(Collectors.groupingBy(
                product -> firstNotBlank(product.getClassifiedName(), product.getClassifiedId(), "uncategorized"),
                LinkedHashMap::new,
                Collectors.counting()
            ));

        List<DeviceCockpitSummary.Category> categories = new ArrayList<>();
        counts.forEach((name, count) -> categories.add(new DeviceCockpitSummary.Category(name, name, count)));
        return categories;
    }

    private QueryParamEntity query(DeviceType deviceType, String column, Object value) {
        QueryParamEntity query = QueryParamEntity.of();
        if (deviceType != null) {
            query.getTerms().add(Term.of("deviceType", TermType.eq, deviceType.getValue()));
        }
        if (column != null && value != null) {
            query.getTerms().add(Term.of(column, TermType.eq, value));
        }
        return query;
    }

    private QueryParamEntity geoQuery(Set<String> deviceIds) {
        QueryParamEntity query = QueryParamEntity.of();
        query.getTerms().add(Term.of("objectType", TermType.eq, "device"));
        if (!deviceIds.isEmpty()) {
            query.getTerms().add(Term.of("objectId", TermType.in, deviceIds));
        }
        return query;
    }

    private DeviceCockpitSummary.Location toLocation(GeoObject object) {
        GeoPoint point = object.getPoint();
        if (point == null) {
            return null;
        }
        return new DeviceCockpitSummary.Location(
            object.getId(),
            object.getObjectId(),
            locationName(object),
            point.getLon(),
            point.getLat()
        );
    }

    private String locationName(GeoObject object) {
        Map<String, Object> tags = object.getTags();
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        Object name = firstValue(tags, "name", "deviceName", "productName");
        return name == null ? "" : String.valueOf(name);
    }

    private String alarmLevel(Map<?, ?> record) {
        Object level = firstValue(record, "level", "alarmLevel", "alarmLevelText", "levelName");
        return level == null ? "unknown" : String.valueOf(level);
    }

    private Object firstValue(Map<?, ?> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private record ProductTypeScope(DeviceType deviceType) {

        private static ProductTypeScope of(String productType) {
            String normalized = productType == null ? "all" : productType.trim();
            if (normalized.isEmpty() || "all".equalsIgnoreCase(normalized)) {
                return new ProductTypeScope(null);
            }
            return switch (normalized.toLowerCase(Locale.ROOT)) {
                case "device" -> new ProductTypeScope(DeviceType.device);
                case "childrendevice" -> new ProductTypeScope(DeviceType.childrenDevice);
                case "gateway" -> new ProductTypeScope(DeviceType.gateway);
                default -> throw new ValidationException(
                    "Unsupported product type: " + productType,
                    PRODUCT_TYPE_ERROR,
                    productType
                );
            };
        }
    }
}
