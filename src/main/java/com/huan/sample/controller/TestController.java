package com.huan.sample.controller;

import com.huan.test.logback.repo.entity.Customer;
import com.huan.test.logback.repo.mapper.CustomerMapper;
import jdk.nashorn.internal.objects.NativeJSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;


@Slf4j
@RestController
@RequestMapping("/test")
public class TestController {
    @Autowired
    private CustomerMapper customerMapper;

    @GetMapping(path = "/mybatis")
    public String studentScoreJobRun() {
        try {
            Customer customer = customerMapper.selectByPrimaryKey(256);
            System.out.println(customer.getMoney());
        } catch (Exception e) {
            log.error("DB ERROR:", e);
        }

//        try{
//            TimeUnit.SECONDS.sleep(20);
//        }catch (Exception e){
//
//        }
//        for (double i = 0; i < 1; i++) {
//            Customer customer = new Customer();
//            customer.setMoney(i);
//            customer.setName("test");
//            try {
//                customerMapper.insert(customer);
//            } catch (Exception e) {
//                log.error("DB ERROR:", e);
//            }
//        }
        return "OK";
    }
}
