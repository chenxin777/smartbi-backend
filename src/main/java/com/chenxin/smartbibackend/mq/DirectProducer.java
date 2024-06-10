package com.chenxin.smartbibackend.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.util.Scanner;

/**
 * @author fangchenxin
 * @description
 * @date 2024/6/8 15:07
 * @modify
 */
public class DirectProducer {
    private static final String EXCHANGE_NAME = "direct_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("admin");
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            // 指定交换机
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String userInput = scanner.nextLine();
                String[] split = userInput.split(" ");
                if (split.length < 1) {
                    continue;
                }
                String routingKey = split[0];
                String message = split[1];
                channel.basicPublish(EXCHANGE_NAME, routingKey, null,
                        message.getBytes("UTF-8"));
                System.out.println("To routing: " + routingKey + ", send: " + message);
            }
            
        }
    }
}
