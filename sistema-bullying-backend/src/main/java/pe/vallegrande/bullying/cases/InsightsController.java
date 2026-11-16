package pe.vallegrande.bullying.cases;
import java.security.Principal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.vallegrande.bullying.users.*;
import static pe.vallegrande.bullying.cases.CaseModels.*;

@RestController
@Transactional(readOnly=true)
public class InsightsController {
 private final JdbcTemplate jdbc;
 private final AccountRepository accounts;
 public InsightsController(JdbcTemplate jdbc,AccountRepository accounts){this.jdbc=jdbc;this.accounts=accounts;}
 private Account actor(Principal p){return accounts.byEmail(p.getName()).filter(Account::activo).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
 public record Alert(long id,String codigo,String estudiantes,long responsableId,String responsable,String clase,LocalDate fecha,LocalDate ultimaAccion){}
 public record Alerts(Page<Alert> resultados,long vencidas,long proximas,long atendidas,long sinFecha){}
 @GetMapping("/api/alerts")
 public Alerts alerts(Principal p,@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String clase,@RequestParam(required=false) Long responsableId,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit){
  CaseService.page(offset,limit);q=CaseService.search(q);var a=actor(p);var today=FollowUpService.today();
  if(!Set.of("","VENCIDA","PROXIMA","EVALUACION","SIN_FECHA").contains(clase))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Filtro de alerta no válido.");
  List<Object> scoped=new ArrayList<>();String visible=CaseRepository.scope(a,scoped);
  List<Object> summaryArgs=new ArrayList<>(List.of(today,today,today.plusDays(7)));summaryArgs.addAll(scoped);
  long[] counts=jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN estado_codigo<>'CERRADO' AND proximo_seguimiento<? THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN estado_codigo<>'CERRADO' AND proximo_seguimiento BETWEEN ? AND ? THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN estado_codigo<>'CERRADO' AND proximo_seguimiento IS NULL THEN 1 ELSE 0 END),0) FROM dbo.casos c WHERE 1=1"+visible,(r,n)->new long[]{r.getLong(1),r.getLong(2),r.getLong(3)},summaryArgs.toArray());
  List<Object> attendedArgs=new ArrayList<>(List.of(today.withDayOfMonth(1),today.plusDays(1)));attendedArgs.addAll(scoped);
  long attended=jdbc.queryForObject("SELECT COUNT(*) FROM dbo.casos c WHERE EXISTS (SELECT 1 FROM dbo.seguimientos s WHERE s.caso_id=c.id AND s.fecha_accion>=? AND s.fecha_accion<?)"+visible,Long.class,attendedArgs.toArray());
  List<Object> args=new ArrayList<>(List.of(today,today.plusDays(7)));
  String derived="(SELECT c.id,c.codigo,c.responsable_id,u.nombres+' '+u.apellidos responsable,c.proximo_seguimiento fecha,CASE WHEN c.proximo_seguimiento IS NULL THEN CASE WHEN c.estado_codigo='REPORTADO' THEN 'EVALUACION' ELSE 'SIN_FECHA' END WHEN c.proximo_seguimiento<? THEN 'VENCIDA' ELSE 'PROXIMA' END clase,"
    +"COALESCE((SELECT STRING_AGG(CONVERT(nvarchar(max),s.nombres+' '+s.apellidos),', ') FROM dbo.caso_estudiantes ce JOIN dbo.estudiantes s ON s.id=ce.estudiante_id WHERE ce.caso_id=c.id AND ce.participacion='AFECTADO'),'') estudiantes,(SELECT MAX(fecha_accion) FROM dbo.seguimientos s WHERE s.caso_id=c.id) ultima_accion FROM dbo.casos c JOIN dbo.usuarios u ON u.id=c.responsable_id WHERE c.estado_codigo<>'CERRADO' AND (c.proximo_seguimiento IS NULL OR c.proximo_seguimiento<=?)"+CaseRepository.scope(a,args)+") x";
  String where=" WHERE 1=1";
  if(!q.isEmpty()){where+=" AND (codigo LIKE ? ESCAPE '\\' OR estudiantes LIKE ? ESCAPE '\\')";args.add(CaseRepository.like(q));args.add(CaseRepository.like(q));}
  if(!clase.isEmpty()){where+=" AND clase=?";args.add(clase);}
  if(responsableId!=null){where+=" AND responsable_id=?";args.add(responsableId);}
  long total=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM "+derived+where,Long.class,args.toArray());args.add(offset);args.add(limit);
  var rows=jdbc.query("SELECT * FROM "+derived+where+" ORDER BY CASE WHEN clase='VENCIDA' THEN 0 WHEN fecha IS NULL THEN 1 ELSE 2 END,fecha,id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",(r,n)->new Alert(r.getLong("id"),r.getString("codigo"),r.getString("estudiantes"),r.getLong("responsable_id"),r.getString("responsable"),r.getString("clase"),r.getDate("fecha")==null?null:r.getDate("fecha").toLocalDate(),r.getDate("ultima_accion")==null?null:r.getDate("ultima_accion").toLocalDate()),args.toArray());
  return new Alerts(new Page<>(rows,total,offset,limit),counts[0],counts[1],attended,counts[2]);
 }
 public record Group(String codigo,String nombre,long total){}
 public record Month(int mes,long registrados,long cerrados,Double promedioDias){}
 public record Report(int year,long total,long cerrados,Double promedioDias,long vencidos,long totalAnterior,List<Group> tipos,List<Group> estados,List<Month> meses){}
 @GetMapping("/api/reports")
 public Report report(Principal p,@RequestParam int year,@RequestParam(required=false) Integer grado,
                      @RequestParam(defaultValue="") String seccion){
  if(year<1900||year>9998)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Año fuera de rango.");
  if(grado!=null&&(grado<1||grado>5))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El grado debe ser de 1 a 5.");
  seccion=seccion.strip().toUpperCase(Locale.ROOT);
  if(seccion.length()>10||seccion.codePoints().anyMatch(Character::isISOControl))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Sección no válida (máximo 10 caracteres).");
  var a=actor(p);String registered="DATEADD(hour,-5,c.creado_en)";List<Object> args=new ArrayList<>(List.of(LocalDate.of(year,1,1),LocalDate.of(year+1,1,1)));
  List<Object> filters=new ArrayList<>();
  String visible=CaseRepository.scope(a,filters);
  if(grado!=null||!seccion.isEmpty()){
   visible+=" AND EXISTS (SELECT 1 FROM dbo.caso_estudiantes ce JOIN dbo.estudiantes s ON s.id=ce.estudiante_id WHERE ce.caso_id=c.id";
   if(grado!=null){visible+=" AND s.grado=?";filters.add(grado);}
   if(!seccion.isEmpty()){visible+=" AND UPPER(s.seccion)=?";filters.add(seccion);}
   visible+=")";
  }
  args.addAll(filters);
  String where=" WHERE "+registered+">=? AND "+registered+"<?"+visible;
  List<Object> totalArgs=new ArrayList<>(List.of(FollowUpService.today()));totalArgs.addAll(args);
  var totals=jdbc.queryForMap("SELECT COUNT_BIG(*) total,COALESCE(SUM(CASE WHEN estado_codigo='CERRADO' THEN 1 ELSE 0 END),0) cerrados,AVG(CASE WHEN estado_codigo='CERRADO' THEN DATEDIFF_BIG(second,creado_en,cerrado_en)/86400.0 END) promedio,COALESCE(SUM(CASE WHEN estado_codigo<>'CERRADO' AND proximo_seguimiento<? THEN 1 ELSE 0 END),0) vencidos FROM dbo.casos c"+where,totalArgs.toArray());
  var types=jdbc.query("SELECT t.codigo,t.nombre,COUNT_BIG(c.id) total FROM dbo.tipos_incidente t LEFT JOIN dbo.casos c ON c.tipo_codigo=t.codigo AND "+registered+">=? AND "+registered+"<?"+visible+" GROUP BY t.codigo,t.nombre ORDER BY total DESC,t.nombre",(r,n)->new Group(r.getString(1),r.getString(2),r.getLong(3)),args.toArray());
  var states=jdbc.query("SELECT t.codigo,t.nombre,COUNT_BIG(c.id) total FROM dbo.estados_caso t LEFT JOIN dbo.casos c ON c.estado_codigo=t.codigo AND "+registered+">=? AND "+registered+"<?"+visible+" GROUP BY t.codigo,t.nombre ORDER BY t.codigo",(r,n)->new Group(r.getString(1),r.getString(2),r.getLong(3)),args.toArray());
  var months=jdbc.query("SELECT MONTH("+registered+") mes,COUNT_BIG(*) total,SUM(CASE WHEN estado_codigo='CERRADO' THEN 1 ELSE 0 END) cerrados,AVG(CASE WHEN estado_codigo='CERRADO' THEN DATEDIFF_BIG(second,creado_en,cerrado_en)/86400.0 END) promedio FROM dbo.casos c"+where+" GROUP BY MONTH("+registered+") ORDER BY mes",(r,n)->new Month(r.getInt(1),r.getLong(2),r.getLong(3),r.getObject(4)==null?null:r.getDouble(4)),args.toArray());
  var priorArgs=new ArrayList<Object>(List.of(LocalDate.of(year-1,1,1),LocalDate.of(year,1,1)));priorArgs.addAll(filters);
  long prior=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.casos c"+where,Long.class,priorArgs.toArray());
  return new Report(year,((Number)totals.get("total")).longValue(),((Number)totals.get("cerrados")).longValue(),totals.get("promedio")==null?null:((Number)totals.get("promedio")).doubleValue(),((Number)totals.get("vencidos")).longValue(),prior,types,states,months);
 }
}
