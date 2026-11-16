package pe.vallegrande.bullying.cases;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import static pe.vallegrande.bullying.cases.CaseModels.*;

@RestController
public class CaseController {
    private final CaseService service;
    private final CaseRepository repository;
    public CaseController(CaseService service,CaseRepository repository) { this.service=service; this.repository=repository; }
    @Operation(summary = "Registrar un caso de bullying",
        description = "Registra el incidente, asigna un responsable e incluye al menos un estudiante afectado. El caso comienza como Reportado.",
        tags = "Casos")
    @PostMapping("/api/cases")
    public ResponseEntity<Detail> create(@RequestBody CreateCase input,Principal actor) {
        Detail detail=service.create(input,actor.getName());
        return ResponseEntity.created(URI.create("/api/cases/"+detail.caso().id())).body(detail);
    }
    @Operation(summary = "Consultar el detalle de un caso",
        description = "Indica el ID numérico del caso para ver el incidente, el responsable y los estudiantes involucrados.",
        tags = "Casos")
    @GetMapping("/api/cases/{id}")
    public Detail detail(@PathVariable long id,Principal actor) { return service.detail(id,actor.getName()); }
    @Operation(summary = "Buscar casos con filtros",
        description = "Lista casos visibles para la cuenta actual. Puedes filtrar por búsqueda, estado, tipo y fechas.",
        tags = "Casos")
    @GetMapping("/api/cases")
    public Page<Summary> list(Principal actor,@RequestParam(defaultValue="") String q,
        @RequestParam(defaultValue="") String estado,@RequestParam(defaultValue="") String tipo,
        @RequestParam(required=false) LocalDate desde,@RequestParam(required=false) LocalDate hasta,
        @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit) {
        return service.list(actor.getName(),q,estado,tipo,desde,hasta,offset,limit);
    }
    @Operation(summary = "Consultar los catálogos de casos",
        description = "Devuelve los tipos de incidentes, estados y participaciones que admite el sistema.",
        tags = "Casos")
    @GetMapping("/api/cases/catalogs")
    public Catalogs catalogs() { return repository.catalogs(); }
    @Operation(summary = "Buscar responsables activos",
        description = "Lista las cuentas activas que se pueden elegir como responsables. Puedes filtrar por nombre.",
        tags = "Casos")
    @GetMapping("/api/cases/responsibles")
    public List<Responsible> responsibles(@RequestParam(defaultValue="") String q,
        @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit) {
        CaseService.page(offset,limit); return repository.responsibles(CaseService.search(q),offset,limit);
    }
    @Operation(summary = "Buscar estudiantes para vincular a un caso",
        description = "Devuelve estudiantes activos visibles para la cuenta actual. Usa el ID de la respuesta para vincular a uno.",
        tags = "Estudiantes")
    @GetMapping("/api/students")
    public Page<Student> students(Principal actor,@RequestParam(defaultValue="") String q,
        @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit) {
        return service.students(actor.getName(),q,offset,limit);
    }
}
