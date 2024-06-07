package com.chenxin.smartbibackend.utils;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import com.github.jsonzou.jmockdata.JMockData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author fangchenxin
 * @description Excel相关工具
 * @date 2024/6/3 21:53
 * @modify
 */
@Slf4j
public class ExcelUtils {

    /**
     * @param multipartFile
     * @return java.lang.String
     * @description excel转csv
     * @author fangchenxin
     * @date 2024/6/6 16:34
     */
    public static String excelToCsv(MultipartFile multipartFile) {
        // 读取数据
        List<Map<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream()).excelType(ExcelTypeEnum.XLSX).sheet().headRowNumber(0).doReadSync();
        } catch (IOException e) {
            log.error("表格处理错误");
        }
        if (CollUtil.isEmpty(list)) {
            return "";
        }
        // 转换为CSV
        StringBuilder stringBuilder = new StringBuilder();
        // 读取表头
        LinkedHashMap<Integer, String> headerMap = (LinkedHashMap<Integer, String>) list.get(0);
        List<String> headerList = headerMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
        stringBuilder.append(StringUtils.join(headerList, ",")).append("\n");
        for (int i = 1; i < list.size(); i++) {
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap<Integer, String>) list.get(i);
            List<String> dataList = dataMap.values().stream().filter(ObjectUtils::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StringUtils.join(dataList, ",")).append("\n");
        }
        return stringBuilder.toString();
    }


    /**
     * @param headList
     * @param classList
     * @param rowNum
     * @description 模拟excel表
     * @author fangchenxin
     * @date 2024/6/6 16:33
     */
    public static void createExcel(List<String> headList, List<Class<?>> classList, int rowNum) {
        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("sheet1");
        // 设置表头
        Row row = sheet.createRow(0);
        for (int i = 0; i < headList.size(); i++) {
            Cell tmpCell = row.createCell(i);
            tmpCell.setCellValue(headList.get(i));
        }
        for (int i = 1; i <= rowNum; i++) {
            Row dataRow = sheet.createRow(i);
            for (int j = 0; j < headList.size(); j++) {
                Cell tmpCell = dataRow.createCell(j);
                Class<?> aClass = classList.get(j);
                tmpCell.setCellValue(mockValue(aClass).toString());
            }
        }

        // 保存Excel文件
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream("src/main/resources/excel/" + "testExcel_" + RandomUtil.randomNumbers(5) + ".xlsx");
            workbook.write(fileOutputStream);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * @param clazz
     * @return java.lang.Object
     * @description 模拟数据
     * @author fangchenxin
     * @date 2024/6/6 16:33
     */
    public static Object mockValue(Class<?> clazz) {
        final List<Class<?>> valueTypeList = Arrays.asList(int.class, double.class, Date.class, String.class);
        if (valueTypeList.contains(clazz)) {
            if (clazz == int.class) {
                return JMockData.mock(int.class);
            }
            if (clazz == double.class) {
                return JMockData.mock(double.class);
            }
            if (clazz == Date.class) {
                Date date = JMockData.mock(Date.class);
                return DateUtil.format(date, "yyyy-MM-dd");
            }
            if (clazz == String.class) {
                return JMockData.mock(String.class);
            }
        }
        return "";
    }

    public static void main(String[] args) {
        List<String> headList = Arrays.asList("日期", "用户数", "日收入");
        List<Class<?>> classList = Arrays.asList(Date.class, int.class, double.class);
        int rowNum = 10;
        createExcel(headList, classList, rowNum);
    }
}
