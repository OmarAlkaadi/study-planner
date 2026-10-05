package de.studyplanner.calendar;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * REST-Controller für den Kalender (GET /api/calendar). Liefert alle Einträge eines Zeitraums;
 * die eigentliche Arbeit macht der CalendarService.
 */
@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /** Beispiel: GET /api/calendar?from=2026-10-01&amp;to=2026-10-31 */
    @GetMapping
    public List<CalendarEvent> getEvents(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return calendarService.getEvents(from, to);
    }
}
