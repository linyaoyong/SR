package com.share.rental.admin.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.share.rental.admin.mapper")
public class AdminMybatisConfig {
}
