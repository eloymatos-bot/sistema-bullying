package pe.vallegrande.bullying.students;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.vallegrande.bullying.cases.CaseModels.NewStudent;
import pe.vallegrande.bullying.cases.CaseModels.Page;
import java.net.URI;
import java.security.Principal;
import static pe.vallegrande.bullying.students.StudentService.*;

@Tag(name = "Estudiantes")
@RestController
public class StudentController {
    private final StudentService service;
    public StudentController(StudentService service) { this.service=service; }
    @Operation(summary = "Buscar en el directorio de estudiantes",
        description = "Busca por nombres o apellidos y permite filtrar por grado y sección. Incluye el estado y la versión de cada ficha.")
    @GetMapping("/api/students/directory")
    public Page<StudentView> directory(Principal actor,@RequestParam(defaultValue="") String q,
        @RequestParam(required=false) Integer grado,@RequestParam(defaultValue="") String seccion,
        @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit) {
        return service.directory(actor.getName(),q,grado,seccion,offset,limit);
    }
    @Operation(summary = "Consultar la ficha de un estudiante",
        description = "Indica el ID que aparece en el directorio. La respuesta incluye la versión vigente para editar.")
    @GetMapping("/api/students/{id}")
    public StudentView detail(@PathVariable long id,Principal actor) { return service.detail(id,actor.getName()); }
    @Operation(summary = "Registrar un estudiante",
        description = "Crea una ficha con nombres, apellidos, grado de 1 a 5 y sección.")
    @PostMapping("/api/students")
    public ResponseEntity<StudentView> create(@RequestBody NewStudent input,Principal actor) {
        var result=service.create(input,actor.getName());
        return ResponseEntity.created(URI.create("/api/students/"+result.id())).body(result);
    }
    @Operation(summary = "Editar la ficha de un estudiante",
        description = "Indica el ID y envía los datos junto con la versión devuelta por la consulta de la ficha.")
    @PutMapping("/api/students/{id}")
    public StudentView edit(@PathVariable long id,@RequestBody EditStudent input,Principal actor) {
        return service.edit(id,input,actor.getName());
    }
    @Operation(summary = "Activar o desactivar un estudiante",
        description = "Envía el estado activo y la versión vigente. Desactivar un estudiante conserva sus casos anteriores.")
    @PatchMapping("/api/students/{id}/status")
    public StudentView status(@PathVariable long id,@RequestBody StudentStatus input,Principal actor) {
        return service.setStatus(id,input,actor.getName());
    }
}
