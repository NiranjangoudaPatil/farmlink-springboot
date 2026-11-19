package com.farmlink.payment;

import com.farmlink.common.ApiException;
import com.farmlink.config.AppProperties;
import com.farmlink.notification.NotificationService;
import com.farmlink.order.Order;
import com.farmlink.order.OrderDto;
import com.farmlink.order.OrderRepository;
import com.farmlink.order.OrderStatus;
import com.farmlink.order.PaymentStatus;
import com.farmlink.payment.PaymentDtos.CreatePaymentRequest;
import com.farmlink.payment.PaymentDtos.CreatePaymentResponse;
import com.farmlink.payment.PaymentDtos.VerifyPaymentRequest;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orders;
    private final RazorpayGateway gateway;
    private final AppProperties props;
    private final NotificationService notifications;

    /** Creates ONE Razorpay order covering all the retailer's selected orders; the amount is computed here. */
    @Transactional
    public CreatePaymentResponse createPayment(UUID retailerId, CreatePaymentRequest req) {
        requireConfigured();
        List<Order> found = orders.findByIdInAndRetailerId(new HashSet<>(req.orderIds()), retailerId);
        if (found.size() != new HashSet<>(req.orderIds()).size()) {
            throw ApiException.notFound("One or more orders were not found");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Order o : found) {
            if (o.getStatus() == OrderStatus.CANCELLED) {
                throw ApiException.badRequest("Order #" + o.getId().toString().substring(0, 8) + " is cancelled");
            }
            if (o.getPaymentStatus() == PaymentStatus.PAID) {
                throw ApiException.conflict("Order #" + o.getId().toString().substring(0, 8) + " is already paid");
            }
            total = total.add(o.getTotalAmount());
        }
        long paise = total.movePointRight(2).longValueExact();

        RazorpayGateway.RazorpayOrder rp = gateway.createOrder(paise, found.get(0).getId().toString());
        found.forEach(o -> o.setRazorpayOrderId(rp.id()));
        return new CreatePaymentResponse(props.razorpay().keyId(), rp.id(), rp.amount(), rp.currency());
    }

    @Transactional
    public List<OrderDto> verify(UUID retailerId, VerifyPaymentRequest req) {
        requireConfigured();
        if (!PaymentSignature.isValid(req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature(),
                props.razorpay().keySecret())) {
            throw ApiException.badRequest("Payment verification failed");
        }
        List<Order> paid = orders.findByRazorpayOrderId(req.razorpayOrderId()).stream()
                .filter(o -> o.getRetailer().getId().equals(retailerId))
                .toList();
        if (paid.isEmpty()) {
            throw ApiException.notFound("No orders found for this payment");
        }
        for (Order o : paid) {
            if (o.getPaymentStatus() == PaymentStatus.PAID) continue; // verify is idempotent
            o.setPaymentStatus(PaymentStatus.PAID);
            o.setRazorpayPaymentId(req.razorpayPaymentId());
            String shortId = o.getId().toString().substring(0, 8);
            notifications.notify(o.getFarmer(), "Payment Received",
                    "Order #" + shortId + " has been paid (" + o.getTotalAmount().toPlainString() + ")",
                    "PAYMENT", o.getId());
            notifications.notify(o.getRetailer(), "Payment Successful",
                    "Payment " + req.razorpayPaymentId() + " for order #" + shortId + " succeeded",
                    "PAYMENT", o.getId());
        }
        return paid.stream().map(OrderDto::from).toList();
    }

    private void requireConfigured() {
        if (!props.razorpay().configured()) {
            throw ApiException.badRequest("Payments are not configured (set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET)");
        }
    }
}
