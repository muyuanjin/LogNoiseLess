package com.example.lognoiselessdemo;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class LogNoiseLessDemoApplicationTests {

    @Test
    void contextLoads() {
        for (int i = 0; i < 5; i++) {
            log.warn("test", new RuntimeException("test"));
        }
    }
}
