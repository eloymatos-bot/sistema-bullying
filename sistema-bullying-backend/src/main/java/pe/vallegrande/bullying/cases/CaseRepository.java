package pe.vallegrande.bullying.cases;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import pe.vallegrande.bullying.users.Account;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static pe.vallegrande.bullying.cases.CaseModels.*;

@Repository
public class CaseRepository {
    private final JdbcTemplate jdbc;
    public CaseRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    private static final String SELECT = "SELECT c.id,c.codigo,c.fecha_incidente,c.lugar,c.tipo_codigo,t.nombre AS tipo,"
        + "c.estado_codigo,e.nombre AS estado,c.responsable_id,u.nombres+' '+u.apellidos AS responsable,"
        + "c.registrado_por,c.creado_en,c.descripcion,c.observaciones ";
    private static final String FROM = "FROM dbo.casos c JOIN dbo.tipos_incidente t ON t.codigo=c.tipo_codigo "
        + "JOIN dbo.estados_caso e ON e.codigo=c.estado_codigo JOIN dbo.usuarios u ON u.id=c.responsable_id ";
    private static final RowMapper<Summary> SUMMARY = (rs,n) -> new Summary(rs.getLong("id"),rs.getString("codigo"),
        rs.getDate("fecha_incidente").toLocalDate(),rs.getString("lugar"),rs.getString("tipo_codigo"),
        rs.getString("tipo"),rs.getString("estado_codigo"),rs.getString("estado"),rs.getLong("responsable_id"),
        rs.getString("responsable"),rs.getLong("registrado_por"),
        rs.getTimestamp("creado_en").toLocalDateTime().atOffset(ZoneOffset.UTC));
    private static final RowMapper<Student> STUDENT = (rs,n) -> new Student(rs.getLong("id"),rs.getString("nombres"),
        rs.getString("apellidos"),rs.getInt("grado"),rs.getString("seccion"));

    static String scope(Account actor, List<Object> args) {
        if (!actor.rol().equals("DOCENTE_TUTOR")) return "";
        args.add(actor.id()); args.add(actor.id());
        return " AND (c.registrado_por=? OR c.responsable_id=?)";
    }
    static String like(String value) {
        return "%" + value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_").replace("[", "\\[") + "%";
    }
    public Page<Summary> list(Account actor, String q, String estado, String tipo,
                              LocalDate desde, LocalDate hasta, int offset, int limit) {
        List<Object> args = new ArrayList<>();
        String where = " WHERE 1=1" + scope(actor,args);
        if (!q.isEmpty()) {
            where += " AND (c.codigo LIKE ? ESCAPE '\\' OR c.lugar LIKE ? ESCAPE '\\' "
                + "OR EXISTS (SELECT 1 FROM dbo.caso_estudiantes ce JOIN dbo.estudiantes s ON s.id=ce.estudiante_id "
                + "WHERE ce.caso_id=c.id AND (s.nombres+' '+s.apellidos) LIKE ? ESCAPE '\\'))";
            String term=like(q); args.add(term); args.add(term); args.add(term);
        }
        if (!estado.isEmpty()) { where += " AND c.estado_codigo=?"; args.add(estado); }
        if (!tipo.isEmpty()) { where += " AND c.tipo_codigo=?"; args.add(tipo); }
        if (desde != null) { where += " AND c.fecha_incidente>=?"; args.add(desde); }
        if (hasta != null) { where += " AND c.fecha_incidente<=?"; args.add(hasta); }
        Long total=jdbc.queryForObject("SELECT COUNT_BIG(*) " + FROM + where,Long.class,args.toArray());
        args.add(offset); args.add(limit);
        var rows=jdbc.query(SELECT+FROM+where+" ORDER BY c.id DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",SUMMARY,args.toArray());
        return new Page<>(rows,total,offset,limit);
    }
    public Optional<Detail> detail(long id, Account actor) {
        List<Object> args=new ArrayList<>(); args.add(id);
        String where=" WHERE c.id=?" + scope(actor,args);
        var rows=jdbc.query(SELECT+FROM+where,(rs,n)->new Detail(SUMMARY.mapRow(rs,n),rs.getString("descripcion"),
            rs.getString("observaciones"),List.of()),args.toArray());
        if (rows.isEmpty()) return Optional.empty();
        Detail row=rows.get(0);
        var people=jdbc.query("SELECT s.id,s.nombres,s.apellidos,s.grado,s.seccion,ce.participacion "
            + "FROM dbo.caso_estudiantes ce JOIN dbo.estudiantes s ON s.id=ce.estudiante_id WHERE ce.caso_id=? "
            + "ORDER BY s.id,ce.participacion",(rs,n)->new Involved(STUDENT.mapRow(rs,n),rs.getString("participacion")),id);
        return Optional.of(new Detail(row.caso(),row.descripcion(),row.observaciones(),people));
    }
    public boolean typeExists(String code) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.tipos_incidente WHERE codigo=?",Integer.class,code)>0;
    }
    public boolean stateExists(String code) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.estados_caso WHERE codigo=?",Integer.class,code)>0;
    }
    private String studentScope(Account actor,List<Object> args) {
        if (!actor.rol().equals("DOCENTE_TUTOR")) return "";
        args.add(actor.id()); args.add(actor.id());
        return " AND EXISTS (SELECT 1 FROM dbo.caso_estudiantes ce JOIN dbo.casos c ON c.id=ce.caso_id "
            + "WHERE ce.estudiante_id=s.id AND (c.registrado_por=? OR c.responsable_id=?))";
    }
    public boolean studentAvailable(long id, Account actor) {
        List<Object> args=new ArrayList<>(); args.add(id);
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.estudiantes s WHERE s.id=? AND s.activo=1"
            +studentScope(actor,args),Integer.class,args.toArray())>0;
    }
    public Page<Student> students(Account actor,String q,int offset,int limit) {
        List<Object> args=new ArrayList<>();
        String where=" WHERE s.activo=1"+studentScope(actor,args);
        if (!q.isEmpty()) { where+=" AND (s.nombres+' '+s.apellidos) LIKE ? ESCAPE '\\'"; args.add(like(q)); }
        Long total=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.estudiantes s"+where,Long.class,args.toArray());
        args.add(offset); args.add(limit);
        var rows=jdbc.query("SELECT s.id,s.nombres,s.apellidos,s.grado,s.seccion FROM dbo.estudiantes s"+where
            +" ORDER BY s.apellidos,s.nombres,s.id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",STUDENT,args.toArray());
        return new Page<>(rows,total,offset,limit);
    }
    public long insertStudent(NewStudent s) {
        return jdbc.queryForObject("INSERT dbo.estudiantes(nombres,apellidos,grado,seccion) OUTPUT INSERTED.id VALUES (?,?,?,?)",
            Long.class,s.nombres(),s.apellidos(),s.grado(),s.seccion());
    }
    public long insertCase(CreateCase c,long actorId) {
        return jdbc.queryForObject("INSERT dbo.casos(fecha_incidente,lugar,tipo_codigo,descripcion,observaciones,responsable_id,registrado_por) "
            +"OUTPUT INSERTED.id VALUES (?,?,?,?,?,?,?)",Long.class,c.fechaIncidente(),c.lugar(),c.tipoCodigo(),
            c.descripcion(),c.observaciones(),c.responsableId(),actorId);
    }
    public void attach(long caseId,long studentId,String participation) {
        jdbc.update("INSERT dbo.caso_estudiantes(caso_id,estudiante_id,participacion) VALUES (?,?,?)",caseId,studentId,participation);
    }
    public Catalogs catalogs() {
        return new Catalogs(jdbc.query("SELECT codigo,nombre FROM dbo.tipos_incidente ORDER BY nombre",(rs,n)->new Catalog(rs.getString(1),rs.getString(2))),
            jdbc.query("SELECT codigo,nombre FROM dbo.estados_caso ORDER BY codigo",(rs,n)->new Catalog(rs.getString(1),rs.getString(2))),
            List.of("AFECTADO","PRESUNTO_AGRESOR","TESTIGO"));
    }
    public List<Responsible> responsibles(String q,int offset,int limit) {
        return jdbc.query("SELECT id,nombres,apellidos,rol_codigo FROM dbo.usuarios WHERE activo=1 "
            +"AND (nombres+' '+apellidos) LIKE ? ESCAPE '\\' ORDER BY apellidos,nombres,id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
            (rs,n)->new Responsible(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getString(4)),like(q),offset,limit);
    }
}
