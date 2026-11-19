package com.farmlink.delivery;

import com.farmlink.common.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
@Tag(name = "Delivery")
@SecurityRequirement(name = "bearerAuth")
public class DeliveryController {

    private final DeliveryService service;

    @GetMapping("/check")
    public ApiResponse<DeliveryService.DeliveryCheck> check(@RequestParam UUID productId, @RequestParam double lat,
                                                            @RequestParam double lng) {
        return ApiResponse.ok(service.check(productId, lat, lng));
    }
}
