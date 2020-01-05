package org.gassman.payment.controller;

import org.gassman.payment.dto.OrderDTO;
import org.gassman.payment.dto.UserDTO;
import org.gassman.payment.entity.*;
import org.gassman.payment.repository.PaymentRepository;
import org.gassman.payment.repository.RechargeUserCreditLogRepository;
import org.gassman.payment.repository.UserCreditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
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
    private RechargeUserCreditLogRepository rechargeUserCreditLogRepository;

    @Autowired
    private MessageChannel orderPaymentChannel;

    @Autowired
    private MessageChannel rechargeUserCreditChannel;

    @GetMapping(value = "/make/payment")
    public ResponseEntity<String> makePayment(@ModelAttribute OrderDTO order) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(order.getUser().getId());
        if (!userCredit.isPresent()) {
            return new ResponseEntity<>(String.format("The user ID %d has not an internal credit in GasSMan", order.getUser().getId()), HttpStatus.NOT_ACCEPTABLE);
        } else if (userCredit.get().getCredit().compareTo(order.getTotalToPay()) < 0) {
            return new ResponseEntity<>(String.format("Payment was not approved : insufficient credit. Total to pay : %s - Actual Credit for user ID %d : %s", order.getTotalToPay(), order.getUser().getId(), userCredit.get().getCredit()), HttpStatus.NOT_ACCEPTABLE);
        } else {

            Optional<Payment> paymentPeristed = paymentRepository.findByOrderId(order.getOrderId());
            if(paymentPeristed.isPresent()){
                return new ResponseEntity<>(String.format("This order (ID #%d) has already been paid",order.getOrderId()), HttpStatus.NOT_ACCEPTABLE);
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
    }

    @Transactional
    @DeleteMapping("/{userId}")
    public ResponseEntity<Boolean> removeCredit(@PathVariable("userId") Long userId) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(userId);
        if(userCredit.isPresent()){
            rechargeUserCreditLogRepository.deleteAllByUserCredit(userCredit.get());
            userCreditRepository.deleteById(userId);
            return new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("The user ID %d has not an internal credit in GasSMan",userId), null);
        }
    }

    @PostMapping("/{credit}")
    public ResponseEntity<UserCredit> newCredit(@RequestBody UserDTO user, @PathVariable("credit") BigDecimal credit) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(user.getId());
        UserCredit userCreditInstance;

        // LOG Transaction
        RechargeUserCreditLog log = new RechargeUserCreditLog();
        log.setNewCredit(credit);
        log.setRechargeDateTime(LocalDateTime.now());
        log.setRechargeUserCreditType(RechargeUserCreditType.WEB_ADMIN);

        if (!userCredit.isPresent()) {
            userCreditInstance = new UserCredit(user.getId(), user.getName(), user.getSurname(), user.getMail(), user.getTelegramUserId(), credit);
            log.setOldCredit(BigDecimal.ZERO);
        } else {
            userCreditInstance = userCredit.get();
            log.setOldCredit(userCreditInstance.getCredit());
            userCreditInstance.setCredit(credit);
        }
        userCreditInstance = userCreditRepository.save(userCreditInstance);

        log.setUserCredit(userCreditInstance);

        if(log.getOldCredit().compareTo(log.getNewCredit()) != 0) {
            rechargeUserCreditLogRepository.save(log);
            Message<RechargeUserCreditLog> msg = MessageBuilder.withPayload(log).build();
            rechargeUserCreditChannel.send(msg);
        }

        return new ResponseEntity<>(userCreditInstance, HttpStatus.OK);
    }

    @GetMapping("/{userId}/log")
    public ResponseEntity<List<RechargeUserCreditLog>> findRechargeUserCreditLogByUserId(@PathVariable("userId") Long userId) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(userId);
        List<RechargeUserCreditLog> logs = new ArrayList<>();
        if (userCredit.isPresent()) {
            logs = rechargeUserCreditLogRepository.findAllByUserCreditOrderByRechargeDateTimeDesc(userCredit.get());
        }

        return new ResponseEntity<>(logs, HttpStatus.OK);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserCredit> findCreditByUser(@PathVariable("userId") Long userId) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(userId);
        UserCredit userCreditInstance = new UserCredit(userId, null, null,null, null, BigDecimal.ZERO);
        if (userCredit.isPresent()) {
            userCreditInstance = userCredit.get();
        }
        return new ResponseEntity<>(userCreditInstance, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<UserCredit>> findAllCredit() {
        List<UserCredit> userCredit = userCreditRepository.findAll();
        return new ResponseEntity<>(userCredit,HttpStatus.OK);
    }
}
