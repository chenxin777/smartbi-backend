package com.chenxin.smartbibackend.bizmq;

import com.chenxin.smartbibackend.common.ErrorCode;
import com.chenxin.smartbibackend.exception.BusinessException;
import com.chenxin.smartbibackend.manager.AiManager;
import com.chenxin.smartbibackend.model.entity.Chart;
import com.chenxin.smartbibackend.model.enums.ChartStatus;
import com.chenxin.smartbibackend.service.ChartService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author fangchenxin
 * @description
 * @date 2024/6/9 15:01
 * @modify
 */
@Component
@Slf4j
public class BiMessageConsumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AiManager aiManager;

    @SneakyThrows
    @RabbitListener(queues = BiMqConstant.BI_QUEUE_NAME, ackMode = "MANUAL")
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receive from {} : {}", BiMqConstant.BI_QUEUE_NAME, message);
        if (StringUtils.isBlank(message)) {
            // 拒绝
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }
        long chartId = Long.parseLong(message);
        Chart chart = chartService.getById(chartId);
        if (chart == null) {
            // 拒绝
            channel.basicNack(deliveryTag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图表为空");
        }
        // 修改图表状态
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(ChartStatus.RUNNING.getValue());
        boolean updateRes = chartService.updateById(updateChart);
        if (!updateRes) {
            chartService.handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
            return;
        }

        // 调用AI接口
        String userInput = buildUserInput(chart);
        String res;
        try {
            res = aiManager.doChat(BiMqConstant.biModelId, userInput);
        } catch (Exception ex) {
            chartService.handleChartUpdateError(chart.getId(), "AI生成错误");
            return;
        }
        if (res == null) {
            chartService.handleChartUpdateError(chart.getId(), "AI生成错误");
            return;
        }
        String[] splits = res.split("【【【【【");
        if (splits.length < 3) {
            chartService.handleChartUpdateError(chart.getId(), "AI生成错误");
            return;
        }
        String genChart = splits[1];
        String genResult = splits[2];
        Chart updateChartResult = new Chart();
        updateChartResult.setId(chart.getId());
        updateChartResult.setGenChart(genChart);
        updateChartResult.setGenResult(genResult);
        updateChartResult.setStatus(ChartStatus.SUCCEED.getValue());
        boolean save = chartService.updateById(updateChartResult);
        if (!save) {
            chartService.handleChartUpdateError(chart.getId(), "更新图表成功状态失败");
        }
        // 确认消息
        channel.basicAck(deliveryTag, false);
    }

    @SneakyThrows
    @RabbitListener(queues = BiMqConstant.DLX_BI_QUEUE_NAME, ackMode = "MANUAL")
    public void receiveDlxMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receive from {} : {}", BiMqConstant.DLX_BI_QUEUE_NAME, message);
        long chartId = Long.parseLong(message);
        chartService.handleChartUpdateError(chartId, "死信队列处理");
        channel.basicAck(deliveryTag, false);
    }

    /**
     * @param chart
     * @return java.lang.String
     * @description 构造用户输入
     * @author fangchenxin
     * @date 2024/6/9 16:50
     */
    private String buildUserInput(Chart chart) {
        // 用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");
        String userGoal = chart.getGoal();
        if (StringUtils.isNotBlank(chart.getChartType())) {
            userGoal += ", 请使用" + chart.getChartType();
        }
        userInput.append(userGoal).append("\n");
        userInput.append("原始数据: ").append(chart.getChartData()).append("\n");
        return userInput.toString();
    }
}
