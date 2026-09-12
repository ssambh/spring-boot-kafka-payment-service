package com.tfs.training.paymentservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * This is the "Envelope" we send BACK to the customer after we process their payment.
 * Notice we don't include internal database things they don't need to see.
 */
@Data
@AllArgsConstructor
public class PaymentResponse {
    
    // We send back the generated ID so they have a receipt number
    private Long paymentId;
    
    private String loanAccountNumber;
    private BigDecimal amount;
    private String status;
    private LocalDateTime timestamp;
    
    // A nice friendly message
    private String message;
}
