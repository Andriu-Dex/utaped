package ec.edu.uta.utaped.identity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class RecoveryDeliveryConfig {
    @Bean(name="recoveryExecutor") ThreadPoolTaskExecutor recoveryExecutor() {
        var executor=new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);executor.setMaxPoolSize(2);executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("recovery-");executor.setWaitForTasksToCompleteOnShutdown(true);executor.setAwaitTerminationSeconds(15);
        return executor;
    }
}
