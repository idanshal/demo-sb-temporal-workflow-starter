package com.idanshal.demos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;


@SpringBootApplication
public class DemoWorkflowStarterApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoWorkflowStarterApplication.class, args).start();
    }
}
