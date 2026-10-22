package ec.edu.uta.utaped.planning;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Business days exclude weekends and the registered planning holidays. */
@Component
public class BusinessCalendar {
    private final JdbcTemplate jdbc;
    public BusinessCalendar(JdbcTemplate jdbc) { this.jdbc=jdbc; }

    /** Returns the date reached after counting the given business days strictly after {@code from}. */
    public LocalDate addBusinessDays(LocalDate from,int days) {
        // Two weeks per business day plus a margin always covers weekends and clustered holidays.
        var holidays=new HashSet<>(jdbc.queryForList("SELECT holiday_date FROM planning_holiday WHERE holiday_date>? AND holiday_date<=?",LocalDate.class,from,from.plusDays(14L*days+14)));
        var date=from;
        for(int counted=0;counted<days;) {
            date=date.plusDays(1);
            if(date.getDayOfWeek()!=DayOfWeek.SATURDAY&&date.getDayOfWeek()!=DayOfWeek.SUNDAY&&!holidays.contains(date)) counted++;
        }
        return date;
    }
}
