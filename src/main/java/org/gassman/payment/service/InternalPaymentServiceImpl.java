package org.gassman.payment.service;

import org.gassman.payment.dto.UserDTO;
import org.gassman.payment.entity.RechargeUserCreditLog;
import org.gassman.payment.entity.RechargeUserCreditType;
import org.gassman.payment.entity.UserCredit;
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
}
