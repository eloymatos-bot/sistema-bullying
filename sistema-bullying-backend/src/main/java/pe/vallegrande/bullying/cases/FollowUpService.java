package pe.vallegrande.bullying.cases;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.vallegrande.bullying.users.*;

@Service
public class FollowUpService {
    private final JdbcTemplate jdbc;
    private final AccountRepository accounts;
    private final CaseRepository cases;
    public FollowUpService(JdbcTemplate jdbc,AccountRepository accounts,CaseRepository cases){this.jdbc=jdbc;this.accounts=accounts;this.cases=cases;}
    public record Entry(long id,String accion,LocalDate fechaAccion,OffsetDateTime registradoEn,String autor,String estadoAnterior,String estadoNuevo,LocalDate proximaFecha){}
    public record Timeline(String version,String estado,LocalDate proximaFecha,boolean puedeEditar,List<Entry> historial){}
    public record Input(String version,String accion,LocalDate fechaAccion,String estado,LocalDate proximaFecha){}
    private record Locked(String state,long responsible,LocalDate incident,byte[] version){}
    private static ResponseStatusException bad(String s){return new ResponseStatusException(HttpStatus.BAD_REQUEST,s);}
    private Account actor(String email){return accounts.byEmail(email).filter(Account::activo).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
    static LocalDate today(){return LocalDate.now(ZoneId.of("America/Lima"));}
    private static LocalDate date(java.sql.Date d){return d==null?null:d.toLocalDate();}
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Timeline timeline(long id,String email){
        var a=actor(email);cases.detail(id,a).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Caso no encontrado."));
        var base=jdbc.queryForObject("SELECT version,estado_codigo,proximo_seguimiento,responsable_id FROM dbo.casos WHERE id=?",(r,n)->new Timeline(HexFormat.of().formatHex(r.getBytes("version")),r.getString("estado_codigo"),date(r.getDate("proximo_seguimiento")),!r.getString("estado_codigo").equals("CERRADO")&&(!a.rol().equals("DOCENTE_TUTOR")||r.getLong("responsable_id")==a.id()),List.of()),id);
        var history=jdbc.query("SELECT s.*,u.nombres+' '+u.apellidos autor FROM dbo.seguimientos s JOIN dbo.usuarios u ON u.id=s.registrado_por WHERE caso_id=? ORDER BY s.registrado_en DESC,s.id DESC",
            (r,n)->new Entry(r.getLong("id"),r.getString("accion"),r.getDate("fecha_accion").toLocalDate(),r.getTimestamp("registrado_en").toLocalDateTime().atOffset(ZoneOffset.UTC),r.getString("autor"),r.getString("estado_anterior"),r.getString("estado_nuevo"),date(r.getDate("proxima_fecha"))),id);
        return new Timeline(base.version(),base.estado(),base.proximaFecha(),base.puedeEditar(),history);
    }
    @Transactional
    public Timeline add(long id,Input input,String email){
        var a=actor(email);cases.detail(id,a).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Caso no encontrado."));
        var c=jdbc.queryForObject("SELECT estado_codigo,responsable_id,fecha_incidente,version FROM dbo.casos WITH (UPDLOCK,HOLDLOCK) WHERE id=?",(r,n)->new Locked(r.getString(1),r.getLong(2),r.getDate(3).toLocalDate(),r.getBytes(4)),id);
        if(a.rol().equals("DOCENTE_TUTOR")&&c.responsible()!=a.id())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el responsable asignado puede registrar el seguimiento.");
        if(c.state().equals("CERRADO"))throw new ResponseStatusException(HttpStatus.CONFLICT,"El caso está cerrado.");
        if(input==null)throw bad("Faltan los datos del seguimiento.");
        if(input.version()==null||!input.version().equalsIgnoreCase(HexFormat.of().formatHex(c.version())))throw new ResponseStatusException(HttpStatus.CONFLICT,"El caso cambió. Recarga antes de guardar; conserva tu texto.");
        String action=CaseService.required(input.accion(),4000,"Acción");
        if(input.fechaAccion()==null||input.fechaAccion().isBefore(c.incident())||input.fechaAccion().isAfter(today()))throw bad("La fecha de acción debe estar entre el incidente y hoy.");
        String state=input.estado();
        var next=Map.of("REPORTADO","EN_EVALUACION","EN_EVALUACION","EN_SEGUIMIENTO","EN_SEGUIMIENTO","CERRADO");
        if(state==null||(!state.equals(c.state())&&!state.equals(next.get(c.state()))))throw bad("Transición no válida. Sigue el orden: reportado, evaluación, seguimiento y cerrado.");
        if(state.equals("CERRADO")&&input.proximaFecha()!=null)throw bad("Un caso cerrado no tiene próxima fecha.");
        if(state.equals("CERRADO")&&jdbc.queryForObject("SELECT COUNT(*) FROM dbo.acuerdos WHERE caso_id=? AND cumplido_en IS NULL",Integer.class,id)>0)
            throw bad("Completa los acuerdos pendientes antes de cerrar el caso.");
        if(input.proximaFecha()!=null&&(input.proximaFecha().isBefore(today())||input.proximaFecha().isBefore(c.incident())||input.proximaFecha().getYear()>9999))throw bad("La próxima fecha debe ser hoy o posterior al incidente.");
        boolean changed=!state.equals(c.state());
        jdbc.update("INSERT dbo.seguimientos(caso_id,registrado_por,accion,fecha_accion,estado_anterior,estado_nuevo,proxima_fecha) VALUES (?,?,?,?,?,?,?)",id,a.id(),action,input.fechaAccion(),changed?c.state():null,changed?state:null,input.proximaFecha());
        int count=jdbc.update("UPDATE dbo.casos SET estado_codigo=?,proximo_seguimiento=?,cerrado_en=CASE WHEN ?='CERRADO' THEN SYSUTCDATETIME() ELSE NULL END WHERE id=? AND version=?",state,input.proximaFecha(),state,id,c.version());
        if(count!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"El caso cambió; recarga antes de guardar.");
        return timeline(id,email);
    }
}
