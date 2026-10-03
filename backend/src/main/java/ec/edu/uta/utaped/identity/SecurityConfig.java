package ec.edu.uta.utaped.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import ec.edu.uta.utaped.audit.AuditService;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return PasswordEncoderFactories.createDelegatingPasswordEncoder(); }
    @Bean UserDetailsService users(JdbcTemplate jdbc) {
        return input -> jdbc.query("SELECT email,password_hash,system_role,active FROM app_user WHERE email=?", (rs,n) ->
            User.withUsername(rs.getString("email")).password(rs.getString("password_hash"))
                .roles(rs.getString("system_role")).disabled(!rs.getBoolean("active")).build(), Accounts.normalize(input))
            .stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
    static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
    @Bean SecurityFilterChain security(HttpSecurity http, Accounts accounts, AuthThrottle throttle, AuditService audit) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password", "/actuator/health", "/error").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginProcessingUrl("/api/auth/login")
                .successHandler((request,response,auth) -> { var actor=accounts.current(auth.getName()); audit.record(actor.id(),"LOGIN_SUCCEEDED",actor.id()); response.setStatus(204); })
                .failureHandler((request,response,e) -> error(response,401,"Correo o contraseña incorrectos.")))
            .logout(logout -> logout.logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true).deleteCookies("SESSION")
                .logoutSuccessHandler((request,response,auth) -> { if(auth!=null) { var actor=accounts.find(auth.getName()); audit.record(actor.id(),"LOGOUT_SUCCEEDED",actor.id()); } response.setStatus(204); }))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request,response,e) -> error(response,401,"Inicie sesión para continuar."))
                .accessDeniedHandler((request,response,e) -> error(response,403,"No tiene permiso o la verificación de seguridad expiró.")))
            .requestCache(cache -> cache.disable());
        http.addFilterBefore(new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                try {
                    if (request.getRequestURI().equals("/api/auth/login") && request.getMethod().equals("POST")) {
                        String email = request.getParameter("username");
                        String password = request.getParameter("password");
                        if (email == null || email.length() > 254 || password == null || password.length() > 128) {
                            error(response,401,"Correo o contraseña incorrectos."); return;
                        }
                        throttle.check("login:" + Accounts.normalize(email), 10);
                        throttle.check("ip:" + request.getRemoteAddr(), 100);
                    }
                    var auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
                        var account = accounts.current(auth.getName());
                        var refreshed = UsernamePasswordAuthenticationToken.authenticated(auth.getPrincipal(), null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + account.systemRole())));
                        SecurityContextHolder.getContext().setAuthentication(refreshed);
                        String path = request.getRequestURI();
                        if (account.mustChangePassword() && !List.of("/api/auth/me","/api/auth/csrf","/api/auth/change-password","/api/auth/logout").contains(path)) {
                            error(response,403,"Cambie su contraseña temporal para continuar."); return;
                        }
                    }
                    chain.doFilter(request,response);
                } catch (ResponseStatusException e) {
                    error(response,e.getStatusCode().value(),e.getReason());
                }
            }
        }, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
