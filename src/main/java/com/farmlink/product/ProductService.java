package com.farmlink.product;

import com.farmlink.common.ApiException;
import com.farmlink.common.PageResponse;
import com.farmlink.order.OrderItemRepository;
import com.farmlink.storage.FileStorageService;
import com.farmlink.user.User;
import com.farmlink.user.UserService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository products;
    private final OrderItemRepository orderItems;
    private final UserService userService;
    private final FileStorageService storage;

    @Transactional(readOnly = true)
    public PageResponse<ProductDto> search(String q, ProductCategory category, UUID farmerId, Boolean available,
                                           Pageable pageable) {
        return PageResponse.of(products.findAll(ProductSpecs.filter(q, category, farmerId, available), pageable)
                .map(ProductDto::from));
    }

    @Transactional(readOnly = true)
    public ProductDto get(UUID id) {
        return ProductDto.from(find(id));
    }

    @Transactional(readOnly = true)
    public List<ProductDto> mine(UUID farmerId) {
        return products.findByFarmerIdOrderByCreatedAtDesc(farmerId).stream().map(ProductDto::from).toList();
    }

    @Transactional
    public ProductDto create(UUID farmerId, ProductRequest r) {
        User farmer = userService.getEntity(farmerId);
        // Only farmers approved by an admin may list produce.
        if (!farmer.isVerified()) {
            throw ApiException.forbidden("Your account is awaiting admin verification. You can add products once it is verified.");
        }
        Product p = new Product();
        p.setFarmer(farmer);
        apply(p, r);
        // fall back to the farmer's profile location when the product has none
        if (p.getLocation() == null) p.setLocation(farmer.getLocation());
        if (p.getLatitude() == null) p.setLatitude(farmer.getLatitude());
        if (p.getLongitude() == null) p.setLongitude(farmer.getLongitude());
        return ProductDto.from(products.save(p));
    }

    @Transactional
    public ProductDto update(UUID farmerId, UUID id, ProductRequest r) {
        Product p = findOwned(farmerId, id);
        apply(p, r);
        return ProductDto.from(p);
    }

    /** Returns true when deleted, false when the product has orders and was only hidden. */
    @Transactional
    public boolean delete(UUID farmerId, UUID id) {
        Product p = findOwned(farmerId, id);
        if (orderItems.existsByProductId(id)) {
            p.setAvailable(false); // order history must keep pointing at it
            return false;
        }
        products.delete(p);
        return true;
    }

    @Transactional
    public ProductDto uploadImage(UUID farmerId, UUID id, MultipartFile file) {
        Product p = findOwned(farmerId, id);
        p.setImageUrl(storage.storeImage(file));
        return ProductDto.from(p);
    }

    @Transactional
    public ProductDto setAvailability(UUID id, boolean available) { // used by admin moderation
        Product p = find(id);
        p.setAvailable(available);
        return ProductDto.from(p);
    }

    private Product find(UUID id) {
        return products.findById(id).orElseThrow(() -> ApiException.notFound("Product not found"));
    }

    private Product findOwned(UUID farmerId, UUID id) {
        Product p = find(id);
        if (!p.getFarmer().getId().equals(farmerId)) {
            throw ApiException.forbidden("This product belongs to another farmer");
        }
        return p;
    }

    private void apply(Product p, ProductRequest r) {
        p.setName(r.name().trim());
        p.setDescription(r.description());
        p.setCategory(r.category() != null ? r.category() : ProductCategory.OTHER);
        p.setPricePerUnit(r.pricePerUnit());
        p.setUnit(r.unit() != null && !r.unit().isBlank() ? r.unit().trim() : "kg");
        p.setQuantityAvailable(r.quantityAvailable());
        if (r.imageUrl() != null) p.setImageUrl(r.imageUrl());
        p.setAvailable(r.available() == null || r.available());
        p.setLocation(r.location());
        p.setShelfLifeHours(r.shelfLifeHours() != null ? r.shelfLifeHours() : 168);
        p.setLatitude(r.latitude());
        p.setLongitude(r.longitude());
    }
}
