package com.api.manojmobiles;

import com.api.manojmobiles.config.RedisProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RedisProperties.class)
public class ManojmobilesApplication {

	public static void main(String[] args) {
		SpringApplication.run(ManojmobilesApplication.class, args);
	}

}
