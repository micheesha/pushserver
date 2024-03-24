package apps.netty.push;

import static org.springframework.boot.SpringApplication.run;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类
 *
 * @author mengxuan liang
 * @create 2018-10-14 13:54
 **/
@SpringBootApplication
@EnableScheduling
public class ServerApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext run = run(ServerApplication.class, args);
    }
}
