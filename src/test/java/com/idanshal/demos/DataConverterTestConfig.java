//package com.temporal.demos.temporalspringbootdemo;
//
//import io.temporal.common.converter.DataConverter;
//import io.temporal.common.converter.DefaultDataConverter;
//import org.springframework.boot.test.context.TestConfiguration;
//import org.springframework.context.annotation.Bean;
//
//@TestConfiguration
//public class DataConverterTestConfig {
//    @Bean
//    public DataConverter appDataConverter() {
//        return DefaultDataConverter.newDefaultInstance()
//                .withPayloadConverterOverrides(new CloudEventsPayloadConverter());
//    }
//}
