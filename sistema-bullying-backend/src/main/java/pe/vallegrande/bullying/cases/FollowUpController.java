package pe.vallegrande.bullying.cases;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
@Tag(name = "Seguimientos")
@RestController
@RequestMapping("/api/cases/{id}/followups")
public class FollowUpController {
 private final FollowUpService service;
 public FollowUpController(FollowUpService service){this.service=service;}
 @Operation(summary = "Consultar el historial de seguimiento",
     description = "Muestra las acciones del caso, su estado, su próxima fecha de seguimiento y su versión.")
 @GetMapping public FollowUpService.Timeline get(@PathVariable long id,Principal p){return service.timeline(id,p.getName());}
 @Operation(summary = "Registrar una acción de seguimiento",
     description = "Envía una acción con su fecha y la versión vigente. Puedes mantener el estado o pasar al siguiente. Para cerrar el caso, completa sus acuerdos pendientes.")
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public FollowUpService.Timeline add(@PathVariable long id,@RequestBody FollowUpService.Input input,Principal p){return service.add(id,input,p.getName());}
}
