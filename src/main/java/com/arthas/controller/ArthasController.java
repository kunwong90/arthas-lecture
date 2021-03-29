package com.arthas.controller;

import com.alibaba.fastjson.JSONObject;
import com.arthas.service.ArthasService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class ArthasController {


    @Resource
    private ArthasService arthasService;

    @GetMapping(value = "/test/{sleep}")
    public String test(@PathVariable("sleep") int sleep) {
        arthasService.test(sleep);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("success", true);
        return jsonObject.toJSONString();
    }
}
