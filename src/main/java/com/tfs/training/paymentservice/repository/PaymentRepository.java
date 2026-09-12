package com.tfs.training.paymentservice.repository;

import com.tfs.training.paymentservice.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * @Repository tells Spring that this interface is responsible for interacting with the database.
 * We extend JpaRepository, which automatically gives us standard tools like save(), findById(), and delete() without writing any code!
 * <Payment, Long> means this repository manages the 'Payment' entity, and the primary key (id) is of type 'Long'.
 */
@Repository
public interface  PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Spring Data JPA is so smart that it reads the name of this method and automatically writes the SQL query for it!
     * It will generate: SELECT * FROM payments WHERE idempotency_key = ?
     * Optional<> is a safe box. It means "We might find a Payment, or the box might be empty (null)."
     */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
