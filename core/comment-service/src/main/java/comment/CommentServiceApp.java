package comment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"comment", "util"}) // для логирования через AOP
@EnableFeignClients
public class CommentServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(CommentServiceApp.class, args);
    }
}
