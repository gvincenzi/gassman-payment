package org.gassman.payment.controller;

import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.PayPalRESTException;
import org.gassman.payment.client.PayPalClient;
import org.gassman.payment.dto.OrderDTO;
import org.gassman.payment.entity.PaymentType;
import org.gassman.payment.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping(value = "/paypal")
public class PayPalController {
    @Autowired
    private PayPalClient payPalClient;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MessageChannel orderPaymentChannel;

    @GetMapping(value = "/make/payment")
    public void makePayment(@ModelAttribute OrderDTO order, HttpServletResponse httpServletResponse) throws PayPalRESTException {
        Map<String, Object> payment = payPalClient.createPayment(order);
        httpServletResponse.setHeader("Location", (String)payment.get("redirect_url"));
        httpServletResponse.setStatus(302);
    }

    @GetMapping("/process")
    public ResponseEntity<String> processPayment(HttpServletRequest request) throws PayPalRESTException{
        try {
            Payment paymentPayPal = payPalClient.completePayment(request);
            if ("approved".equalsIgnoreCase(paymentPayPal.getState()) && !paymentPayPal.getTransactions().isEmpty()) {
                Transaction transaction = paymentPayPal.getTransactions().iterator().next();
                org.gassman.payment.entity.Payment payment = new org.gassman.payment.entity.Payment();
                payment.setPaymentId(paymentPayPal.getId());
                payment.setPaymentDateTime(LocalDateTime.parse(paymentPayPal.getCreateTime(), DateTimeFormatter.ISO_DATE_TIME));
                payment.setOrderId(Long.parseLong(transaction.getCustom()));
                payment.setPaymentType(PaymentType.PAYPAL);
                paymentRepository.save(payment);
                Message<org.gassman.payment.entity.Payment> msg = MessageBuilder.withPayload(payment).build();
                orderPaymentChannel.send(msg);
                return new ResponseEntity<>("Payment succesfully approved", HttpStatus.OK);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, String.format("Payment (%s) was not approved", paymentPayPal.getId()), null);
            }
        } catch (PayPalRESTException e){
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, String.format("An error occurred during payment process : %s", e.getDetails()), null);
        }
    }

    @GetMapping("/cancel")
    public ResponseEntity<String> cancelPayment(HttpServletRequest request){
        return new ResponseEntity<>("Payment correctly cancelled", HttpStatus.OK);
    }
}
