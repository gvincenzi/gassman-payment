package org.gassman.payment.listener;

import org.gassman.payment.binding.MQBinding;
import org.gassman.payment.dto.OrderDTO;
import org.gassman.payment.dto.UserDTO;
import org.gassman.payment.entity.Order;
import org.gassman.payment.entity.UserCredit;
import org.gassman.payment.repository.OrderRepository;
import org.gassman.payment.repository.UserCreditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;

import java.math.BigDecimal;
import java.util.Optional;

@EnableBinding(MQBinding.class)
public class MQListener {
    @Autowired
    private UserCreditRepository userCreditRepository;

    @Autowired
    private OrderRepository orderRepository;

    @StreamListener(target = MQBinding.USER_REGISTRATION)
    public void processUserRegistration(UserDTO msg) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(msg.getId());
        if (!userCredit.isPresent()) {
            UserCredit userCreditToPersist = new UserCredit(msg.getId(), msg.getName(), msg.getSurname(), msg.getMail(), msg.getTelegramUserId(), BigDecimal.ZERO);
            userCreditRepository.save(userCreditToPersist);
        }
    }

    @StreamListener(target = MQBinding.USER_ORDER)
    public void processUserOrderChannel(OrderDTO msg) {
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
}
