package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.RequestPipeline;
import com.prestouniverse.pay.internal.SdkAccess;

import java.util.List;

final class PaymentsAccess implements SdkAccess.Payments {

    @Override
    public PaymentsClient newPaymentsClient(RequestPipeline pipeline) {
        return new PaymentsClient(pipeline);
    }

    @Override
    public List<PaymentDetail> parsePaymentDetails(String json) {
        return PaymentDetail.parseList(json);
    }
}
