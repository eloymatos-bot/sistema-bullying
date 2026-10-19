package pe.vallegrande.bullying.users;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public class AccountRepository {
    private final JdbcTemplate jdbc;
    private static final String COLUMNS = "id,nombres,apellidos,correo,password_hash,rol_codigo,activo";
    private static final RowMapper<Account> MAPPER = (rs, n) -> new Account(rs.getLong("id"),
        rs.getString("nombres"), rs.getString("apellidos"), rs.getString("correo"),
        rs.getString("password_hash"), rs.getString("rol_codigo"), rs.getBoolean("activo"));

    public AccountRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Account> byEmail(String email) {
        return jdbc.query("SELECT " + COLUMNS + " FROM dbo.usuarios WHERE correo=?", MAPPER, email).stream().findFirst();
    }
    public Optional<Account> byId(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM dbo.usuarios WHERE id=?", MAPPER, id).stream().findFirst();
    }
    public List<Account.UserView> list(int offset, int limit) {
        return jdbc.query("SELECT " + COLUMNS + " FROM dbo.usuarios ORDER BY id OFFSET ? ROWS FETCH NEXT ? ROWS ONLY",
            MAPPER, offset, limit).stream().map(Account::view).toList();
    }
    public Account insert(String nombres, String apellidos, String correo, String hash, String rol) {
        Long id = jdbc.queryForObject("INSERT dbo.usuarios(nombres,apellidos,correo,password_hash,rol_codigo) "
            + "OUTPUT INSERTED.id VALUES (?,?,?,?,?)", Long.class, nombres, apellidos, correo, hash, rol);
        return byId(id).orElseThrow();
    }
    public int lockAndCountAdmins() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM dbo.usuarios WITH (UPDLOCK,HOLDLOCK) "
            + "WHERE rol_codigo='ADMINISTRADOR' AND activo=1", Integer.class);
    }
    public void setActive(long id, boolean active) {
        jdbc.update("UPDATE dbo.usuarios SET activo=? WHERE id=?", active, id);
    }
    public void edit(long id,String nombres,String apellidos,String correo,String rol){
        jdbc.update("UPDATE dbo.usuarios SET nombres=?,apellidos=?,correo=?,rol_codigo=? WHERE id=?",nombres,apellidos,correo,rol,id);
    }
}
