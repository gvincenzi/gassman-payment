package org.gassman.payment.controller;

import org.gassman.payment.client.OrderResourceClient;
import org.gassman.payment.dto.OrderDTO;
import org.gassman.payment.dto.UserDTO;
import org.gassman.payment.entity.*;
import org.gassman.payment.repository.OrderRepository;
import org.gassman.payment.repository.PaymentRepository;
import org.gassman.payment.repository.RechargeUserCreditLogRepository;
import org.gassman.payment.repository.UserCreditRepository;
import org.gassman.payment.service.InternalPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private OrderRepository orderRepository;

    @Autowired
    private MessageChannel orderPaymentChannel;

    @Autowired
    private InternalPaymentService internalPaymentService;

    @Autowired
    private OrderResourceClient orderResourceClient;

    @Value("${message.userNotFound}")
    public String userNotFound;

    @Value("${message.insufficientCredit}")
    public String insufficientCredit;

    @Value("${message.alreadyPaid}")
    public String alreadyPaid;

    @Value("${message.paymentApproved}")
    public String paymentApproved;

    @Value("${message.orderNotExist}")
    public String orderNotExist;

    @GetMapping(value = "/make/payment/{orderId}")
    public ResponseEntity<String> makePayment(@PathVariable("orderId") Long orderId) {
        Optional<Order> orderToPay = orderRepository.findById(orderId);
        Order order = null;
        if (!orderToPay.isPresent()) {
            OrderDTO orderToPayRemote = orderResourceClient.findOrderById(orderId);
            if(orderToPayRemote != null){
                order = internalPaymentService.processUserOrder(orderToPayRemote);
            } else {
                return new ResponseEntity<>(String.format(orderNotExist, orderId), HttpStatus.NOT_ACCEPTABLE);
            }
        } else {
            order = orderToPay.get();
        }

        UserCredit userCredit = order.getUserCredit();
        if (userCredit.getCredit().compareTo(order.getTotalToPay()) < 0) {
            return new ResponseEntity<>(String.format(insufficientCredit, order.getTotalToPay(), userCredit.getUserId(), userCredit.getCredit()), HttpStatus.NOT_ACCEPTABLE);
        } else {
            Optional<Payment> paymentPeristed = paymentRepository.findByOrderId(order.getOrderId());
            if(paymentPeristed.isPresent()){

                return new ResponseEntity<>(String.format(alreadyPaid,order.getOrderId()), HttpStatus.NOT_ACCEPTABLE);
            } else {
                Payment payment = new Payment();
                payment.setPaymentId("INTERNAL_PAYID_" + System.currentTimeMillis());
                payment.setPaymentDateTime(LocalDateTime.now());
                payment.setOrderId(order.getOrderId());
                payment.setPaymentType(PaymentType.INTERNAL_CREDIT);
                paymentRepository.save(payment);
                BigDecimal newCredit = userCredit.getCredit().subtract(order.getTotalToPay());
                userCredit.setCredit(newCredit);
                userCreditRepository.save(userCredit);

                Message<org.gassman.payment.entity.Payment> msg = MessageBuilder.withPayload(payment).build();
                orderPaymentChannel.send(msg);
                return new ResponseEntity<>(paymentApproved, HttpStatus.OK);
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format(userNotFound,userId), null);
        }
    }

    @PostMapping("/{credit}")
    public ResponseEntity<UserCredit> newCredit(@RequestBody UserDTO user, @PathVariable("credit") BigDecimal credit) {
        UserCredit userCreditInstance = internalPaymentService.userCreditUpdateCredit(user,credit,RechargeUserCreditType.WEB_ADMIN);
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

    @GetMapping("/{userId}/order")
    public ResponseEntity<List<Order>> findOrdersByUser(@PathVariable("userId") Long userId) {
        return new ResponseEntity<>(orderRepository.findByUserCreditUserId(userId), HttpStatus.OK);
    }

    @GetMapping("/totalUserCredit")
    public ResponseEntity<BigDecimal> totalUserCredit() {
        BigDecimal total = BigDecimal.ZERO;
        for(UserCredit userCredit : userCreditRepository.findAll()){
            total = total.add(userCredit.getCredit());
        }
        return new ResponseEntity<>(total, HttpStatus.OK);
    }

    @DeleteMapping("/{userId}/order/{orderId}")
    public ResponseEntity<Boolean> findOrdersByUser(@PathVariable("userId") Long userId, @PathVariable("orderId") Long orderId) {
        Optional<Order> orderOptional = orderRepository.findById(orderId);
        if(orderOptional.isPresent()){
            orderRepository.deleteById(orderId);
            return new ResponseEntity<>(Boolean.TRUE, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(Boolean.FALSE, HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping
    public ResponseEntity<List<UserCredit>> findAllCredit() {
        List<UserCredit> userCredit = userCreditRepository.findAll();
        return new ResponseEntity<>(userCredit,HttpStatus.OK);
    }
}
