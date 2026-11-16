package pe.vallegrande.bullying.cases;

import java.security.Principal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import pe.vallegrande.bullying.users.Account;
import pe.vallegrande.bullying.users.AccountRepository;

@RestController
@RequestMapping("/api/cases/{id}/agreements")
public class AgreementController {
    private final JdbcTemplate db;
    private final AccountRepository accounts;
    public AgreementController(JdbcTemplate db, AccountRepository accounts) { this.db=db; this.accounts=accounts; }
    public record Agreement(long id,String descripcion,long responsableId,String responsable,LocalDate fechaLimite,
                            String creadoPor,OffsetDateTime creadoEn,OffsetDateTime cumplidoEn,String cumplidoPor,String version) {}
    public record Directory(boolean puedeEditar,List<Agreement> items) {}
    public record Create(String descripcion,Long responsableId,LocalDate fechaLimite) {}
    public record Complete(String version) {}
    private record CaseAccess(String estado,long responsable) {
        boolean editable(Account a) { return !"CERRADO".equals(estado) && (!"DOCENTE_TUTOR".equals(a.rol()) || a.id()==responsable); }
    }
    private static ResponseStatusException problem(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
    private Account actor(Principal p) {
        return accounts.byEmail(p.getName()).filter(Account::activo)
            .orElseThrow(()->problem(HttpStatus.UNAUTHORIZED,"Sesión no válida."));
    }
    private CaseAccess access(long id,Account a,boolean write) {
        List<Object> args=new ArrayList<>(); args.add(id);
        var rows=db.query("SELECT c.estado_codigo,c.responsable_id FROM dbo.casos c"
            +(write?" WITH (UPDLOCK,HOLDLOCK)":"")+" WHERE c.id=?"+CaseRepository.scope(a,args),
            (r,n)->new CaseAccess(r.getString(1),r.getLong(2)),args.toArray());
        if(rows.isEmpty()) throw problem(HttpStatus.NOT_FOUND,"Caso no encontrado.");
        var c=rows.get(0);
        if(write && "CERRADO".equals(c.estado())) throw problem(HttpStatus.CONFLICT,"El caso está cerrado.");
        if(write && !c.editable(a)) throw problem(HttpStatus.FORBIDDEN,"Solo el docente asignado puede gestionar acuerdos de este caso.");
        return c;
    }
    private List<Agreement> read(long id,Long agreementId) {
        return db.query("""
            SELECT g.id,g.descripcion,g.responsable_id,r.nombres+' '+r.apellidos responsable,
            g.fecha_limite,a.nombres+' '+a.apellidos autor,g.creado_en,g.cumplido_en,
            u.nombres+' '+u.apellidos completado_por,g.version
            FROM dbo.acuerdos g JOIN dbo.usuarios r ON r.id=g.responsable_id
            JOIN dbo.usuarios a ON a.id=g.creado_por LEFT JOIN dbo.usuarios u ON u.id=g.cumplido_por
            WHERE g.caso_id=?
            """+(agreementId==null?"":" AND g.id=?")+" ORDER BY g.id DESC",(rs,n)->{
                var completed=rs.getTimestamp("cumplido_en");
                return new Agreement(rs.getLong("id"),rs.getString("descripcion"),rs.getLong("responsable_id"),
                    rs.getString("responsable"),rs.getDate("fecha_limite").toLocalDate(),rs.getString("autor"),
                    rs.getTimestamp("creado_en").toLocalDateTime().atOffset(ZoneOffset.UTC),
                    completed==null?null:completed.toLocalDateTime().atOffset(ZoneOffset.UTC),
                    rs.getString("completado_por"),HexFormat.of().formatHex(rs.getBytes("version")));
            },agreementId==null?new Object[]{id}:new Object[]{id,agreementId});
    }
    @GetMapping
    @Transactional(readOnly=true)
    public Directory list(@PathVariable long id,Principal p) {
        var a=actor(p);return new Directory(access(id,a,false).editable(a),read(id,null));
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Agreement create(@PathVariable long id,@RequestBody Create input,Principal p) {
        var a=actor(p);access(id,a,true);
        String description=CaseService.required(input.descripcion(),2000,"Descripción del acuerdo");
        if(input.fechaLimite()==null || input.fechaLimite().isBefore(FollowUpService.today()) || input.fechaLimite().getYear()>9999)
            throw problem(HttpStatus.BAD_REQUEST,"La fecha límite debe ser hoy o posterior.");
        if(input.responsableId()==null || accounts.byId(input.responsableId()).filter(Account::activo).isEmpty())
            throw problem(HttpStatus.BAD_REQUEST,"Selecciona un responsable activo.");
        long key=db.queryForObject("""
            INSERT dbo.acuerdos(caso_id,descripcion,responsable_id,fecha_limite,creado_por)
            OUTPUT INSERTED.id VALUES(?,?,?,?,?)
            """,Long.class,id,description,input.responsableId(),input.fechaLimite(),a.id());
        return read(id,key).get(0);
    }
    @PatchMapping("/{agreementId}/complete")
    @Transactional
    public Agreement complete(@PathVariable long id,@PathVariable long agreementId,@RequestBody Complete input,Principal p) {
        var a=actor(p);access(id,a,true);
        if(input.version()==null || !input.version().matches("[0-9a-fA-F]{16}"))
            throw problem(HttpStatus.BAD_REQUEST,"Se requiere la versión del acuerdo.");
        var previous=read(id,agreementId).stream().findFirst()
            .orElseThrow(()->problem(HttpStatus.NOT_FOUND,"Acuerdo no encontrado."));
        if(previous.cumplidoEn()!=null)
            throw problem(HttpStatus.CONFLICT,"El acuerdo ya se marcó como cumplido.");
        int changed=db.update("""
            UPDATE dbo.acuerdos SET cumplido_en=SYSUTCDATETIME(),cumplido_por=?
            WHERE id=? AND caso_id=? AND version=? AND cumplido_en IS NULL
            """,a.id(),agreementId,id,HexFormat.of().parseHex(input.version()));
        if(changed!=1) throw problem(HttpStatus.CONFLICT,"El acuerdo cambió. Recarga y vuelve a revisar.");
        return read(id,agreementId).get(0);
    }
}
