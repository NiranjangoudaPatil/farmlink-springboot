package com.farmlink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.farmlink.payment.PaymentSignature;
import com.farmlink.payment.RazorpayGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** End-to-end flow against the real Spring context + Flyway schema (H2 in MySQL mode). */
@SpringBootTest
@AutoConfigureMockMvc
class FarmLinkFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @MockBean RazorpayGateway razorpay;

    @Test
    void fullTradingFlow_registerListOrderConfirmDeliverReview() throws Exception {
        String farmer = register("farmer1@test.com", "FARMER");
        String retailer = register("retailer1@test.com", "RETAILER");

        // public marketplace works without a token
        mvc.perform(get("/api/products")).andExpect(status().isOk());
        // protected endpoints reject anonymous callers
        mvc.perform(get("/api/orders/mine")).andExpect(status().isUnauthorized());
        // role rules: a retailer cannot create products, a farmer cannot place orders
        mvc.perform(post("/api/products").header("Authorization", bearer(retailer))
                .contentType(MediaType.APPLICATION_JSON).content(productJson("Nope", "10"))).andExpect(status().isForbidden());

        // farmer lists a product (50 kg)
        String productId = data(call(post("/api/products"), farmer, productJson("Tomatoes", "50")), "id");

        // farmer cannot place an order
        mvc.perform(post("/api/orders").header("Authorization", bearer(farmer))
                .contentType(MediaType.APPLICATION_JSON).content(orderJson(productId, "5"))).andExpect(status().isForbidden());

        // ordering more than the stock is rejected
        mvc.perform(post("/api/orders").header("Authorization", bearer(retailer))
                .contentType(MediaType.APPLICATION_JSON).content(orderJson(productId, "1000"))).andExpect(status().isBadRequest());

        // retailer orders 10 kg @ 20 = 200, server computes the total
        JsonNode orders = node(call(post("/api/orders"), retailer, orderJson(productId, "10")));
        String orderId = orders.path("data").get(0).path("id").asText();
        assertThat(orders.path("data").get(0).path("totalAmount").decimalValue()).isEqualByComparingTo("200");
        assertThat(orders.path("data").get(0).path("status").asText()).isEqualTo("PENDING");

        // farmer got an in-app notification
        mvc.perform(get("/api/notifications/unread-count").header("Authorization", bearer(farmer)))
                .andExpect(jsonPath("$.data.unread").value(2)); // welcome + new order

        // a retailer cannot confirm; the farmer can, and stock drops 50 -> 40
        patchStatus(retailer, orderId, "CONFIRMED").andExpect(status().isBadRequest());
        patchStatus(farmer, orderId, "CONFIRMED").andExpect(status().isOk());
        mvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.data.quantityAvailable").value(40.0));

        // cannot review before delivery
        mvc.perform(post("/api/reviews").header("Authorization", bearer(retailer))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\":\"" + orderId + "\",\"rating\":5,\"comment\":\"great\"}"))
                .andExpect(status().isBadRequest());

        patchStatus(farmer, orderId, "SHIPPED").andExpect(status().isOk());
        patchStatus(farmer, orderId, "DELIVERED").andExpect(status().isOk());

        // review once (201), second time conflicts (409)
        String review = "{\"orderId\":\"" + orderId + "\",\"rating\":4,\"comment\":\"fresh\"}";
        mvc.perform(post("/api/reviews").header("Authorization", bearer(retailer))
                .contentType(MediaType.APPLICATION_JSON).content(review)).andExpect(status().isCreated());
        mvc.perform(post("/api/reviews").header("Authorization", bearer(retailer))
                .contentType(MediaType.APPLICATION_JSON).content(review)).andExpect(status().isConflict());

        String farmerId = data(call(get("/api/users/me"), farmer, null), "id");
        mvc.perform(get("/api/farmers/" + farmerId + "/rating"))
                .andExpect(jsonPath("$.data.average").value(4.0))
                .andExpect(jsonPath("$.data.count").value(1));

        mvc.perform(get("/api/farmers/me/analytics").header("Authorization", bearer(farmer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalOrders").value(1))
                .andExpect(jsonPath("$.data.topProducts[0].name").value("Tomatoes"));
    }

    @Test
    void cancellingAConfirmedOrderGivesStockBack() throws Exception {
        String farmer = register("farmer2@test.com", "FARMER");
        String retailer = register("retailer2@test.com", "RETAILER");
        String productId = data(call(post("/api/products"), farmer, productJson("Onions", "30")), "id");
        String orderId = node(call(post("/api/orders"), retailer, orderJson(productId, "10")))
                .path("data").get(0).path("id").asText();

        patchStatus(farmer, orderId, "CONFIRMED").andExpect(status().isOk());
        mvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.data.quantityAvailable").value(20.0));
        patchStatus(farmer, orderId, "CANCELLED").andExpect(status().isOk());
        mvc.perform(get("/api/products/" + productId)).andExpect(jsonPath("$.data.quantityAvailable").value(30.0));
    }

    @Test
    void authValidation() throws Exception {
        register("dup@test.com", "RETAILER");
        String body = registerJson("dup@test.com", "RETAILER");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerJson("boss@test.com", "ADMIN"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"dup@test.com\",\"password\":\"wrong-password\"}")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanVerifyUsersAndSeeAnalytics_othersCannot() throws Exception {
        String farmer = register("farmer3@test.com", "FARMER");
        String admin = data(node(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@farmlink.test\",\"password\":\"admin-pass-123\"}"))
                .andExpect(status().isOk())), "token");

        mvc.perform(get("/api/admin/analytics").header("Authorization", bearer(farmer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/analytics").header("Authorization", bearer(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usersByRole.ADMIN").value(1));

        String farmerId = data(call(get("/api/users/me"), farmer, null), "id");
        mvc.perform(patch("/api/admin/users/" + farmerId + "/verification").header("Authorization", bearer(admin))
                .contentType(MediaType.APPLICATION_JSON).content("{\"verified\":true}"))
                .andExpect(jsonPath("$.data.verified").value(true));
    }

    @Test
    void paymentCreateAndVerify() throws Exception {
        when(razorpay.createOrder(anyLong(), anyString()))
                .thenReturn(new RazorpayGateway.RazorpayOrder("order_TEST123", 20000, "INR"));

        String farmer = register("farmer4@test.com", "FARMER");
        String retailer = register("retailer4@test.com", "RETAILER");
        String productId = data(call(post("/api/products"), farmer, productJson("Wheat", "100")), "id");
        String orderId = node(call(post("/api/orders"), retailer, orderJson(productId, "10")))
                .path("data").get(0).path("id").asText();

        mvc.perform(post("/api/payments/create-order").header("Authorization", bearer(retailer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"orderIds\":[\"" + orderId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.razorpayOrderId").value("order_TEST123"))
                .andExpect(jsonPath("$.data.razorpayKeyId").value("rzp_test_key"));

        // forged signature is rejected
        String bad = "{\"razorpayOrderId\":\"order_TEST123\",\"razorpayPaymentId\":\"pay_1\",\"razorpaySignature\":\"forged\"}";
        mvc.perform(post("/api/payments/verify").header("Authorization", bearer(retailer))
                .contentType(MediaType.APPLICATION_JSON).content(bad)).andExpect(status().isBadRequest());

        // correct HMAC marks the order paid
        String sig = PaymentSignature.sign("order_TEST123", "pay_1", "test-razorpay-secret");
        String good = "{\"razorpayOrderId\":\"order_TEST123\",\"razorpayPaymentId\":\"pay_1\",\"razorpaySignature\":\"" + sig + "\"}";
        mvc.perform(post("/api/payments/verify").header("Authorization", bearer(retailer))
                        .contentType(MediaType.APPLICATION_JSON).content(good))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].paymentStatus").value("PAID"));
    }

    // ---------- helpers ----------

    private String register(String email, String role) throws Exception {
        return data(node(mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(email, role))).andExpect(status().isCreated())), "token");
    }

    private String registerJson(String email, String role) throws Exception {
        return json.writeValueAsString(Map.of("email", email, "password", "secret123", "fullName", "User " + email,
                "role", role, "location", "Dharwad", "latitude", 15.45, "longitude", 75.0));
    }

    private String productJson(String name, String qty) throws Exception {
        return json.writeValueAsString(Map.of("name", name, "category", "vegetables", "pricePerUnit", "20.00",
                "unit", "kg", "quantityAvailable", qty, "shelfLifeHours", 72));
    }

    private String orderJson(String productId, String qty) throws Exception {
        return json.writeValueAsString(Map.of("items", java.util.List.of(Map.of("productId", productId, "quantity", qty)),
                "deliveryAddress", "Hubli market", "deliveryLatitude", 15.36, "deliveryLongitude", 75.12));
    }

    private ResultActions patchStatus(String token, String orderId, String status) throws Exception {
        return mvc.perform(patch("/api/orders/" + orderId + "/status").header("Authorization", bearer(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + status + "\"}"));
    }

    private ResultActions call(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder b,
                               String token, String body) throws Exception {
        b.header("Authorization", bearer(token));
        if (body != null) b.contentType(MediaType.APPLICATION_JSON).content(body);
        return mvc.perform(b).andExpect(status().is2xxSuccessful());
    }

    private JsonNode node(ResultActions r) throws Exception {
        return json.readTree(r.andReturn().getResponse().getContentAsString());
    }

    private String data(ResultActions r, String field) throws Exception {
        return data(node(r), field);
    }

    private String data(JsonNode root, String field) {
        JsonNode d = root.path("data");
        return d.has(field) ? d.path(field).asText() : d.path("user").path(field).asText();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    @SuppressWarnings("unused")
    private static UUID id() {
        return UUID.randomUUID();
    }
}
