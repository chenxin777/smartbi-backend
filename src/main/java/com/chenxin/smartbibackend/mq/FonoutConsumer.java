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
public class FonoutConsumer {

    private static final String EXCHANGE_NAME = "fonout-exchange";

    public static void main(String[] argv) throws Exception {
        // 建立连接
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("admin");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        // 声明交换机
        channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

        // 创建A队列
        String aQueueName = "A";
        channel.queueDeclare(aQueueName, true, false, false, null);
        // 队列绑定交换机
        channel.queueBind(aQueueName, EXCHANGE_NAME, "");

        // 创建B队列
        String bQueueName = "B";
        channel.queueDeclare(bQueueName, true, false, false, null);
        // 队列绑定交换机
        channel.queueBind(bQueueName, EXCHANGE_NAME, "");


        DeliverCallback deliverCallback1 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [A] Received '" + message + "'");
        };

        DeliverCallback deliverCallback2 = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [B] Received '" + message + "'");
        };

        // 监听A、B两个队列
        channel.basicConsume(aQueueName, true, deliverCallback1, consumerTag -> {
        });
        channel.basicConsume(bQueueName, true, deliverCallback2, consumerTag -> {
        });
    }

}
