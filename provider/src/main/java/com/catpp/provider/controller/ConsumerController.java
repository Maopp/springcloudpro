package com.catpp.provider.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

/**
 * com.catpp.provider.controller
 *
 * @Author cat_pp
 * @Date 2018/12/10
 * @Description
 */
@RestController
@RequestMapping("/consumer")
@Slf4j
public class ConsumerController {

    /**
     * 注入服务客户端实例
     */
    @Autowired
    private DiscoveryClient discoveryClient;

    /**
     * 注入RestTemplate模板
     */
    @Autowired
    private RestTemplate restTemplate;

    /**
     * 服务消费者业务逻辑方法
     * 该方法使用restTemplate访问获取返回数据
     *
     * @return
     */
    @RequestMapping("/logic")
    public String home() {
        return "this is home page";
    }

    @RequestMapping("/index")
    public void index() {
        discoveryClient.getInstances("catpp-spring-cloud-eureka-provider")
                .stream()
                .forEach(
                        instance -> {
                            log.info("服务地址：{}，服务端口号：{}，服务实例编号：{}，服务地址：{}",
                                    instance.getHost(), instance.getPort(), instance.getServiceId(), instance.getUri());
                            String response = restTemplate.getForEntity("http://" + instance.getServiceId() + "/consumer/logic", String.class).getBody();
                            log.info("响应内容：{}", response);
                        });
    }
}
