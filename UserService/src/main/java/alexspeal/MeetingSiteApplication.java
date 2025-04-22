package alexspeal;

import alexspeal.config.ApplicationConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ApplicationConfig.class)
public class MeetingSiteApplication {
    public static void main(String[] args) {
        SpringApplication.run(MeetingSiteApplication.class, args);
    }
}