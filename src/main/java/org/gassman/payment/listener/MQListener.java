package org.gassman.payment.listener;

import org.gassman.payment.binding.MQBinding;
import org.gassman.payment.dto.UserDTO;
import org.gassman.payment.entity.UserCredit;
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

    @StreamListener(target = MQBinding.USER_REGISTRATION)
    public void processUserRegistration(UserDTO msg) {
        Optional<UserCredit> userCredit = userCreditRepository.findById(msg.getId());
        if (!userCredit.isPresent()) {
            UserCredit userCreditToPersist = new UserCredit(msg.getId(), msg.getName(), msg.getSurname(), msg.getMail(), msg.getTelegramUserId(), BigDecimal.ZERO);
            userCreditRepository.save(userCreditToPersist);
        }
    }
}
