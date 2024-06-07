package com.chenxin.smartbibackend.model.enums;

/**
 * @author fangchenxin
 * @description
 * @date 2024/6/7 12:05
 * @modify
 */
public enum ChartStatus {
    WAIT("排队中", "wait"),
    RUNNING("分析中", "running"),
    SUCCEED("已完成", "succeed"),
    FAILED("失败", "failed");

    final String text;
    final String value;

    ChartStatus(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public String getValue() {
        return value;
    }
}
