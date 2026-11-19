package com.farmlink.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Razorpay signs "order_id|payment_id" with HMAC-SHA256 using your key secret. */
public final class PaymentSignature {

    private PaymentSignature() {}

    public static String sign(String razorpayOrderId, String razorpayPaymentId, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((razorpayOrderId + "|" + razorpayPaymentId).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("HMAC failure", e);
        }
    }

    public static boolean isValid(String razorpayOrderId, String razorpayPaymentId, String signature, String secret) {
        if (signature == null) return false;
        String expected = sign(razorpayOrderId, razorpayPaymentId, secret);
        // constant-time comparison so timing cannot leak the signature
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
