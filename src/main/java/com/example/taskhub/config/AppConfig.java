package com.example.taskhub.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Feature switches. Think of these as the "AddXyz()" calls in Program.cs. */
@Configuration
@EnableJpaAuditing   // fills @CreatedDate / @LastModifiedDate
@EnableAsync         // honours @Async
@EnableScheduling    // honours @Scheduled
@EnableCaching       // honours @Cacheable / @CacheEvict
public class AppConfig {
}
