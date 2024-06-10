package com.chenxin.smartbibackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chenxin.smartbibackend.mapper.ChartMapper;
import com.chenxin.smartbibackend.model.entity.Chart;
import com.chenxin.smartbibackend.model.enums.ChartStatus;
import com.chenxin.smartbibackend.service.ChartService;
import org.springframework.stereotype.Service;

/**
 * @author fangchenxin
 * @description 针对表【chart(图表信息表)】的数据库操作Service实现
 * @createDate 2024-06-03 12:16:24
 */
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Override
    public void handleChartUpdateError(long chartId, String execMessage) {
        Chart chart = new Chart();
        chart.setId(chartId);
        chart.setStatus(ChartStatus.FAILED.getValue());
        chart.setExecMessage(execMessage);
        boolean res = this.updateById(chart);
        if (!res) {
            log.error("更新图表失败状态失败" + chartId + "," + execMessage);
        }
    }

}




