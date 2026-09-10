package com.studioos.payment;
import org.springframework.data.repository.Repository;
import java.util.*;
public interface PaymentRefundRepository extends Repository<PaymentRefund,UUID> {
    PaymentRefund save(PaymentRefund refund);
    Optional<PaymentRefund> findByStudioIdAndPaymentId(UUID studioId,UUID paymentId);
}
