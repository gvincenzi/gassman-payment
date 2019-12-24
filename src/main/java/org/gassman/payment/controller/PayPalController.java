package org.gassman.payment.controller;

import com.paypal.api.payments.Payment;
import com.paypal.api.payments.Transaction;
import com.paypal.base.rest.PayPalRESTException;
import org.gassman.payment.client.PayPalClient;
import org.gassman.payment.dto.OrderDTO;
import org.gassman.payment.dto.PaymentDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping(value = "/paypal")
public class PayPalController {
    @Autowired
    private PayPalClient payPalClient;

    @Autowired
    private MessageChannel orderPaymentChannel;

    @GetMapping(value = "/make/payment")
    public void makePayment(@ModelAttribute OrderDTO order, HttpServletResponse httpServletResponse) {
        Map<String, Object> payment = payPalClient.createPayment(order);
        httpServletResponse.setHeader("Location", (String)payment.get("redirect_url"));
        httpServletResponse.setStatus(302);
    }

    @GetMapping("/process")
    public ResponseEntity<String> processPayment(HttpServletRequest request){
        try {
            Payment payment = payPalClient.completePayment(request);
            if ("approved".equalsIgnoreCase(payment.getState()) && !payment.getTransactions().isEmpty()) {
                Transaction transaction = payment.getTransactions().iterator().next();
                PaymentDTO paymentDTO = new PaymentDTO();
                paymentDTO.setPaymentId(payment.getId());
                paymentDTO.setPaymentDateTime(LocalDateTime.parse(payment.getCreateTime(), DateTimeFormatter.ISO_DATE_TIME));
                paymentDTO.setOrderId(Long.parseLong(transaction.getCustom()));
                Message<PaymentDTO> msg = MessageBuilder.withPayload(paymentDTO).build();
                orderPaymentChannel.send(msg);
                return new ResponseEntity<>("Payment succesfully approved", HttpStatus.OK);
            } else {
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, String.format("Payment (%s) was not approved", payment.getId()), null);
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
