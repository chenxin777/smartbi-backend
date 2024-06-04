package com.chenxin.smartbibackend.model.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @author fangchenxin
 * @description
 * @date 2024/6/4 14:33
 * @modify
 */
@Data
public class BiResponse implements Serializable {

    private static final long serialVersionUID = 9036799525447216163L;

    private Long chartId;

    private String genChart;

    private String genResult;
}
