package pe.vallegrande.bullying;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.WebApplicationType;
import java.util.Map;

@SpringBootApplication
public class BullyingApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BullyingApplication.class);
        boolean bootstrap = "true".equalsIgnoreCase(System.getenv("BOOTSTRAP_ADMIN"));
        if (bootstrap) {
            app.setWebApplicationType(WebApplicationType.NONE);
            app.setDefaultProperties(Map.of("app.bootstrap", "true"));
        }
        var context = app.run(args);
        if (bootstrap) context.close();
    }
}
