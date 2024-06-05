package com.chenxin.smartbibackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chenxin.smartbibackend.model.entity.Chart;

import java.util.List;
import java.util.Map;


/**
 * @author fangchenxin
 * @description 针对表【chart(图表信息表)】的数据库操作Mapper
 * @createDate 2024-06-03 12:16:24
 * @Entity
 */
public interface ChartMapper extends BaseMapper<Chart> {

    List<Map<String, Object>> queryChartData(String querySql);

}




