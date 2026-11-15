package pe.vallegrande.bullying.cases;

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
    @PostMapping("/api/cases")
    public ResponseEntity<Detail> create(@RequestBody CreateCase input,Principal actor) {
        Detail detail=service.create(input,actor.getName());
        return ResponseEntity.created(URI.create("/api/cases/"+detail.caso().id())).body(detail);
    }
    @GetMapping("/api/cases/{id}")
    public Detail detail(@PathVariable long id,Principal actor) { return service.detail(id,actor.getName()); }
    @GetMapping("/api/cases")
    public Page<Summary> list(Principal actor,@RequestParam(defaultValue="") String q,
        @RequestParam(defaultValue="") String estado,@RequestParam(defaultValue="") String tipo,
        @RequestParam(required=false) LocalDate desde,@RequestParam(required=false) LocalDate hasta,
        @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit) {
        return service.list(actor.getName(),q,estado,tipo,desde,hasta,offset,limit);
    }
    @GetMapping("/api/cases/catalogs")
    public Catalogs catalogs() { return repository.catalogs(); }
    @GetMapping("/api/cases/responsibles")
    public List<Responsible> responsibles(@RequestParam(defaultValue="") String q,
        @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit) {
        CaseService.page(offset,limit); return repository.responsibles(CaseService.search(q),offset,limit);
    }
    @GetMapping("/api/students")
    public Page<Student> students(Principal actor,@RequestParam(defaultValue="") String q,
        @RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit) {
        return service.students(actor.getName(),q,offset,limit);
    }
}
