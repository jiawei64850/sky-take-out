package com.sky.task;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Slf4j
public class DemoTask {
    /**
     * 每隔5秒，触发日志打印
     */
    //@Scheduled(cron = "0/5 * * * * ?")
    public void printLog(){
        log.info("执行定时任务: {}", LocalDateTime.now().toString());
    }
}
