package pe.vallegrande.bullying.users;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

@Service
public class AccountService {
    private final AccountRepository repository;
    private final PasswordEncoder encoder;
    public static final Set<String> ROLES = Set.of("ADMINISTRADOR", "CONVIVENCIA", "DOCENTE_TUTOR");
    public record CreateUser(String nombres, String apellidos, String correo, String password, String rol) {}

    public AccountService(AccountRepository repository, PasswordEncoder encoder) {
        this.repository = repository; this.encoder = encoder;
    }
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
    private static String name(String value, String field) {
        if (value == null || value.isBlank() || value.strip().length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " es obligatorio (máximo 100 caracteres).");
        return value.strip();
    }
    public static void validatePassword(String password) {
        if (password == null || password.isBlank() || password.length() < 12
                || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 12 caracteres y como máximo 72 bytes UTF-8.");
    }
    @Transactional
    public Account.UserView create(CreateUser input) {
        if (input == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faltan los datos del usuario.");
        String nombres = name(input.nombres(), "Nombres"), apellidos = name(input.apellidos(), "Apellidos");
        String email = normalizeEmail(input.correo());
        if (email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Correo no válido.");
        if (input.rol() == null || !ROLES.contains(input.rol()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol no válido.");
        validatePassword(input.password());
        return repository.insert(nombres, apellidos, email, encoder.encode(input.password()), input.rol()).view();
    }
    @Transactional
    public Account.UserView setActive(long id, boolean active, String actor) {
        int admins = repository.lockAndCountAdmins();
        Account account = repository.byId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));
        if (!active && account.correo().equalsIgnoreCase(actor))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No puedes desactivar tu propia cuenta.");
        if (!active && account.activo() && account.rol().equals("ADMINISTRADOR") && admins <= 1)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Debe quedar un administrador activo.");
        repository.setActive(id, active);
        return repository.byId(id).orElseThrow().view();
    }
    @Transactional
    public Account.UserView bootstrap(CreateUser input) {
        if (repository.lockAndCountAdmins() > 0)
            throw new IllegalStateException("Ya existe un administrador activo. No se modificaron sus credenciales.");
        return create(new CreateUser(input.nombres(), input.apellidos(), input.correo(), input.password(), "ADMINISTRADOR"));
    }
}
