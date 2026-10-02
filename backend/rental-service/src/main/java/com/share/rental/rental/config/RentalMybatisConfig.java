package com.share.rental.rental.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.share.rental.rental.mapper")
public class RentalMybatisConfig {
}
