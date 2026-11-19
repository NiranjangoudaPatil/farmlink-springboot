package com.farmlink.payment;

import com.farmlink.common.ApiException;
import com.farmlink.config.AppProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Thin client for the Razorpay Orders API (the old Supabase "create-order" edge function). */
@Component
public class RazorpayGateway {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RazorpayOrder(String id, long amount, String currency) {}

    private final AppProperties props;
    private final RestClient client;

    public RazorpayGateway(AppProperties props) {
        this.props = props;
        this.client = RestClient.builder().baseUrl("https://api.razorpay.com/v1").build();
    }

    public RazorpayOrder createOrder(long amountInPaise, String receipt) {
        AppProperties.Razorpay rp = props.razorpay();
        try {
            return client.post()
                    .uri("/orders")
                    .headers(h -> h.setBasicAuth(rp.keyId(), rp.keySecret()))
                    .body(Map.of("amount", amountInPaise, "currency", "INR", "receipt", receipt))
                    .retrieve()
                    .body(RazorpayOrder.class);
        } catch (RestClientException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Payment provider error: " + e.getMessage());
        }
    }
}
