package pe.vallegrande.bullying.users;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Ejecutable local explícito: no existe endpoint público de registro de administradores. */
@Component
@ConditionalOnProperty(name="app.bootstrap", havingValue="true")
public class BootstrapAdmin implements CommandLineRunner {
    private final AccountService service;
    private final Environment environment;
    public BootstrapAdmin(AccountService service, Environment environment) {
        this.service = service; this.environment = environment;
    }
    @Override
    public void run(String... args) {
        service.bootstrap(new AccountService.CreateUser(environment.getRequiredProperty("ADMIN_NOMBRES"),
            environment.getRequiredProperty("ADMIN_APELLIDOS"), environment.getRequiredProperty("ADMIN_EMAIL"),
            environment.getRequiredProperty("ADMIN_PASSWORD"), "ADMINISTRADOR"));
        System.out.println("Administrador inicial creado. La contraseña no se muestra ni se guarda en archivos.");
    }
}
