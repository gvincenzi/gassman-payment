package org.gassman.payment.controller;

import org.gassman.payment.dto.OrderDTO;
import org.gassman.payment.entity.Payment;
import org.gassman.payment.entity.PaymentType;
import org.gassman.payment.entity.UserCredit;
import org.gassman.payment.repository.PaymentRepository;
import org.gassman.payment.repository.UserCreditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = "/internal-credit/")
public class InternalCreditController {
    @Autowired
    private UserCreditRepository userCreditRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MessageChannel orderPaymentChannel;

    @GetMapping(value = "/make/payment")
    public ResponseEntity<String> makePayment(@ModelAttribute OrderDTO order, HttpServletResponse httpServletResponse) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(order.getUser().getId());
        if (!userCredit.isPresent()) {
            return new ResponseEntity<>(String.format("The user ID %d has not an internal credit in GasSMan", order.getUser().getId()), HttpStatus.NOT_ACCEPTABLE);
        } else if (userCredit.get().getCredit().compareTo(order.getTotalToPay()) < 0) {
            return new ResponseEntity<>(String.format("Payment was not approved : insufficient credit. Total to pay : %s - Actual Credit for user ID %d : %s", order.getTotalToPay(), order.getUser().getId(), userCredit.get().getCredit()), HttpStatus.NOT_ACCEPTABLE);
        } else {
            Payment payment = new Payment();
            payment.setPaymentId("INTERNAL_PAYID_" + System.currentTimeMillis());
            payment.setPaymentDateTime(LocalDateTime.now());
            payment.setOrderId(order.getOrderId());
            payment.setPaymentType(PaymentType.INTERNAL_CREDIT);
            paymentRepository.save(payment);
            UserCredit credit = userCredit.get();
            BigDecimal newCredit = credit.getCredit().subtract(order.getTotalToPay());
            credit.setCredit(newCredit);
            userCreditRepository.save(credit);

            Message<org.gassman.payment.entity.Payment> msg = MessageBuilder.withPayload(payment).build();
            orderPaymentChannel.send(msg);
            return new ResponseEntity<>("Payment succesfully approved", HttpStatus.OK);
        }
    }

    @PutMapping("/{userId}/add/{additionalCredit}")
    public ResponseEntity<UserCredit> addCredit(@PathVariable("userId") Long userId, @PathVariable("additionalCredit") BigDecimal additionalCredit) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(userId);
        UserCredit userCreditInstance;
        if (!userCredit.isPresent()) {
            userCreditInstance = new UserCredit(userId, additionalCredit);
        } else {
            userCreditInstance = userCredit.get();
            BigDecimal newCredit = userCreditInstance.getCredit().add(additionalCredit);
            userCreditInstance.setCredit(newCredit);
        }
        userCreditRepository.save(userCreditInstance);
        return new ResponseEntity<>(userCreditInstance, HttpStatus.OK);
    }

    @PutMapping("/{userId}/{credit}")
    public ResponseEntity<UserCredit> newCredit(@PathVariable("userId") Long userId, @PathVariable("credit") BigDecimal credit) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(userId);
        UserCredit userCreditInstance;
        if (!userCredit.isPresent()) {
            userCreditInstance = new UserCredit(userId, credit);
        } else {
            userCreditInstance = userCredit.get();
            userCreditInstance.setCredit(credit);
        }
        userCreditRepository.save(userCreditInstance);
        return new ResponseEntity<>(userCreditInstance, HttpStatus.OK);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserCredit> findCreditByUser(@PathVariable("userId") Long userId) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(userId);
        if (!userCredit.isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, String.format("The user ID %d has not an internal credit in GasSMan", userId), null);
        } else {
            return new ResponseEntity<>(userCredit.get(), HttpStatus.OK);
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserCredit>> findAllCredit() {
        List<UserCredit> userCredit = userCreditRepository.findAll();
        return new ResponseEntity<>(userCredit,HttpStatus.OK);
    }
}
