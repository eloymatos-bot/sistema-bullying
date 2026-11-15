package pe.vallegrande.bullying.students;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.vallegrande.bullying.cases.CaseModels.NewStudent;
import pe.vallegrande.bullying.cases.CaseModels.Page;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import static pe.vallegrande.bullying.students.StudentService.*;

@RestController
public class StudentController {
    private final StudentService service;
    public StudentController(StudentService service) { this.service=service; }

    /**
     * Lista todos los estudiantes del sistema con paginación y filtros opcionales.
     * Incluye activos e inactivos (campo {@code activo} en cada registro).
     * Los docentes solo ven estudiantes de sus casos asignados.
     */
    @GetMapping("/api/students/all")
    public List<StudentView> all() {
        return service.listAll();
    }

    @GetMapping("/api/students/directory")
    public Page<StudentView> directory(Principal actor,@RequestParam(defaultValue="") String q,
        @RequestParam(required=false) Integer grado,@RequestParam(defaultValue="") String seccion,
        @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit) {
        return service.directory(actor.getName(),q,grado,seccion,offset,limit);
    }
    @GetMapping("/api/students/{id}")
    public StudentView detail(@PathVariable long id,Principal actor) { return service.detail(id,actor.getName()); }
    @PostMapping("/api/students")
    public ResponseEntity<StudentView> create(@RequestBody NewStudent input,Principal actor) {
        var result=service.create(input,actor.getName());
        return ResponseEntity.created(URI.create("/api/students/"+result.id())).body(result);
    }
    @PutMapping("/api/students/{id}")
    public StudentView edit(@PathVariable long id,@RequestBody EditStudent input,Principal actor) {
        return service.edit(id,input,actor.getName());
    }
    @PatchMapping("/api/students/{id}/status")
    public StudentView status(@PathVariable long id,@RequestBody StudentStatus input,Principal actor) {
        return service.changeStatus(id,input,actor.getName());
    }
}
