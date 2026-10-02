package com.share.rental.item.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.share.rental.item.mapper")
public class ItemMybatisConfig {
}
