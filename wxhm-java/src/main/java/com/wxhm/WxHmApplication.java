package com.wxhm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class WxHmApplication {

    public static void main(String[] args) {
        SpringApplication.run(WxHmApplication.class, args);
    }
}
