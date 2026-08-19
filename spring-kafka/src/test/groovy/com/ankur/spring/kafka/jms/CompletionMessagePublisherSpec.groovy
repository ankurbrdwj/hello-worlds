package com.ankur.spring.kafka.jms

import org.apache.kafka.common.protocol.Message
import org.mockito.Mockito
import spock.lang.Specification

class CompletionMessagePublisherSpec extends Specification {

    def publisher = new CompletionMessagePublisher()

    def "publish returns whatever the underlying JMS publish call returns"() {
        given:
        def notification = new NotificationDto()

        when:
        def result = publisher.publish(notification, "dataSet", "topic")

        then:
        result == null
    }

    def "JmsProperties can be constructed with a broker name"() {
        given:
        def brokerName = "solace"

        when:
        def properties = new JmsProperties(brokerName)

        then:
        properties != null
    }

    def "RegularJmsFactory can be constructed with JmsProperties"() {
        given:
        def properties = new JmsProperties("solace")

        when:
        def factory = new RegularJmsFactory(properties)

        then:
        factory != null
        factory instanceof JmsFactory
    }

    def "publish delegates to a mocked JmsTopicPublisher instead of making a real JMS call"() {
        given:
        def notification = new NotificationDto()
        def expectedCreatedMessage = Mockito.mock(Message)
        def expectedPublishResult = Mockito.mock(Message)
        def mockedFactory = Mockito.mockConstruction(RegularJmsFactory)
        def mockedPublisher = Mockito.mockConstruction(JmsTopicPublisher) { mock, context ->
            Mockito.when(mock.createMessage(Mockito.any())).thenReturn(expectedCreatedMessage)
        }

        when:
        def result = publisher.publish(notification, "dataSet", "topic")

        then:
        mockedFactory.constructed().size() == 1
        mockedPublisher.constructed().size() == 1
        def mockPublisher = mockedPublisher.constructed()[0]
        def verifiedCreateMessage = Mockito.verify(mockPublisher).createMessage(notification.toString())
        def verifiedPublish = Mockito.verify(mockPublisher).publish(expectedCreatedMessage)
        result.is(expectedCreatedMessage)

        cleanup:
        mockedPublisher.close()
        mockedFactory.close()
    }
}