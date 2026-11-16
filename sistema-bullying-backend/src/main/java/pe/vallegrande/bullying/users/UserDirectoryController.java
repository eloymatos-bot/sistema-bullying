package pe.vallegrande.bullying.users;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.vallegrande.bullying.cases.CaseModels.Page;
import pe.vallegrande.bullying.cases.CaseService;
@Tag(name = "Usuarios")
@RestController
public class UserDirectoryController {
 private final JdbcTemplate jdbc;
 public UserDirectoryController(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public record Directory(Page<Account.UserView> resultados,long activos,long convivencia,long docentes){}
 @Operation(summary = "Buscar usuarios con filtros",
     description = "Busca por nombre o correo y filtra por rol o estado. Incluye los totales del directorio.")
 @GetMapping("/api/users/directory")
 public Directory directory(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String rol,@RequestParam(required=false) Boolean activo,@RequestParam(defaultValue="0") int offset,@RequestParam(defaultValue="20") int limit){
  CaseService.page(offset,limit);q=CaseService.search(q);
  if(!rol.isEmpty()&&!AccountService.ROLES.contains(rol))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Rol no válido.");
  List<Object> args=new ArrayList<>();String where=" WHERE 1=1";
  if(!q.isEmpty()){where+=" AND (nombres+' '+apellidos LIKE ? ESCAPE '\\' OR correo LIKE ? ESCAPE '\\')";String term="%"+q.replace("\\","\\\\").replace("%","\\%").replace("_","\\_").replace("[","\\[")+"%";args.add(term);args.add(term);}
  if(!rol.isEmpty()){where+=" AND rol_codigo=?";args.add(rol);}
  if(activo!=null){where+=" AND activo=?";args.add(activo);}
  long total=jdbc.queryForObject("SELECT COUNT_BIG(*) FROM dbo.usuarios"+where,Long.class,args.toArray());args.add(offset);args.add(limit);
  var rows=jdbc.query("SELECT id,nombres,apellidos,correo,rol_codigo,activo FROM dbo.usuarios"+where+" ORDER BY apellidos,nombres,id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",(r,n)->new Account.UserView(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getBoolean(6)),args.toArray());
  long[] counts=jdbc.queryForObject("SELECT COALESCE(SUM(CASE WHEN activo=1 THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN activo=1 AND rol_codigo='CONVIVENCIA' THEN 1 ELSE 0 END),0),COALESCE(SUM(CASE WHEN activo=1 AND rol_codigo='DOCENTE_TUTOR' THEN 1 ELSE 0 END),0) FROM dbo.usuarios",(r,n)->new long[]{r.getLong(1),r.getLong(2),r.getLong(3)});
  return new Directory(new Page<>(rows,total,offset,limit),counts[0],counts[1],counts[2]);
 }
}
