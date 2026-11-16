package pe.vallegrande.bullying.users;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.security.Principal;
import java.util.List;

@Tag(name = "Usuarios")
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final AccountRepository repository;
    private final AccountService service;
    public UserController(AccountRepository repository, AccountService service) {
        this.repository = repository; this.service = service;
    }
    @Operation(summary = "Listar cuentas de usuario",
        description = "Devuelve las cuentas del sistema por páginas. Requiere una cuenta administradora.")
    @GetMapping
    public List<Account.UserView> list(@RequestParam(defaultValue="0") int offset,
                                      @RequestParam(defaultValue="50") int limit) {
        if (offset < 0 || limit < 1 || limit > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginación no válida (limit: 1 a 100).");
        return repository.list(offset, limit);
    }
    @Operation(summary = "Crear una cuenta de usuario",
        description = "Registra una cuenta con correo, rol y contraseña inicial.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Account.UserView create(@RequestBody AccountService.CreateUser input) { return service.create(input); }
    @Operation(summary = "Editar los datos de un usuario",
        description = "Usa el ID del usuario para actualizar sus nombres, apellidos, correo o rol. La contraseña se conserva.")
    @PutMapping("/{id}")
    public Account.UserView edit(@PathVariable long id,@RequestBody AccountService.EditUser input,Principal actor){return service.edit(id,input,actor.getName());}

    public record StatusInput(Boolean activo) {}
    @Operation(summary = "Activar o desactivar una cuenta",
        description = "Envía activo: true para habilitar el acceso o activo: false para quitarlo. Se conserva el historial.")
    @PatchMapping("/{id}/status")
    public Account.UserView status(@PathVariable long id, @RequestBody StatusInput input, Principal actor) {
        if (input == null || input.activo() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica activo: true o false.");
        return service.setActive(id, input.activo(), actor.getName());
    }
}
