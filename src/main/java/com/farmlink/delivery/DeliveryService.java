package com.farmlink.delivery;

import com.farmlink.common.ApiException;
import com.farmlink.product.Product;
import com.farmlink.product.ProductRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Port of the React "delivery distance" logic: warns the retailer when transport time
 * would eat most of the product's shelf life.
 */
@Service
@RequiredArgsConstructor
public class DeliveryService {

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double AVG_SPEED_KMH = 40.0;
    private static final int HANDLING_HOURS = 2;

    private final ProductRepository products;

    public record DeliveryCheck(Double distanceKm, Integer estimatedHours, Integer shelfLifeHours, String warning) {}

    @Transactional(readOnly = true)
    public DeliveryCheck check(UUID productId, double lat, double lng) {
        Product p = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product not found"));
        if (p.getLatitude() == null || p.getLongitude() == null) {
            return new DeliveryCheck(null, null, p.getShelfLifeHours(), null); // farmer location unknown
        }
        double km = distanceKm(p.getLatitude(), p.getLongitude(), lat, lng);
        int hours = estimateHours(km);
        int shelfLife = p.getShelfLifeHours();

        String warning = null;
        if (hours >= shelfLife * 0.8) {
            warning = String.format("This product has a shelf life of %dh but estimated delivery takes ~%dh (%.0f km). "
                    + "Quality may degrade significantly during transport.", shelfLife, hours, km);
        } else if (shelfLife <= 48 && km > 100) {
            warning = String.format("Perishable product (%dh shelf life) with %.0f km delivery distance. "
                    + "Quality may not be maintained.", shelfLife, km);
        }
        return new DeliveryCheck(Math.round(km * 10) / 10.0, hours, shelfLife, warning);
    }

    /** Haversine great-circle distance in km. */
    public static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** ~40 km/h average plus 2h handling. */
    public static int estimateHours(double km) {
        return (int) Math.ceil(km / AVG_SPEED_KMH) + HANDLING_HOURS;
    }
}
