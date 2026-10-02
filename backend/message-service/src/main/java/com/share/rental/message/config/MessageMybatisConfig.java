package com.share.rental.message.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.share.rental.message.mapper")
public class MessageMybatisConfig {
}
