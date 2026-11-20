package sk.kasv.fx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/** Lets e-mails go out in the background (@Async) so requests do not wait for the mail server. */
@Configuration
@EnableAsync
public class AsyncConfig {}
