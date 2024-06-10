package com.chenxin.smartbibackend.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

/**
 * @author fangchenxin
 * @description
 * @date 2024/6/8 13:19
 * @modify
 */
public class DirectConsumer {

    private static final String EXCHANGE_NAME = "direct_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("admin");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        channel.exchangeDeclare(EXCHANGE_NAME, "direct");

        String cQueueName = "C";
        channel.queueDeclare(cQueueName, true, false, false, null);
        channel.queueBind(cQueueName, EXCHANGE_NAME, "c");

        String dQueueName = "D";
        channel.queueDeclare(dQueueName, true, false, false, null);
        channel.queueBind(dQueueName, EXCHANGE_NAME, "d");


        DeliverCallback deliverCallback3 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [C] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        DeliverCallback deliverCallback4 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [D] Received '" +
                    delivery.getEnvelope().getRoutingKey() + "':'" + message + "'");
        };

        channel.basicConsume(cQueueName, true, deliverCallback3, consumerTag -> {
        });
        channel.basicConsume(dQueueName, true, deliverCallback4, consumerTag -> {
        });
    }

}
