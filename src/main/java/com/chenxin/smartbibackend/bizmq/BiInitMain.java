package com.chenxin.smartbibackend.bizmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fangchenxin
 * @description
 * @date 2024/6/9 16:04
 * @modify
 */
@Slf4j
public class BiInitMain {

    public static void main(String[] args) {

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("127.0.0.1");
            factory.setPort(5672);
            factory.setUsername("admin");
            factory.setPassword("admin");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            // 定义业务交换机
            channel.exchangeDeclare(BiMqConstant.BI_EXCHANGE_NAME, "direct");
            // 定义死信交换机
            channel.exchangeDeclare(BiMqConstant.DLX_BI_EXCHANGE_NAME, "direct");
            // 定义死信队列
            channel.queueDeclare(BiMqConstant.DLX_BI_QUEUE_NAME, true, false, false, null);
            // 绑定死信交换机和队列
            channel.queueBind(BiMqConstant.DLX_BI_QUEUE_NAME, BiMqConstant.DLX_BI_EXCHANGE_NAME, BiMqConstant.DLX_BI_ROUTING_KEY);
            // 定义死信队列参数
            Map<String, Object> argsMap = new HashMap<>();
            // 要绑定哪个交换机
            argsMap.put("x-dead-letter-exchange", BiMqConstant.DLX_BI_EXCHANGE_NAME);
            // 指定死信转发到哪个队列
            argsMap.put("x-dead-letter-routing-key", BiMqConstant.DLX_BI_QUEUE_NAME);
            // 定义业务队列，绑定死信队列
            channel.queueDeclare(BiMqConstant.BI_QUEUE_NAME, true, false, false, argsMap);
            channel.queueBind(BiMqConstant.BI_QUEUE_NAME, BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY);
        } catch (Exception ex) {
            log.error("消息队列初始化失败");
        }

    }

}
