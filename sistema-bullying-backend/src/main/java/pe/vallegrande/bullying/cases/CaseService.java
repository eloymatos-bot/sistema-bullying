package pe.vallegrande.bullying.cases;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.vallegrande.bullying.users.Account;
import pe.vallegrande.bullying.users.AccountRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static pe.vallegrande.bullying.cases.CaseModels.*;

@Service
public class CaseService {
    private final CaseRepository repository;
    private final AccountRepository accounts;
    private static final Set<String> PARTICIPATIONS=Set.of("AFECTADO","PRESUNTO_AGRESOR","TESTIGO");
    public CaseService(CaseRepository repository,AccountRepository accounts) { this.repository=repository; this.accounts=accounts; }
    private static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private Account actor(String email) {
        return accounts.byEmail(email).filter(Account::activo)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"La sesión ya no es válida."));
    }
    static String required(String value,int max,String label) {
        if (value==null || value.isBlank() || value.strip().length()>max) throw bad(label+" es obligatorio (máximo "+max+" caracteres).");
        return value.strip();
    }
    public static String search(String q) {
        if (q==null) return "";
        if (q.strip().length()>100) throw bad("La búsqueda admite hasta 100 caracteres.");
        return q.strip();
    }
    public static void page(int offset,int limit) {
        if (offset<0 || limit<1 || limit>100) throw bad("Paginación no válida (limit: 1 a 100).");
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public Detail create(CreateCase input,String email) {
        Account actor=actor(email);
        if (input==null) throw bad("Faltan los datos del caso.");
        LocalDate today=LocalDate.now(ZoneId.of("America/Lima"));
        if (input.fechaIncidente()==null || input.fechaIncidente().getYear()<1 || input.fechaIncidente().isAfter(today))
            throw bad("La fecha del incidente es obligatoria y no puede estar en el futuro.");
        String lugar=required(input.lugar(),200,"Lugar"), descripcion=required(input.descripcion(),4000,"Descripción");
        String tipo=required(input.tipoCodigo(),30,"Tipo de incidente");
        if (!repository.typeExists(tipo)) throw bad("Tipo de incidente no válido.");
        String observations=input.observaciones()==null ? null : input.observaciones().strip();
        if (observations!=null && observations.length()>2000) throw bad("Las observaciones admiten hasta 2000 caracteres.");
        if (input.responsableId()==null || input.responsableId()<1
            || accounts.byId(input.responsableId()).filter(Account::activo).isEmpty())
            throw bad("Selecciona un responsable activo.");
        if (input.estudiantes()==null || input.estudiantes().isEmpty() || input.estudiantes().size()>50)
            throw bad("Incluye de 1 a 50 participaciones de estudiantes.");
        List<InvolvedInput> normalized=new ArrayList<>();
        Set<String> seen=new HashSet<>(); boolean affected=false;
        for (InvolvedInput person:input.estudiantes()) {
            if (person==null || person.participacion()==null || !PARTICIPATIONS.contains(person.participacion()))
                throw bad("Participación de estudiante no válida.");
            affected |= person.participacion().equals("AFECTADO");
            if ((person.estudianteId()==null)==(person.nuevoEstudiante()==null))
                throw bad("Indica un estudiante existente o uno nuevo, no ambos.");
            NewStudent student=null;
            if (person.estudianteId()!=null) {
                if (person.estudianteId()<1 || !repository.studentAvailable(person.estudianteId(),actor))
                    throw bad("El estudiante seleccionado no está disponible.");
                if (!seen.add(person.estudianteId()+":"+person.participacion())) throw bad("Participación de estudiante duplicada.");
            } else {
                NewStudent raw=person.nuevoEstudiante();
                if (raw.grado()==null || raw.grado()<1 || raw.grado()>5) throw bad("El grado debe estar entre 1 y 5.");
                student=new NewStudent(required(raw.nombres(),100,"Nombres"),required(raw.apellidos(),100,"Apellidos"),
                    raw.grado(),required(raw.seccion(),10,"Sección"));
            }
            normalized.add(new InvolvedInput(person.estudianteId(),student,person.participacion()));
        }
        if (!affected) throw bad("El caso debe incluir al menos un estudiante afectado.");
        CreateCase clean=new CreateCase(input.fechaIncidente(),lugar,tipo,descripcion,observations,input.responsableId(),normalized);
        long id=repository.insertCase(clean,actor.id());
        for (InvolvedInput person:normalized) {
            long studentId=person.estudianteId()!=null ? person.estudianteId() : repository.insertStudent(person.nuevoEstudiante());
            repository.attach(id,studentId,person.participacion());
        }
        return repository.detail(id,actor).orElseThrow();
    }
    @Transactional(readOnly=true)
    public Detail detail(long id,String email) {
        return repository.detail(id,actor(email))
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Caso no encontrado."));
    }
    @Transactional(readOnly=true)
    public Page<Summary> list(String email,String q,String estado,String tipo,LocalDate desde,LocalDate hasta,int offset,int limit) {
        page(offset,limit); q=search(q);
        if ((desde!=null && (desde.getYear()<1 || desde.getYear()>9999))
            || (hasta!=null && (hasta.getYear()<1 || hasta.getYear()>9999))) throw bad("Fecha fuera de rango.");
        if (!estado.isEmpty() && !repository.stateExists(estado)) throw bad("Estado no válido.");
        if (!tipo.isEmpty() && !repository.typeExists(tipo)) throw bad("Tipo de incidente no válido.");
        if (desde!=null && hasta!=null && desde.isAfter(hasta)) throw bad("El rango de fechas no es válido.");
        return repository.list(actor(email),q,estado,tipo,desde,hasta,offset,limit);
    }
    @Transactional(readOnly=true)
    public Page<Student> students(String email,String q,int offset,int limit) {
        page(offset,limit); return repository.students(actor(email),search(q),offset,limit);
    }
}
