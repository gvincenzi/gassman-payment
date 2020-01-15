package org.gassman.payment.service;

import org.gassman.payment.dto.OrderDTO;
import org.gassman.payment.dto.UserDTO;
import org.gassman.payment.entity.*;
import org.gassman.payment.repository.OrderRepository;
import org.gassman.payment.repository.PaymentRepository;
import org.gassman.payment.repository.RechargeUserCreditLogRepository;
import org.gassman.payment.repository.UserCreditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class InternalPaymentServiceImpl implements InternalPaymentService {
    @Autowired
    private UserCreditRepository userCreditRepository;
    @Autowired
    private RechargeUserCreditLogRepository rechargeUserCreditLogRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private MessageChannel rechargeUserCreditChannel;

    @Override
    public UserCredit userCreditUpdateCredit(UserDTO user, BigDecimal credit, RechargeUserCreditType type){
        Optional<UserCredit> userCredit = userCreditRepository.findById(user.getId());
        UserCredit userCreditInstance;

        // LOG Transaction
        RechargeUserCreditLog log = new RechargeUserCreditLog();
        log.setNewCredit(credit);
        log.setRechargeDateTime(LocalDateTime.now());
        log.setRechargeUserCreditType(type);

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

        return userCreditInstance;
    }

    @Override
    public void processUserRegistration(UserDTO msg) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(msg.getId());
        if (!userCredit.isPresent()) {
            UserCredit userCreditToPersist = new UserCredit(msg.getId(), msg.getName(), msg.getSurname(), msg.getMail(), msg.getTelegramUserId(), BigDecimal.ZERO);
            userCreditRepository.save(userCreditToPersist);
        }
    }

    @Override
    public Order processUserOrder(OrderDTO msg) {
        Optional<Order> order = orderRepository.findById(msg.getOrderId());
        Order orderToPersist = null;
        if (order.isPresent()) {
            orderToPersist = order.get();
        } else {
            orderToPersist = new Order();
        }

        BigDecimal totalToPay = msg.computeTotalToPay();
        orderToPersist.setOrderId(msg.getOrderId());
        orderToPersist.setTotalToPay(totalToPay);

        Optional<UserCredit> userCredit = userCreditRepository.findById(msg.getUser().getId());
        UserCredit orderUserCredit = null;
        if (!userCredit.isPresent()) {
            orderUserCredit = new UserCredit(msg.getUser().getId(), msg.getUser().getName(), msg.getUser().getSurname(), msg.getUser().getMail(), msg.getUser().getTelegramUserId(), BigDecimal.ZERO);
            orderUserCredit = userCreditRepository.save(orderUserCredit);
        } else {
            orderUserCredit = userCredit.get();
        }
        orderToPersist.setUserCredit(orderUserCredit);
        return orderRepository.save(orderToPersist);
    }

    @Override
    public void processUserCancellation(UserDTO msg) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(msg.getId());
        if(userCredit.isPresent()){
            userCredit.get().setCredit(BigDecimal.ZERO);
            rechargeUserCreditLogRepository.deleteAllByUserCredit(userCredit.get());
            userCreditRepository.save(userCredit.get());
        }
    }

    @Override
    public void processOrderCancellation(OrderDTO msg) {
        Optional<Order> order = orderRepository.findById(msg.getOrderId());
        if(order.isPresent()){
            Optional<Payment> payment = paymentRepository.findByOrderId(msg.getOrderId());
            if(payment.isPresent()){
                BigDecimal actualCredit = order.get().getUserCredit().getCredit();
                BigDecimal newCredit = actualCredit.add(order.get().getTotalToPay());
                this.userCreditUpdateCredit(msg.getUser(),newCredit, RechargeUserCreditType.ORDER_CANCELLED);
                paymentRepository.deleteById(payment.get().getPaymentId());
            }
            orderRepository.deleteById(msg.getOrderId());
        }
    }
}
