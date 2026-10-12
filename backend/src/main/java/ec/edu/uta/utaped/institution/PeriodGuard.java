package ec.edu.uta.utaped.institution;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class PeriodGuard {
    private final JdbcTemplate jdbc;
    public PeriodGuard(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    // Held until transaction completion: closure cannot race a document mutation.
    public void writable(UUID period) {
        var states=jdbc.queryForList("SELECT status FROM academic_period WHERE id=? FOR SHARE",String.class,period);
        if(states.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Período no disponible.");
        if(!states.getFirst().equals("ACTIVE")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El período está cerrado. Su historial es de solo lectura.");
    }
}
