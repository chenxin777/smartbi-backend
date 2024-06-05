package com.chenxin.smartbibackend.mapper;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@SpringBootTest
class ChartMapperTest {

    @Resource
    private ChartMapper chartMapper;

    @Test
    void queryChartData() {
        String chartId = "156678899000";
        String querySql = String.format("select * from chart_%s", chartId);
        List<Map<String, Object>> map = chartMapper.queryChartData(querySql);
        System.out.println(map);

    }
}