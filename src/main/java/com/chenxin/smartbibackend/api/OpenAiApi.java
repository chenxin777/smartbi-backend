package com.chenxin.smartbibackend.api;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * @author fangchenxin
 * @description
 * @date 2024/6/4 13:14
 * @modify
 */
public class OpenAiApi {


    public static void main(String[] args) {

        String url = "https://api.openai.com/v1/chat/completions";
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("model", "gpt-3.5-turbo");
        dataMap.put("message", "你好gpt，给我写首诗");
        String json = JSONUtil.toJsonStr(dataMap);
        HttpResponse res = HttpRequest.post(url)
                .header("Content-Type", "application/json")
                .header("Authorization", "")
                .body(json)
                .execute();
        System.out.println(res.body());

    }
}
