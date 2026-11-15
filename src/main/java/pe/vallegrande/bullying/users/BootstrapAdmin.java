package pe.vallegrande.bullying.users;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Crea automáticamente un administrador inicial si no existe ninguno en la base.
 *
 * Se ejecuta en cada arranque del backend. Si ya existe un administrador activo,
 * no hace nada. Las credenciales se leen primero como variable de entorno del SO
 * y, si no están definidas, como propiedad de Spring (útil con application-local.properties).
 *
 *   ADMIN_INITIAL_EMAIL     (por defecto: admin@institucion.edu.pe)
 *   ADMIN_INITIAL_PASSWORD  (por defecto: Admin1234*Inicio)
 *   ADMIN_INITIAL_NOMBRES   (por defecto: Administrador)
 *   ADMIN_INITIAL_APELLIDOS (por defecto: Sistema)
 *
 * La contraseña nunca aparece en logs ni en respuestas de la API.
 */
@Component
public class BootstrapAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

    private final AccountService service;
    private final AccountRepository repository;
    private final Environment env;

    public BootstrapAdmin(AccountService service, AccountRepository repository, Environment env) {
        this.service    = service;
        this.repository = repository;
        this.env        = env;
    }

    @Override
    public void run(ApplicationArguments args) {
        int admins = repository.lockAndCountAdmins();
        if (admins > 0) {
            log.info("Administrador inicial: ya existe {} administrador(es) activo(s). No se creó ninguno nuevo.", admins);
            return;
        }

        String email     = prop("ADMIN_INITIAL_EMAIL",     "admin@institucion.edu.pe");
        String password  = prop("ADMIN_INITIAL_PASSWORD",  "Admin1234*Inicio");
        String nombres   = prop("ADMIN_INITIAL_NOMBRES",   "Administrador");
        String apellidos = prop("ADMIN_INITIAL_APELLIDOS", "Sistema");

        try {
            service.bootstrap(new AccountService.CreateUser(nombres, apellidos, email, password, "ADMINISTRADOR"));
            log.info("Administrador inicial creado. Correo: {}. Cambia la contraseña al primer acceso.", email);
        } catch (IllegalStateException e) {
            log.info("Administrador inicial: {}. No se creó ninguno nuevo.", e.getMessage());
        } catch (Exception e) {
            log.error("No se pudo crear el administrador inicial: {}. Verifica la base de datos.", e.getMessage());
        }
    }

    /**
     * Lee una propiedad buscando primero en variables de entorno del SO y luego
     * en el Environment de Spring (que incluye application-local.properties).
     */
    private String prop(String name, String defaultValue) {
        // Primero variable de entorno del SO
        String osValue = System.getenv(name);
        if (osValue != null && !osValue.isBlank()) return osValue.strip();
        // Luego propiedad Spring (cubre application-local.properties)
        String springValue = env.getProperty(name);
        if (springValue != null && !springValue.isBlank()) return springValue.strip();
        return defaultValue;
    }
}
