package com.share.rental.wallet.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.share.rental.wallet.mapper")
public class WalletMybatisConfig {
}
