package com.ecommerce.gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/system")
@Tag(name = "System Discovery & Mesh API", description = "Safe cluster metadata and service discovery inspection for Control Center")
public class SystemController {

    private static final Logger log = LoggerFactory.getLogger(SystemController.class);

    private final DiscoveryClient discoveryClient;

    public SystemController(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @GetMapping("/services")
    @Operation(summary = "Get registered microservices from Service Registry", description = "Returns active microservice instances, status, host, port, and health safely without exposing Eureka admin controls.")
    public ResponseEntity<List<Map<String, Object>>> getRegisteredServices() {
        List<String> services = discoveryClient.getServices();
        List<Map<String, Object>> result = new ArrayList<>();

        if (services != null) {
            for (String serviceName : services) {
                List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
                int count = (instances != null) ? instances.size() : 0;
                String host = count > 0 ? instances.get(0).getHost() : "localhost";
                int port = count > 0 ? instances.get(0).getPort() : 0;
                String status = count > 0 ? "UP" : "DOWN";

                Map<String, Object> serviceMap = new LinkedHashMap<>();
                serviceMap.put("serviceName", serviceName.toUpperCase());
                serviceMap.put("status", status);
                serviceMap.put("host", host);
                serviceMap.put("port", port);
                serviceMap.put("instanceCount", count + (count == 1 ? " instance" : " instances"));
                
                List<Map<String, Object>> instanceList = new ArrayList<>();
                if (instances != null) {
                    for (ServiceInstance inst : instances) {
                        Map<String, Object> instMap = new LinkedHashMap<>();
                        instMap.put("instanceId", inst.getInstanceId() != null ? inst.getInstanceId() : inst.getHost() + ":" + inst.getPort());
                        instMap.put("host", inst.getHost());
                        instMap.put("port", inst.getPort());
                        instMap.put("uri", inst.getUri().toString());
                        instanceList.add(instMap);
                    }
                }
                serviceMap.put("instances", instanceList);
                result.add(serviceMap);
            }
        }

        return ResponseEntity.ok(result);
    }
}
