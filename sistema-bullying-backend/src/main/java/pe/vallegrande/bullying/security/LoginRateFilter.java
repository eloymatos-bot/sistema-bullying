package pe.vallegrande.bullying.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Límite local: 15 intentos por IP/minuto. Para varias instancias usar un almacén compartido. */
final class LoginRateFilter extends OncePerRequestFilter {
    private record Window(long start, int attempts) {}
    private final Map<String, Window> windows = new HashMap<>();
    private synchronized boolean allow(String ip) {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(e -> now - e.getValue().start() >= 60_000);
        Window previous = windows.get(ip);
        if (previous == null && windows.size() >= 1000) return false;
        if (previous != null && previous.attempts() >= 15) return false;
        windows.put(ip, previous == null ? new Window(now, 1) : new Window(previous.start(), previous.attempts()+1));
        return true;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (req.getMethod().equals("POST") && req.getServletPath().equals("/api/auth/login") && !allow(req.getRemoteAddr())) {
            res.setStatus(429); res.setHeader("Retry-After", "60");
            res.setContentType("application/json;charset=UTF-8");
            res.getWriter().write("{\"error\":\"Demasiados intentos. Espera un minuto.\"}");
            return;
        }
        if (req.getMethod().equals("POST") && req.getServletPath().equals("/api/auth/login")) {
            String email = req.getParameter("email"), password = req.getParameter("password");
            if (email == null || email.strip().length() > 254 || password == null || password.isEmpty()
                    || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                res.setStatus(401); res.setContentType("application/json;charset=UTF-8");
                res.getWriter().write("{\"error\":\"Correo o contraseña incorrectos, o cuenta inactiva.\"}");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
