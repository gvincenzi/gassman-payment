package org.gassman.payment.listener;

import org.gassman.payment.binding.MQBinding;
import org.gassman.payment.dto.OrderDTO;
import org.gassman.payment.dto.UserDTO;
import org.gassman.payment.entity.Order;
import org.gassman.payment.entity.Payment;
import org.gassman.payment.entity.RechargeUserCreditType;
import org.gassman.payment.entity.UserCredit;
import org.gassman.payment.repository.OrderRepository;
import org.gassman.payment.repository.PaymentRepository;
import org.gassman.payment.repository.RechargeUserCreditLogRepository;
import org.gassman.payment.repository.UserCreditRepository;
import org.gassman.payment.service.InternalPaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@EnableBinding(MQBinding.class)
public class MQListener {
    @Autowired
    private UserCreditRepository userCreditRepository;

    @Autowired
    private RechargeUserCreditLogRepository rechargeUserCreditLogRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InternalPaymentService internalPaymentService;

    @StreamListener(target = MQBinding.USER_REGISTRATION)
    public void processUserRegistration(UserDTO msg) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(msg.getId());
        if (!userCredit.isPresent()) {
            UserCredit userCreditToPersist = new UserCredit(msg.getId(), msg.getName(), msg.getSurname(), msg.getMail(), msg.getTelegramUserId(), BigDecimal.ZERO);
            userCreditRepository.save(userCreditToPersist);
        }
    }

    @StreamListener(target = MQBinding.USER_ORDER)
    public void processUserOrder(OrderDTO msg) {
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
        orderRepository.save(orderToPersist);
    }

    @Transactional
    @StreamListener(target = MQBinding.USER_CANCELLATION)
    public void processUserCancellation(UserDTO msg) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(msg.getId());
        if(userCredit.isPresent()){
            userCredit.get().setCredit(BigDecimal.ZERO);
            rechargeUserCreditLogRepository.deleteAllByUserCredit(userCredit.get());
            userCreditRepository.save(userCredit.get());
        }
    }

    @StreamListener(target = MQBinding.ORDER_CANCELLATION)
    public void processOrderCancellation(OrderDTO msg) {
        Optional<Order> order = orderRepository.findById(msg.getOrderId());
        if(order.isPresent()){
            Optional<Payment> payment = paymentRepository.findByOrderId(msg.getOrderId());
            if(payment.isPresent()){
                BigDecimal actualCredit = order.get().getUserCredit().getCredit();
                BigDecimal newCredit = actualCredit.add(order.get().getTotalToPay());
                internalPaymentService.userCreditUpdateCredit(msg.getUser(),newCredit, RechargeUserCreditType.ORDER_CANCELLED);
                paymentRepository.deleteById(payment.get().getPaymentId());
            }
            orderRepository.deleteById(msg.getOrderId());
        }
    }
}
