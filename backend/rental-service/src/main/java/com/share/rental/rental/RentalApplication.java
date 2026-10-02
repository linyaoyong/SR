package com.share.rental.rental;

import com.share.rental.common.feign.FeignConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.share.rental.rental.client", defaultConfiguration = FeignConfig.class)
@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.share.rental.rental", "com.share.rental.common"})
public class RentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(RentalApplication.class, args);
    }
}
