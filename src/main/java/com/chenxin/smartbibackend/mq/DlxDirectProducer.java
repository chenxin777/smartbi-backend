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
public class DlxDirectProducer {
    private static final String DEAD_EXCHANGE_NAME = "dlx_direct_exchange";

    private static final String WORK_EXCHANGE_NAME = "direct2_exchange";

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setUsername("admin");
        factory.setPassword("admin");
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            // 声明死信交换机
            channel.exchangeDeclare(DEAD_EXCHANGE_NAME, "direct");

            // 创建队列
            String queueName1 = "boss_dlx_queue";
            channel.queueDeclare(queueName1, true, false, false, null);
            channel.queueBind(queueName1, DEAD_EXCHANGE_NAME, "boss");

            String queueName2 = "wb_dlx_queue";
            channel.queueDeclare(queueName2, true, false, false, null);
            channel.queueBind(queueName2, DEAD_EXCHANGE_NAME, "wb");

            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNext()) {
                String userInput = scanner.nextLine();
                String[] split = userInput.split(" ");
                if (split.length < 1) {
                    continue;
                }
                String routingKey = split[0];
                String message = split[1];
                channel.basicPublish(WORK_EXCHANGE_NAME, routingKey, null,
                        message.getBytes("UTF-8"));
                System.out.println("To routing: " + routingKey + ", send: " + message);
            }

        }
    }
}
