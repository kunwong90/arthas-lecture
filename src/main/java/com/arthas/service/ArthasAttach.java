package com.arthas.service;

import com.taobao.arthas.agent.attach.ArthasAgent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ArthasAttach implements InitializingBean {

    @Value("${arthas.tunnelServer}")
    private String arthasTunnelServer;

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, String> configMap = new HashMap<>();
        configMap.put("arthas.appName", "arthas-lecture");
        configMap.put("arthas.tunnelServer", arthasTunnelServer);
        configMap.put("arthas.agentId", "arthas-lecture");
        ArthasAgent.attach(configMap);
    }
}
