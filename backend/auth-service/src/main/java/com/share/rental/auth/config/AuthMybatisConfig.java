package com.share.rental.auth.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.share.rental.auth.mapper")
public class AuthMybatisConfig {
}
