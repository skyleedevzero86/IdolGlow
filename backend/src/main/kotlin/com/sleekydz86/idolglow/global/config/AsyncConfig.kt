package com.sleekydz86.idolglow.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {
    @Bean(name = ["imageStorageExecutor"])
    fun imageStorageExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 4
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("image-storage-")
        executor.initialize()
        return executor
    }

    @Bean(name = ["subscriptionDispatchExecutor"])
    fun subscriptionDispatchExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 4
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("subscription-dispatch-")
        executor.initialize()
        return executor
    }

    @Bean(name = ["surveyLlmExecutor"])
    fun surveyLlmExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 1
        executor.maxPoolSize = 1
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("survey-llm-")
        executor.initialize()
        return executor
    }
}
