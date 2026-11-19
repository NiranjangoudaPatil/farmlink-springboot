package com.farmlink.order;

import com.farmlink.common.ApiException;
import com.farmlink.notification.NotificationService;
import com.farmlink.product.Product;
import com.farmlink.product.ProductRepository;
import com.farmlink.security.AuthUser;
import com.farmlink.user.Role;
import com.farmlink.user.User;
import com.farmlink.user.UserService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderService {

    /** Who may move an order from one status to which. */
    private static final Map<OrderStatus, Set<OrderStatus>> FARMER_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
            OrderStatus.CONFIRMED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, Set.of(OrderStatus.DELIVERED));
    private static final Map<OrderStatus, Set<OrderStatus>> RETAILER_TRANSITIONS = Map.of(
            OrderStatus.PENDING, Set.of(OrderStatus.CANCELLED));

    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserService userService;
    private final NotificationService notifications;

    @Transactional
    public List<OrderDto> place(UUID retailerId, PlaceOrderRequest req) {
        User retailer = userService.getEntity(retailerId);

        // merge duplicate lines for the same product
        Map<UUID, BigDecimal> wanted = new LinkedHashMap<>();
        req.items().forEach(l -> wanted.merge(l.productId(), l.quantity(), BigDecimal::add));

        List<Product> found = products.findAllByIdIn(wanted.keySet());
        if (found.size() != wanted.size()) {
            throw ApiException.notFound("One or more products no longer exist");
        }

        Map<UUID, List<Product>> byFarmer = new LinkedHashMap<>();
        for (Product p : found) {
            BigDecimal qty = wanted.get(p.getId());
            if (!p.isAvailable()) {
                throw ApiException.badRequest(p.getName() + " is not available");
            }
            if (qty.compareTo(p.getQuantityAvailable()) > 0) {
                throw ApiException.badRequest("Only " + p.getQuantityAvailable().stripTrailingZeros().toPlainString()
                        + " " + p.getUnit() + " of " + p.getName() + " available");
            }
            byFarmer.computeIfAbsent(p.getFarmer().getId(), k -> new ArrayList<>()).add(p);
        }

        List<OrderDto> created = new ArrayList<>();
        for (List<Product> farmerProducts : byFarmer.values()) {
            Order order = new Order();
            order.setRetailer(retailer);
            order.setFarmer(farmerProducts.get(0).getFarmer());
            order.setDeliveryAddress(req.deliveryAddress());
            order.setDeliveryLatitude(req.deliveryLatitude());
            order.setDeliveryLongitude(req.deliveryLongitude());
            order.setNotes(req.notes());

            BigDecimal total = BigDecimal.ZERO;
            for (Product p : farmerProducts) {
                BigDecimal qty = wanted.get(p.getId());
                OrderItem item = new OrderItem();
                item.setProduct(p);
                item.setProductName(p.getName());
                item.setQuantity(qty);
                item.setPricePerUnit(p.getPricePerUnit());
                item.setTotalPrice(p.getPricePerUnit().multiply(qty)); // price is read server-side, never trusted from client
                order.addItem(item);
                total = total.add(item.getTotalPrice());
            }
            order.setTotalAmount(total);
            orders.save(order);

            notifications.notify(order.getFarmer(), "New Order Received",
                    "Order #" + shortId(order.getId()) + " placed by " + retailer.getFullName()
                            + " (total " + total.toPlainString() + ")", "ORDER", order.getId());
            notifications.notify(retailer, "Order Placed",
                    "Your order #" + shortId(order.getId()) + " was sent to " + order.getFarmer().getFullName(),
                    "ORDER", order.getId());
            created.add(OrderDto.from(order));
        }
        return created;
    }

    @Transactional(readOnly = true)
    public List<OrderDto> forRetailer(UUID retailerId) {
        return orders.findByRetailerIdOrderByCreatedAtDesc(retailerId).stream().map(OrderDto::from).toList();
    }

    @Transactional(readOnly = true)
    public List<OrderDto> forFarmer(UUID farmerId) {
        return orders.findByFarmerIdOrderByCreatedAtDesc(farmerId).stream().map(OrderDto::from).toList();
    }

    @Transactional(readOnly = true)
    public OrderDto get(UUID orderId, AuthUser me) {
        Order o = find(orderId);
        boolean participant = o.getRetailer().getId().equals(me.id()) || o.getFarmer().getId().equals(me.id());
        if (!participant && me.role() != Role.ADMIN) {
            throw ApiException.forbidden("You are not part of this order");
        }
        return OrderDto.from(o);
    }

    @Transactional
    public OrderDto updateStatus(UUID orderId, AuthUser me, OrderStatus next) {
        Order o = find(orderId);
        boolean isFarmer = o.getFarmer().getId().equals(me.id());
        boolean isRetailer = o.getRetailer().getId().equals(me.id());
        if (!isFarmer && !isRetailer) {
            throw ApiException.forbidden("You are not part of this order");
        }

        OrderStatus current = o.getStatus();
        Map<OrderStatus, Set<OrderStatus>> allowed = isFarmer ? FARMER_TRANSITIONS : RETAILER_TRANSITIONS;
        if (!allowed.getOrDefault(current, Set.of()).contains(next)) {
            throw ApiException.badRequest("You cannot change an order from " + current + " to " + next);
        }

        if (next == OrderStatus.CONFIRMED) {
            adjustStock(o, true);
        } else if (next == OrderStatus.CANCELLED && current != OrderStatus.PENDING) {
            adjustStock(o, false); // stock was reserved at confirmation, give it back
        }
        o.setStatus(next);

        User other = isFarmer ? o.getRetailer() : o.getFarmer();
        notifications.notify(other, "Order Status Updated",
                "Order #" + shortId(o.getId()) + " is now " + next.name().toLowerCase(), "ORDER", o.getId());
        return OrderDto.from(o);
    }

    /** Takes (or returns) the ordered quantities from product stock under a row lock. */
    private void adjustStock(Order o, boolean take) {
        List<OrderItem> sorted = o.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getProduct().getId())) // fixed lock order avoids deadlocks
                .toList();
        for (OrderItem item : sorted) {
            Product p = products.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> ApiException.notFound("Product no longer exists"));
            if (take) {
                if (p.getQuantityAvailable().compareTo(item.getQuantity()) < 0) {
                    throw ApiException.conflict("Not enough stock available for " + p.getName());
                }
                p.setQuantityAvailable(p.getQuantityAvailable().subtract(item.getQuantity()));
            } else {
                p.setQuantityAvailable(p.getQuantityAvailable().add(item.getQuantity()));
            }
        }
    }

    private Order find(UUID id) {
        return orders.findDetailedById(id).orElseThrow(() -> ApiException.notFound("Order not found"));
    }

    static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}
