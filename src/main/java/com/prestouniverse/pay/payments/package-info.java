/**
 * Payment operations: {@link com.prestouniverse.pay.payments.PaymentsClient} with immutable {@code *Request}
 * builders and their responses.
 *
 * <p>Gateway code lists ({@code PaymentStatus}, {@code PaymentMethod}, {@code ErrorCode}, ...) are
 * {@code String} constants rather than enums, because the gateway may add values; compare with
 * {@code equals} and handle unknown values. Amounts are in minor currency units.
 */
package com.prestouniverse.pay.payments;
