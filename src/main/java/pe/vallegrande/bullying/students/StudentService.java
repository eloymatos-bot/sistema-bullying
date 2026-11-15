package pe.vallegrande.bullying.students;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.vallegrande.bullying.cases.CaseModels.NewStudent;
import pe.vallegrande.bullying.cases.CaseModels.Page;
import pe.vallegrande.bullying.cases.CaseService;
import pe.vallegrande.bullying.users.Account;
import pe.vallegrande.bullying.users.AccountRepository;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class StudentService {
    public record StudentView(long id, String nombres, String apellidos, int grado,
                              String seccion, boolean activo, String version) {}
    public record EditStudent(String nombres, String apellidos, Integer grado, String seccion, String version) {}
    public record StudentStatus(Boolean activo, String version) {}
    private final JdbcTemplate jdbc;
    private final AccountRepository accounts;
    private static final String FIELDS = "s.id,s.nombres,s.apellidos,s.grado,s.seccion,s.activo,s.version";
    private static final RowMapper<StudentView> MAPPER = (r,n) -> new StudentView(r.getLong("id"),
        r.getString("nombres"),r.getString("apellidos"),r.getInt("grado"),r.getString("seccion"),
        r.getBoolean("activo"),HexFormat.of().formatHex(r.getBytes("version")));

    public StudentService(JdbcTemplate jdbc, AccountRepository accounts) { this.jdbc=jdbc; this.accounts=accounts; }
    private static ResponseStatusException bad(String text) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,text); }
    private static String text(String value,int max,String label) {
        if(value==null || value.isBlank() || value.strip().length()>max) throw bad(label+" es obligatorio (máximo "+max+" caracteres).");
        if(value.codePoints().anyMatch(Character::isISOControl)) throw bad(label+" no admite caracteres de control.");
        return value.strip();
    }
    public static NewStudent clean(NewStudent input) {
        if(input==null) throw bad("Faltan los datos del estudiante.");
        if(input.grado()==null || input.grado()<1 || input.grado()>5) throw bad("El grado debe estar entre 1 y 5.");
        String section=text(input.seccion(),10,"Sección").toUpperCase(Locale.ROOT);
        if(section.length()>10) throw bad("La sección admite hasta 10 caracteres.");
        return new NewStudent(text(input.nombres(),100,"Nombres"),text(input.apellidos(),100,"Apellidos"),input.grado(),section);
    }
    private Account actor(String email) {
        return accounts.byEmail(email).filter(Account::activo)
            .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"La sesión ya no es válida."));
    }
    private Account editor(String email) {
        Account user=actor(email);
        if(!List.of("ADMINISTRADOR","CONVIVENCIA").contains(user.rol()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo administración y convivencia pueden administrar estudiantes.");
        return user;
    }
    private static String scope(Account actor,List<Object> args) {
        if(!actor.rol().equals("DOCENTE_TUTOR")) return "";
        args.add(actor.id());args.add(actor.id());
        return " AND s.activo=1 AND EXISTS (SELECT 1 FROM dbo.caso_estudiantes ce JOIN dbo.casos c ON c.id=ce.caso_id "
            +"WHERE ce.estudiante_id=s.id AND (c.registrado_por=? OR c.responsable_id=?))";
    }
    private static String like(String s) {
        return "%"+s.replace("\\","\\\\").replace("%","\\%").replace("_","\\_").replace("[","\\[")+"%";
    }
    @Transactional(readOnly=true)
    public List<StudentView> listAll() {
        return jdbc.query(
            "SELECT " + FIELDS + " FROM dbo.estudiantes s ORDER BY s.apellidos,s.nombres,s.id",
            MAPPER);
    }

    @Transactional(readOnly=true)
    public Page<StudentView> directory(String email,String q,Integer grado,String seccion,int offset,int limit) {
        Account user=actor(email); CaseService.page(offset,limit); q=CaseService.search(q);
        if(grado!=null && (grado<1 || grado>5)) throw bad("El grado debe estar entre 1 y 5.");
        seccion=seccion==null || seccion.isBlank()?"":text(seccion,10,"Sección").toUpperCase(Locale.ROOT);
        List<Object> args=new ArrayList<>();String where=" WHERE 1=1"+scope(user,args);
        if(!q.isEmpty()){where+=" AND (s.nombres+' '+s.apellidos) LIKE ? ESCAPE '\\'";args.add(like(q));}
        if(grado!=null){where+=" AND s.grado=?";args.add(grado);}
        if(!seccion.isEmpty()){where+=" AND UPPER(s.seccion)=?";args.add(seccion);}
        long total=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.estudiantes s"+where,Long.class,args.toArray());
        args.add(offset);args.add(limit);
        var rows=jdbc.query("SELECT "+FIELDS+" FROM dbo.estudiantes s"+where+
            " ORDER BY s.apellidos,s.nombres,s.id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",MAPPER,args.toArray());
        return new Page<>(rows,total,offset,limit);
    }
    private StudentView find(long id,Account user) {
        if(id<1) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Estudiante no encontrado.");
        List<Object> args=new ArrayList<>();args.add(id);
        var rows=jdbc.query("SELECT "+FIELDS+" FROM dbo.estudiantes s WHERE s.id=?"+scope(user,args),MAPPER,args.toArray());
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Estudiante no encontrado.");
        return rows.get(0);
    }
    @Transactional(readOnly=true)
    public StudentView detail(long id,String email) { return find(id,actor(email)); }
    @Transactional
    public StudentView create(NewStudent input,String email) {
        Account user=editor(email);NewStudent student=clean(input);
        long id=jdbc.queryForObject("INSERT dbo.estudiantes(nombres,apellidos,grado,seccion) OUTPUT INSERTED.id VALUES(?,?,?,?)",
            Long.class,student.nombres(),student.apellidos(),student.grado(),student.seccion());
        return find(id,user);
    }
    @Transactional
    public StudentView edit(long id,EditStudent input,String email) {
        Account user=editor(email);
        if(input==null) throw bad("Faltan los datos del estudiante.");
        NewStudent clean=clean(new NewStudent(input.nombres(),input.apellidos(),input.grado(),input.seccion()));
        if(input.version()==null || !input.version().matches("[0-9a-fA-F]{16}")) throw bad("Se requiere una versión hexadecimal de 16 caracteres.");
        find(id,user);
        int changed=jdbc.update("UPDATE dbo.estudiantes SET nombres=?,apellidos=?,grado=?,seccion=? WHERE id=? AND version=?",
            clean.nombres(),clean.apellidos(),clean.grado(),clean.seccion(),id,HexFormat.of().parseHex(input.version()));
        if(changed!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Otra persona modificó el estudiante. Recarga sus datos antes de guardar.");
        return find(id,user);
    }
    @Transactional
    public StudentView changeStatus(long id,StudentStatus input,String email) {
        Account user=editor(email);
        if(input==null || input.activo()==null) throw bad("Indica si el estudiante queda activo o inactivo.");
        if(input.version()==null || !input.version().matches("[0-9a-fA-F]{16}")) throw bad("Se requiere una versión hexadecimal de 16 caracteres.");
        find(id,user);
        int changed=jdbc.update("UPDATE dbo.estudiantes SET activo=? WHERE id=? AND version=?",
            input.activo(),id,HexFormat.of().parseHex(input.version()));
        if(changed!=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"Otra persona modificó el estudiante. Recarga sus datos antes de guardar.");
        return find(id,user);
    }
}
