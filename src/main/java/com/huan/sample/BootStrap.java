package com.huan.test.logback;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication()
@MapperScan(basePackages = {"com.huan.sample.repo.mapper"})
@ComponentScan(basePackages = {"com.huan.sample"})
public class BootStrap {

    public static void main(String[] args) {
        SpringApplication.run(BootStrap.class,args);
        System.out.println("服务已启动");
    }
}
