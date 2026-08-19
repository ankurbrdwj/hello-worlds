package com.ankur.spring.kafka.jms;

import org.apache.kafka.common.protocol.Message;

public class CompletionMessagePublisher {
    Message publish(NotificationDto notificationDto, String dataSet, String topic) {
        JmsProperties pros = new JmsProperties("solace");
        JmsFactory factory = new RegularJmsFactory(pros);
        JmsTopicPublisher solacePublisher = new JmsTopicPublisher(factory,topic);
        Message msg = solacePublisher.createMessage(notificationDto.toString());
         solacePublisher.publish(msg);
        return msg;
    }
}