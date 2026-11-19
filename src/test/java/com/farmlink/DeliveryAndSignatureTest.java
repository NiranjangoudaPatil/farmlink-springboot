package com.farmlink;

import static org.assertj.core.api.Assertions.assertThat;

import com.farmlink.delivery.DeliveryService;
import com.farmlink.payment.PaymentSignature;
import org.junit.jupiter.api.Test;

class DeliveryAndSignatureTest {

    @Test
    void haversineDistanceBetweenBengaluruAndMysuruIsAbout125Km() {
        double km = DeliveryService.distanceKm(12.9716, 77.5946, 12.2958, 76.6394);
        assertThat(km).isBetween(120.0, 130.0);
    }

    @Test
    void sameLocationHasZeroDistance() {
        assertThat(DeliveryService.distanceKm(15.0, 75.0, 15.0, 75.0)).isZero();
    }

    @Test
    void deliveryEstimateIsDrivingTimePlusTwoHoursHandling() {
        assertThat(DeliveryService.estimateHours(80)).isEqualTo(4);   // 80/40 = 2h + 2h
        assertThat(DeliveryService.estimateHours(81)).isEqualTo(5);   // rounds up
    }

    @Test
    void signatureValidatesOnlyForTheRightSecretAndData() {
        String sig = PaymentSignature.sign("order_1", "pay_1", "secret");
        assertThat(PaymentSignature.isValid("order_1", "pay_1", sig, "secret")).isTrue();
        assertThat(PaymentSignature.isValid("order_1", "pay_2", sig, "secret")).isFalse();
        assertThat(PaymentSignature.isValid("order_1", "pay_1", sig, "other-secret")).isFalse();
        assertThat(PaymentSignature.isValid("order_1", "pay_1", null, "secret")).isFalse();
    }
}
