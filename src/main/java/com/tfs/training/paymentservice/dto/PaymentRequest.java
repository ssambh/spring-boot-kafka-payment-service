package com.tfs.training.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO stands for Data Transfer Object.
 * This class is the specific "Envelope" we use to receive incoming data from the customer (Client).
 */
@Data
public class PaymentRequest {

    /**
     * @NotBlank is part of our Validation (The Bouncer).
     * It ensures the customer actually types in an account number and doesn't just leave it blank.
     */
    @NotBlank(message = "Loan account number is required")
    private String loanAccountNumber;

    /**
     * @NotNull ensures they provided an amount.
     * @DecimalMin ensures the amount is at least 1 cent. No negative payments allowed!
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;
}
