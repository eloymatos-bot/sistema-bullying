package pe.vallegrande.bullying.security;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import pe.vallegrande.bullying.users.Account;
import pe.vallegrande.bullying.users.AccountRepository;
import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountRepository repository;
    public AuthController(AccountRepository repository) { this.repository = repository; }
    @GetMapping("/csrf")
    public Map<String,String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }
    @GetMapping("/me")
    public Account.UserView me(Principal principal) {
        return repository.byEmail(principal.getName()).orElseThrow().view();
    }
}
