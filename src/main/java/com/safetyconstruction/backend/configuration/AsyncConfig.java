package com.safetyconstruction.backend.configuration;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // Số lượng luồng tối thiểu luôn sẵn sàng
        executor.setCorePoolSize(5);
        // Số lượng luồng tối đa khi hệ thống quá tải (như lúc test JMeter)
        executor.setMaxPoolSize(20);
        // Hàng đợi chứa các task chờ xử lý
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("SafetyNotify-");
        executor.initialize();
        return executor;
    }
}
