package org.jetlinks.pro.device.cockpit.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.jetlinks.pro.device.cockpit.dto.DeviceCockpitSummary;
import org.jetlinks.pro.device.cockpit.service.DeviceCockpitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/device/cockpit")
@Tag(name = "设备驾驶舱")
public class DeviceCockpitController {

    private final DeviceCockpitService cockpitService;

    public DeviceCockpitController(DeviceCockpitService cockpitService) {
        this.cockpitService = cockpitService;
    }

    @GetMapping("/summary")
    @Operation(summary = "设备驾驶舱汇总")
    public Mono<DeviceCockpitSummary> summary(@RequestParam(defaultValue = "all") String productType) {
        return cockpitService.summary(productType);
    }
}
