package com.tfs.training.paymentservice.controller;

import com.tfs.training.paymentservice.dto.PaymentRequest;
import com.tfs.training.paymentservice.dto.PaymentResponse;
import com.tfs.training.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * @RestController tells Spring: "This class is the Receptionist. It handles internet traffic."
 * @RequestMapping("/api/v1/payments") sets the street address for this receptionist.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    // The Receptionist needs the Head Chef to actually do the work!
    private final PaymentService paymentService;

    /**
     * @PostMapping means "When someone sends data (a POST request) to our address, run this method."
     * @RequestHeader("Idempotency-Key") forces the customer to provide that secret unique code we talked about.
     * @Valid activates the Bouncer! It checks the @NotBlank and @DecimalMin rules in our DTO before the Chef even sees it.
     * @RequestBody tells Spring to take the JSON data from the internet and convert it into our PaymentRequest envelope.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        // 1. The Receptionist hands the envelope to the Chef.
        PaymentResponse response = paymentService.processPayment(idempotencyKey, request);

        // 2. The Receptionist takes the receipt from the Chef and hands it back to the customer with a 201 Created status.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * @GetMapping means "When someone sends a GET request to /api/v1/payments, run this method."
     * Used to fetch all payments in the system.
     */
    @GetMapping
    public ResponseEntity<java.util.List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * @GetMapping("/{id}") means "When someone sends a GET request to /api/v1/payments/1, run this method."
     * @PathVariable extracts the "1" from the URL and passes it to the method.
     */
    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long id) {
        return ResponseEntity.ok(paymentService.getPaymentById(id));
    }
}
