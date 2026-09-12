package com.tfs.training.paymentservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * We create this custom error so our Head Chef (Service) can shout exactly what went wrong.
 * @ResponseStatus tells Spring that if this error is thrown, return a 409 Conflict code to the customer.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String message) {
        super(message);
    }
}
