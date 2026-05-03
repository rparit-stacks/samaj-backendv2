package com.rps.samaj.donation;

import com.rps.samaj.api.dto.DonationDtos;
import com.rps.samaj.config.app.RuntimeConfigService;
import com.rps.samaj.user.model.UserProfile;
import com.rps.samaj.user.repository.UserProfileRepository;
import com.rps.samaj.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DonationService {

    static final String KEY_RAZORPAY_KEY_ID = "razorpay.key-id";
    static final String KEY_RAZORPAY_KEY_SECRET = "razorpay.key-secret";
    static final String KEY_RAZORPAY_ENABLED = "razorpay.enabled";
    static final String KEY_DONATION_MIN_PAISE = "donation.min-amount-paise";
    static final String KEY_DONATION_MAX_PAISE = "donation.max-amount-paise";

    private static final long DEFAULT_MIN_PAISE = 5000L;      // ₹50
    private static final long DEFAULT_MAX_PAISE = 10_000_000L; // ₹1,00,000

    private final DonationRepository donationRepository;
    private final RuntimeConfigService runtimeConfig;
    private final UserRepository userRepository;
    private final UserProfileRepository profileRepository;

    public DonationService(
            DonationRepository donationRepository,
            RuntimeConfigService runtimeConfig,
            UserRepository userRepository,
            UserProfileRepository profileRepository
    ) {
        this.donationRepository = donationRepository;
        this.runtimeConfig = runtimeConfig;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
    }

    // ==================== CONFIG ====================

    public DonationDtos.DonationPublicConfigResponse getPublicConfig() {
        return new DonationDtos.DonationPublicConfigResponse(
                isEnabled(),
                getMinAmountPaise(),
                getMaxAmountPaise(),
                runtimeConfig.getString(KEY_RAZORPAY_KEY_ID, "")
        );
    }

    public DonationDtos.DonationAdminConfigResponse getAdminConfig() {
        String keyId = runtimeConfig.getString(KEY_RAZORPAY_KEY_ID, "");
        boolean configured = !keyId.isBlank()
                && !runtimeConfig.getString(KEY_RAZORPAY_KEY_SECRET, "").isBlank();
        return new DonationDtos.DonationAdminConfigResponse(
                isEnabled(),
                getMinAmountPaise(),
                getMaxAmountPaise(),
                keyId,
                configured
        );
    }

    @Transactional
    public DonationDtos.DonationAdminConfigResponse updateConfig(
            DonationDtos.DonationConfigUpdateRequest body,
            UUID adminId
    ) {
        if (body.keyId() != null) {
            runtimeConfig.upsert(KEY_RAZORPAY_KEY_ID, body.keyId().trim(), adminId);
        }
        if (body.keySecret() != null && !body.keySecret().isBlank()) {
            runtimeConfig.upsert(KEY_RAZORPAY_KEY_SECRET, body.keySecret().trim(), adminId);
        }
        if (body.enabled() != null) {
            runtimeConfig.upsert(KEY_RAZORPAY_ENABLED, String.valueOf(body.enabled()), adminId);
        }
        if (body.minAmountPaise() != null && body.minAmountPaise() > 0) {
            runtimeConfig.upsert(KEY_DONATION_MIN_PAISE, String.valueOf(body.minAmountPaise()), adminId);
        }
        if (body.maxAmountPaise() != null && body.maxAmountPaise() > 0) {
            runtimeConfig.upsert(KEY_DONATION_MAX_PAISE, String.valueOf(body.maxAmountPaise()), adminId);
        }
        return getAdminConfig();
    }

    // ==================== ORDER ====================

    @Transactional
    @SuppressWarnings("unchecked")
    public DonationDtos.CreateOrderResponse createOrder(UUID userId, DonationDtos.CreateOrderRequest body) {
        if (!isEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Donations are not currently enabled");
        }

        String keyId = runtimeConfig.getString(KEY_RAZORPAY_KEY_ID, "");
        String keySecret = runtimeConfig.getString(KEY_RAZORPAY_KEY_SECRET, "");
        if (keyId.isBlank() || keySecret.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Payment gateway is not configured");
        }

        long minPaise = getMinAmountPaise();
        long maxPaise = getMaxAmountPaise();
        if (body.amountPaise() < minPaise || body.amountPaise() > maxPaise) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Amount must be between ₹" + (minPaise / 100) + " and ₹" + (maxPaise / 100));
        }

        String receipt = "don_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        Map<String, Object> rzpOrder;
        try {
            rzpOrder = RestClient.create()
                    .post()
                    .uri("https://api.razorpay.com/v1/orders")
                    .headers(h -> h.setBasicAuth(keyId, keySecret))
                    .body(Map.of("amount", body.amountPaise(), "currency", "INR", "receipt", receipt))
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to create payment order: " + e.getMessage());
        }

        if (rzpOrder == null || rzpOrder.get("id") == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid response from payment gateway");
        }

        String orderId = (String) rzpOrder.get("id");

        UserProfile profile = profileRepository.findById(userId).orElse(null);
        String userName = (profile != null && profile.getFullName() != null && !profile.getFullName().isBlank())
                ? profile.getFullName() : "Member";

        DonationEntity entity = new DonationEntity();
        entity.setUserId(userId);
        entity.setUserName(userName);
        entity.setAmountPaise(body.amountPaise());
        entity.setCurrency("INR");
        entity.setRazorpayOrderId(orderId);
        entity.setStatus("PENDING");
        if (body.notes() != null && !body.notes().isBlank()) {
            entity.setNotes(body.notes().trim());
        }
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        donationRepository.save(entity);

        return new DonationDtos.CreateOrderResponse(orderId, body.amountPaise(), "INR", keyId);
    }

    // ==================== VERIFY ====================

    @Transactional
    public DonationDtos.DonationItem verifyPayment(UUID userId, DonationDtos.VerifyPaymentRequest body) {
        DonationEntity entity = donationRepository.findByRazorpayOrderId(body.razorpayOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Donation order not found"));

        if (!entity.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if ("SUCCESS".equals(entity.getStatus())) {
            return toItem(entity);
        }

        String keySecret = runtimeConfig.getString(KEY_RAZORPAY_KEY_SECRET, "");
        if (!verifySignature(body.razorpayOrderId(), body.razorpayPaymentId(), body.razorpaySignature(), keySecret)) {
            entity.setStatus("FAILED");
            entity.setUpdatedAt(Instant.now());
            donationRepository.save(entity);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment signature verification failed");
        }

        entity.setRazorpayPaymentId(body.razorpayPaymentId());
        entity.setRazorpaySignature(body.razorpaySignature());
        entity.setStatus("SUCCESS");
        entity.setUpdatedAt(Instant.now());
        if (body.notes() != null && !body.notes().isBlank() && entity.getNotes() == null) {
            entity.setNotes(body.notes().trim());
        }
        donationRepository.save(entity);

        return toItem(entity);
    }

    // ==================== USER QUERIES ====================

    @Transactional(readOnly = true)
    public DonationDtos.DonationPageResponse myDonations(UUID userId, int page, int size) {
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), 50);
        Page<DonationEntity> result = donationRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(validPage, validSize));
        return toPageResponse(result);
    }

    // ==================== ADMIN QUERIES ====================

    @Transactional(readOnly = true)
    public DonationDtos.DonationPageResponse adminList(int page, int size, String status) {
        int validPage = Math.max(page, 0);
        int validSize = Math.min(Math.max(size, 1), 100);
        Page<DonationEntity> result;
        if (status != null && !status.isBlank()) {
            result = donationRepository.findByStatusOrderByCreatedAtDesc(
                    status.toUpperCase(), PageRequest.of(validPage, validSize));
        } else {
            result = donationRepository.findAll(
                    PageRequest.of(validPage, validSize, Sort.by(Sort.Direction.DESC, "createdAt")));
        }
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    public DonationDtos.DonationStatsResponse adminStats() {
        Instant monthStart = Instant.now().minus(30, ChronoUnit.DAYS);
        return new DonationDtos.DonationStatsResponse(
                donationRepository.sumSuccessAmountPaise(),
                donationRepository.sumSuccessAmountPaiseSince(monthStart),
                donationRepository.countDistinctDonors(),
                donationRepository.countByStatus("SUCCESS"),
                donationRepository.countByStatus("FAILED"),
                donationRepository.countByStatus("PENDING")
        );
    }

    // ==================== HELPERS ====================

    private boolean isEnabled() {
        return runtimeConfig.getBoolean(KEY_RAZORPAY_ENABLED, false);
    }

    private long getMinAmountPaise() {
        try {
            String v = runtimeConfig.getString(KEY_DONATION_MIN_PAISE, "");
            return v.isBlank() ? DEFAULT_MIN_PAISE : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return DEFAULT_MIN_PAISE;
        }
    }

    private long getMaxAmountPaise() {
        try {
            String v = runtimeConfig.getString(KEY_DONATION_MAX_PAISE, "");
            return v.isBlank() ? DEFAULT_MAX_PAISE : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return DEFAULT_MAX_PAISE;
        }
    }

    private static boolean verifySignature(String orderId, String paymentId, String signature, String secret) {
        try {
            String message = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            String expected = HexFormat.of().formatHex(hash);
            return expected.equalsIgnoreCase(signature);
        } catch (Exception e) {
            return false;
        }
    }

    private DonationDtos.DonationItem toItem(DonationEntity e) {
        return new DonationDtos.DonationItem(
                e.getId(),
                e.getUserId() != null ? e.getUserId().toString() : null,
                e.getUserName(),
                e.getAmountPaise(),
                e.getCurrency(),
                e.getStatus(),
                e.getRazorpayOrderId(),
                e.getRazorpayPaymentId(),
                e.getNotes(),
                e.getCreatedAt() != null ? e.getCreatedAt().toString() : null
        );
    }

    private DonationDtos.DonationPageResponse toPageResponse(Page<DonationEntity> page) {
        List<DonationDtos.DonationItem> content = page.getContent().stream().map(this::toItem).toList();
        return new DonationDtos.DonationPageResponse(
                content,
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber(),
                page.isFirst(),
                page.isLast()
        );
    }
}
