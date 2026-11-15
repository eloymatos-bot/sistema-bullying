package pe.vallegrande.bullying.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.vallegrande.bullying.users.AccountRepository;
import java.io.IOException;

final class ActiveAccountFilter extends OncePerRequestFilter {
    private final AccountRepository repository;
    ActiveAccountFilter(AccountRepository repository) { this.repository = repository; }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            var user = repository.byEmail(authentication.getName());
            if (user.isEmpty() || !user.get().activo()
                || authentication.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_" + user.get().rol()))) {
                SecurityContextHolder.clearContext();
                var session = req.getSession(false);
                if (session != null) session.invalidate();
                res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"La sesión ya no es válida. Inicia sesión nuevamente.\"}");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
