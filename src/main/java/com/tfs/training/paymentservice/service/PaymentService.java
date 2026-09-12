package com.tfs.training.paymentservice.service;

import com.tfs.training.paymentservice.dto.PaymentRequest;
import com.tfs.training.paymentservice.dto.PaymentResponse;
import com.tfs.training.paymentservice.dto.PaymentProcessedEvent;
import com.tfs.training.paymentservice.entity.Payment;
import com.tfs.training.paymentservice.exception.DuplicatePaymentException;
import com.tfs.training.paymentservice.exception.PaymentNotFoundException;
import com.tfs.training.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Service tells Spring: "This class is the Head Chef. It contains the business logic."
 * @RequiredArgsConstructor (Lombok) acts like an HR manager, automatically hiring the dependencies for us.
 * @Slf4j gives us a megaphone (log) so the Chef can print messages to the console.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, PaymentProcessedEvent> kafkaTemplate;
    
    private static final String PAYMENT_TOPIC = "payment-completed-topic";

    /**
     * @Transactional is the safety net. If ANY part of this method fails or crashes, 
     * it rolls back (undoes) everything so we don't accidentally save half a payment.
     */
    @Transactional
    public PaymentResponse processPayment(String idempotencyKey, PaymentRequest request) {
        log.info("Processing payment for loan account: {} with idempotency key: {}", request.getLoanAccountNumber(), idempotencyKey);

        // 1. Idempotency Check (FinTech is strict!)
        // Check if a payment with this exact ticket number already exists.
        if (paymentRepository.findByIdempotencyKey(idempotencyKey).isPresent()) {
            log.warn("Duplicate payment attempt detected for idempotency key: {}", idempotencyKey);
            throw new DuplicatePaymentException("A payment with this Idempotency Key has already been processed.");
        }

        // 2. Open the Envelope (DTO) and put the data into our Database Blueprint (Entity)
        Payment payment = new Payment();
        payment.setIdempotencyKey(idempotencyKey);
        payment.setLoanAccountNumber(request.getLoanAccountNumber());
        payment.setAmount(request.getAmount());
        payment.setStatus("COMPLETED"); // In real life, we'd call a bank first.
        payment.setTimestamp(LocalDateTime.now());

        // 3. Hand the Blueprint to the Filing Clerk to save in the Database
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment successfully saved with ID: {}", savedPayment.getId());

        // 4. Publish Event to Kafka asynchronously
        PaymentProcessedEvent event = new PaymentProcessedEvent(savedPayment.getLoanAccountNumber(), savedPayment.getAmount());
        kafkaTemplate.send(PAYMENT_TOPIC, event);
        log.info("Published PaymentProcessedEvent to Kafka topic: {}", PAYMENT_TOPIC);

        // 5. Create a Receipt (Response DTO) to hand back to the customer
        return new PaymentResponse(
                savedPayment.getId(),
                savedPayment.getLoanAccountNumber(),
                savedPayment.getAmount(),
                savedPayment.getStatus(),
                savedPayment.getTimestamp(),
                "Payment processed successfully."
        );
    }

    public PaymentResponse getPaymentById(Long id) {
        log.info("Fetching payment with ID: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + id));

        return new PaymentResponse(
                payment.getId(),
                payment.getLoanAccountNumber(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getTimestamp(),
                "Payment retrieved successfully."
        );
    }

    public List<PaymentResponse> getAllPayments() {
        log.info("Fetching all payments from the database.");
        return paymentRepository.findAll().stream()
                .map(payment -> new PaymentResponse(
                        payment.getId(),
                        payment.getLoanAccountNumber(),
                        payment.getAmount(),
                        payment.getStatus(),
                        payment.getTimestamp(),
                        "Payment retrieved successfully."
                ))
                .collect(Collectors.toList());
    }
}
