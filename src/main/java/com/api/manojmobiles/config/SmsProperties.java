package com.api.manojmobiles.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "sms.td-digital")
@Getter
@Setter
public class SmsProperties {
    private String baseUrl;
    private String authKey;
    private String senderId;
    private String domesticRoute;
}
