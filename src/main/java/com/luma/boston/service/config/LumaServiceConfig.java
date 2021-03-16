package com.luma.boston.service.config;

import com.luma.security.config.EnableApplicationSecurity;
import com.luma.security.server.EnableDefaultResourceServer;
import com.luma.spring.swagger.EnableApplicationSwagger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;

@Configuration
@EnableApplicationSecurity
@EnableDefaultResourceServer
@EnableApplicationSwagger
public class LumaServiceConfig {
    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties("security.oauth2.interservice")
    public ClientCredentialsResourceDetails interServiceResourceDetails() {
        return new ClientCredentialsResourceDetails();
    }
}
