package com.studioos.payment;
/** Invoked under the same Studio lock and transaction as the refund. */
public interface PaymentRefundEffect {void apply(Payment payment);}
