package com.luma.boston.pdw.updates.service.config;

import com.luma.boston.pdw.updates.service.client.ProductDataWarehouseServiceClient;
import com.luma.security.feign.EnableFeignSecuritySupport;
import com.luma.security.server.EnableDefaultResourceServer;
import feign.Logger;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignSecuritySupport
@EnableDefaultResourceServer
@EnableFeignClients(basePackageClasses = ProductDataWarehouseServiceClient.class)
public class LumaServiceConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

}
