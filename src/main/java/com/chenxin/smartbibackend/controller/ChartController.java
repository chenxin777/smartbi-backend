package com.chenxin.smartbibackend.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chenxin.smartbibackend.annotation.AuthCheck;
import com.chenxin.smartbibackend.bizmq.BiMessageProducer;
import com.chenxin.smartbibackend.bizmq.BiMqConstant;
import com.chenxin.smartbibackend.common.BaseResponse;
import com.chenxin.smartbibackend.common.DeleteRequest;
import com.chenxin.smartbibackend.common.ErrorCode;
import com.chenxin.smartbibackend.common.ResultUtils;
import com.chenxin.smartbibackend.constant.CommonConstant;
import com.chenxin.smartbibackend.constant.UserConstant;
import com.chenxin.smartbibackend.exception.BusinessException;
import com.chenxin.smartbibackend.exception.ThrowUtils;
import com.chenxin.smartbibackend.manager.AiManager;
import com.chenxin.smartbibackend.manager.RedisLimiterManager;
import com.chenxin.smartbibackend.model.dto.chart.*;
import com.chenxin.smartbibackend.model.entity.Chart;
import com.chenxin.smartbibackend.model.entity.User;
import com.chenxin.smartbibackend.model.enums.ChartStatus;
import com.chenxin.smartbibackend.model.vo.BiResponse;
import com.chenxin.smartbibackend.service.ChartService;
import com.chenxin.smartbibackend.service.UserService;
import com.chenxin.smartbibackend.utils.ExcelUtils;
import com.chenxin.smartbibackend.utils.SqlUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 帖子接口
 *
 * @author <a href="https://github.com/lichenxin">程序员鱼皮</a>
 * @from <a href="https://chenxin.icu">编程导航知识星球</a>
 */
@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AiManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BiMessageProducer biMessageProducer;

    /**
     * 文件最大允许1MB
     */
    final long ONE_MB = 1024 * 1024L;

    /**
     * 允许上传的文件格式
     */
    final List<String> validFileSuffix = Arrays.asList("xlsx", "xls");

    private QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String chartName = chartQueryRequest.getChartName();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();
        String sortField = chartQueryRequest.getSortField();
        String sortOrder = chartQueryRequest.getSortOrder();

        queryWrapper.eq(id != null && id > 0, "id", id);
        queryWrapper.like(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotBlank(chartName), "chartName", chartName);
        queryWrapper.eq(StringUtils.isNotBlank(chartType), "chartType", chartType);
        queryWrapper.eq(userId != null && userId > 0, "userId", userId);
        queryWrapper.eq("isDelete", false);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC), sortField);
        return queryWrapper;
    }

    // region 增删改查

    /**
     * 创建
     *
     * @param chartAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }

    /**
     * 删除
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = userService.getLoginUser(request);
        long id = deleteRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可删除
        if (!oldChart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(id);
        return ResultUtils.success(b);
    }

    /**
     * 更新（仅管理员）
     *
     * @param chartUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateChart(@RequestBody ChartUpdateRequest chartUpdateRequest) {
        if (chartUpdateRequest == null || chartUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartUpdateRequest, chart);
        long id = chartUpdateRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * 根据 id 获取
     *
     * @param id
     * @return
     */
    @GetMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return ResultUtils.success(chart);
    }

    /**
     * 分页获取列表（仅管理员）
     *
     * @param chartQueryRequest
     * @return
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest) {
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        Page<Chart> chartPage = chartService.page(new Page<>(current, size), this.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    /**
     * 分页获取当前用户创建的资源列表
     *
     * @param chartQueryRequest
     * @param request
     * @return
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listMyChartByPage(@RequestBody ChartQueryRequest chartQueryRequest, HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        chartQueryRequest.setUserId(loginUser.getId());
        long current = chartQueryRequest.getCurrent();
        long size = chartQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        Page<Chart> chartPage = chartService.page(new Page<>(current, size), this.getQueryWrapper(chartQueryRequest));
        return ResultUtils.success(chartPage);
    }

    // endregion


    /**
     * 编辑（用户）
     *
     * @param chartEditRequest
     * @param request
     * @return
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editChart(@RequestBody ChartEditRequest chartEditRequest, HttpServletRequest request) {
        if (chartEditRequest == null || chartEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartEditRequest, chart);
        User loginUser = userService.getLoginUser(request);
        long id = chartEditRequest.getId();
        // 判断是否存在
        Chart oldChart = chartService.getById(id);
        ThrowUtils.throwIf(oldChart == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldChart.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean result = chartService.updateById(chart);
        return ResultUtils.success(result);
    }

    /**
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return com.chenxin.smartbibackend.common.BaseResponse<com.chenxin.smartbibackend.model.vo.BiResponse>
     * @description 智能分析（异步）
     * @author fangchenxin
     * @date 2024/6/7 13:57
     */
    @PostMapping("/upload/async")
    public BaseResponse<BiResponse> genChartByAiAsync(@RequestPart("file") MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        String chartName = genChartByAiRequest.getChartName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验请求参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(chartName) && chartName.length() > 100, ErrorCode.PARAMS_ERROR, "图表名称过长");
        ThrowUtils.throwIf(StringUtils.isBlank(chartName), ErrorCode.PARAMS_ERROR, "图表名称为空");
        // 校验文件
        String originalFilename = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize();
        // 获取后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        // 校验后缀
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型不支持");
        // 校验文件大小
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大于1MB");
        // 限流
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // TODO AI模型id
        // 用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ", 请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        // 压缩后的数据
        String data = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据: ").append(data).append("\n");

        // 存库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartName(chartName);
        chart.setChartData(data);
        chart.setChartType(chartType);
        // 设置状态
        chart.setStatus(ChartStatus.WAIT.getValue());
        chart.setUserId(loginUser.getId());
        boolean saveRes = chartService.save(chart);
        ThrowUtils.throwIf(!saveRes, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        // 将任务提交到线程池
        try {
            CompletableFuture.runAsync(() -> {
                // 修改图表状态
                Chart updateChart = new Chart();
                updateChart.setId(chart.getId());
                updateChart.setStatus(ChartStatus.RUNNING.getValue());
                boolean updateRes = chartService.updateById(updateChart);
                if (!updateRes) {
                    chartService.handleChartUpdateError(chart.getId(), "更新图表执行中状态失败");
                    return;
                }

                // 调用AI接口
                String res = aiManager.doChat(BiMqConstant.biModelId, userInput.toString());
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
            }, threadPoolExecutor).get(1, TimeUnit.MINUTES);
        } catch (Exception ex) {
            chartService.handleChartUpdateError(chart.getId(), "服务异常，请稍后重试");
        }

        // 封装返回
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        return ResultUtils.success(biResponse);

    }

    /**
     * @param multipartFile
     * @param genChartByAiRequest
     * @param request
     * @return com.chenxin.smartbibackend.common.BaseResponse<com.chenxin.smartbibackend.model.vo.BiResponse>
     * @description 智能分析（同步）
     * @author fangchenxin
     * @date 2024/6/7 13:56
     */
    @PostMapping("/upload")
    public BaseResponse<BiResponse> genChartByAi(@RequestPart("file") MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        String chartName = genChartByAiRequest.getChartName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(chartName) && chartName.length() > 100, ErrorCode.PARAMS_ERROR, "图表名称过长");
        ThrowUtils.throwIf(StringUtils.isBlank(chartName), ErrorCode.PARAMS_ERROR, "图表名称为空");

        // 校验文件
        String originalFilename = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024L;
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffix = Arrays.asList("png", "jpg", "svg", "webp", "jpeg", "xlsx", "xls");
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型不支持");
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大于1MB");
        // 限流
        redisLimiterManager.doRateLimit("genChartByAi_" + String.valueOf(loginUser.getId()));

//        final String prompt = "你是一名数据分析师和前端开发专家，接下来我会按照以下固定格式给你提供内容：\n" +
//                "分析需求：\n" +
//                "（数据分析的需求或者目标）\n" +
//                "原始数据：\n" +
//                "（csv格式的原始数据，用,作为分隔符）\n" +
//                "请根据这两部分内容，按照以下格式生成内容（此外不需要输出任何多余的开头、结尾、注释等内容）\n" +
//                "【【【【【\n" +
//                "{前端Echarts V5的option配置对象js代码，合理地将数据进行可视化，不要生成多余的注释}\n" +
//                "【【【【【\n" +
//                "{明确的数据分析结论、越详细越好，不要生成多余的注释}";
        long biModelId = 1759424033143119874L;
        // 用户输入
        StringBuilder userInput = new StringBuilder();
        userInput.append("分析需求: ").append("\n");
        String userGoal = goal;
        if (StringUtils.isNotBlank(chartType)) {
            userGoal += ", 请使用" + chartType;
        }
        userInput.append(userGoal).append("\n");
        // 压缩后的数据
        String data = ExcelUtils.excelToCsv(multipartFile);
        userInput.append("原始数据: ").append(data).append("\n");
        String res = aiManager.doChat(biModelId, userInput.toString());
        if (res == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误");
        }
        String[] splits = res.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI生成错误");
        }
        String genChart = splits[1];
        String genResult = splits[2];

        // 存库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartName(chartName);
        chart.setChartData(data);
        chart.setChartType(chartType);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(loginUser.getId());
        boolean saveRes = chartService.save(chart);
        ThrowUtils.throwIf(!saveRes, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        // 封装返回
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(chart.getId());
        biResponse.setGenChart(genChart);
        biResponse.setGenResult(genResult);
        return ResultUtils.success(biResponse);
    }

    @PostMapping("/upload/async/mq")
    public BaseResponse<BiResponse> genChartByAiAsyncMq(@RequestPart("file") MultipartFile multipartFile, GenChartByAiRequest genChartByAiRequest, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        String chartName = genChartByAiRequest.getChartName();
        String goal = genChartByAiRequest.getGoal();
        String chartType = genChartByAiRequest.getChartType();
        // 校验请求参数
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(chartName) && chartName.length() > 100, ErrorCode.PARAMS_ERROR, "图表名称过长");
        ThrowUtils.throwIf(StringUtils.isBlank(chartName), ErrorCode.PARAMS_ERROR, "图表名称为空");
        // 校验文件
        String originalFilename = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize();
        // 获取后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        // 校验后缀
        ThrowUtils.throwIf(!validFileSuffix.contains(suffix), ErrorCode.PARAMS_ERROR, "文件类型不支持");
        // 校验文件大小
        ThrowUtils.throwIf(fileSize > ONE_MB, ErrorCode.PARAMS_ERROR, "文件大于1MB");
        // 限流
        redisLimiterManager.doRateLimit("genChartByAi_" + loginUser.getId());

        // 压缩后的数据
        String data = ExcelUtils.excelToCsv(multipartFile);

        // 存库
        Chart chart = new Chart();
        chart.setGoal(goal);
        chart.setChartName(chartName);
        chart.setChartData(data);
        chart.setChartType(chartType);
        // 设置状态
        chart.setStatus(ChartStatus.WAIT.getValue());
        chart.setUserId(loginUser.getId());
        boolean saveRes = chartService.save(chart);
        ThrowUtils.throwIf(!saveRes, ErrorCode.SYSTEM_ERROR, "图表保存失败");
        // 发送到消息队列中
        Long newChartId = chart.getId();
        biMessageProducer.sendMessage(String.valueOf(newChartId));
        // 封装返回
        BiResponse biResponse = new BiResponse();
        biResponse.setChartId(newChartId);
        return ResultUtils.success(biResponse);
    }


}
