package com.chenxin.smartbibackend.manager;

import com.chenxin.smartbibackend.common.ErrorCode;
import com.chenxin.smartbibackend.exception.ThrowUtils;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author fangchenxin
 * @description
 * @date 2024/6/4 13:43
 * @modify
 */
@Service
public class AiManager {

    @Resource
    private YuCongMingClient yuCongMingClient;

    /**
     * @param modelId
     * @param message
     * @return java.lang.String
     * @description AI对话
     * @author fangchenxin
     * @date 2024/6/4 14:14
     */
    public String doChat(Long modelId, String message) {
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(modelId);
        devChatRequest.setMessage(message);
        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        ThrowUtils.throwIf(response == null || response.getData() == null, ErrorCode.SYSTEM_ERROR, "AI响应异常");
        return response.getData().getContent();
    }
}
