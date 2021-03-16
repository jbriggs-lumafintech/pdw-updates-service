package com.luma.boston.service.config;

import com.luma.security.feign.ribbon.LocalhostServersRibbonConfig;
import org.springframework.cloud.netflix.ribbon.RibbonClients;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@RibbonClients(
        defaultConfiguration = LocalhostServersRibbonConfig.class)
@Profile("ide")
public class LocalhostConfig {
}
