package com.chenxin.smartbibackend.bizmq;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * @author fangchenxin
 * @description
 * @date 2024/6/9 15:01
 * @modify
 */
@Component
@Slf4j
public class MyMessageConsumer {

    @SneakyThrows
    @RabbitListener(queues = "code_queue", ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receiveMessage={}", message);
        channel.basicAck(deliveryTag, false);
    }
}
