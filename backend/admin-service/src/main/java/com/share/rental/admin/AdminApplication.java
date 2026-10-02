package com.share.rental.admin;

import com.share.rental.common.feign.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.share.rental.admin.client", defaultConfiguration = FeignConfig.class)
@SpringBootApplication(scanBasePackages = {"com.share.rental.admin", "com.share.rental.common"})
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
