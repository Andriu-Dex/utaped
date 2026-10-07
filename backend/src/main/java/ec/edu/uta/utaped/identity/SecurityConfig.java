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
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
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
    @Bean PasswordEncoder passwordEncoder() {
        return new DelegatingPasswordEncoder("argon2id", java.util.Map.of("argon2id",new Argon2PasswordEncoder(16,32,1,19456,2),"bcrypt",new BCryptPasswordEncoder()));
    }
    @Bean UserDetailsService users(JdbcTemplate jdbc) {
        return input -> jdbc.query("SELECT email,password_hash,system_role,active FROM app_user WHERE email=? OR username=?", (rs,n) ->
            User.withUsername(rs.getString("email")).password(rs.getString("password_hash"))
                .roles(rs.getString("system_role")).disabled(!rs.getBoolean("active")).build(), Accounts.normalize(input),Accounts.normalize(input))
            .stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
    @Bean DaoAuthenticationProvider authenticationProvider(UserDetailsService users,PasswordEncoder encoder,JdbcTemplate jdbc,AuditService audit) {
        var provider=new DaoAuthenticationProvider(users);provider.setPasswordEncoder(encoder);
        provider.setUserDetailsPasswordService((user,password) -> {
            if(jdbc.update("UPDATE app_user SET password_hash=? WHERE email=? AND password_hash=? AND active",password,user.getUsername(),user.getPassword())!=1)
                throw new org.springframework.security.authentication.BadCredentialsException("Credentials changed");
            audit.record(jdbc.queryForObject("SELECT id FROM app_user WHERE email=?",java.util.UUID.class,user.getUsername()),"PASSWORD_HASH_UPGRADED",null);
            return User.withUserDetails(user).password(password).build();
        });
        return provider;
    }
    static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }
    @Bean SecurityFilterChain security(HttpSecurity http, Accounts accounts, AuthThrottle throttle, AuditService audit, CaptchaService captcha) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(new HttpSessionCsrfTokenRepository()))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/csrf", "/api/auth/captcha", "/api/auth/login", "/api/auth/forgot-password", "/api/auth/reset-password", "/actuator/health", "/error").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginProcessingUrl("/api/auth/login")
                .successHandler((request,response,auth) -> { var actor=accounts.current(auth.getName()); audit.record(actor.id(),"LOGIN_SUCCEEDED",actor.id()); response.setStatus(204); })
                .failureHandler((request,response,e) -> error(response,401,"Usuario o contraseña incorrectos.")))
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
                            error(response,401,"Usuario o contraseña incorrectos."); return;
                        }
                        throttle.check("ip:" + request.getRemoteAddr(), 100);
                        throttle.check("login:" + accounts.loginKey(email), 10);
                        if(!captcha.consume(request)) { error(response,400,"El código de verificación es incorrecto o expiró. Introduzca el nuevo código.");return; }
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
                    if(e.getStatusCode().value()==401) {
                        SecurityContextHolder.clearContext();
                        var session=request.getSession(false);if(session!=null) session.invalidate();
                    }
                    error(response,e.getStatusCode().value(),e.getReason());
                }
            }
        }, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
