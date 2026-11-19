package com.farmlink.product;

import com.farmlink.common.ApiResponse;
import com.farmlink.common.PageResponse;
import com.farmlink.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products")
public class ProductController {

    private final ProductService service;

    @GetMapping
    @Operation(summary = "Browse the marketplace (public). Filters: q, category, farmerId. Sort: ?sort=pricePerUnit,asc")
    public ApiResponse<PageResponse<ProductDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) ProductCategory category,
            @RequestParam(required = false) UUID farmerId,
            @RequestParam(defaultValue = "true") boolean availableOnly,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(service.search(q, category, farmerId, availableOnly ? true : null, pageable));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(service.get(id));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<List<ProductDto>> mine(@AuthenticationPrincipal AuthUser me) {
        return ApiResponse.ok(service.mine(me.id()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FARMER')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ProductDto> create(@AuthenticationPrincipal AuthUser me,
                                          @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(service.create(me.id(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ProductDto> update(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                          @Valid @RequestBody ProductRequest request) {
        return ApiResponse.ok(service.update(me.id(), id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('FARMER')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id) {
        boolean deleted = service.delete(me.id(), id);
        return ApiResponse.ok(null, deleted ? "Product deleted"
                : "Product has existing orders, so it was hidden from the marketplace instead of deleted");
    }

    @PostMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('FARMER')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ProductDto> uploadImage(@AuthenticationPrincipal AuthUser me, @PathVariable UUID id,
                                               @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(service.uploadImage(me.id(), id, file));
    }
}
