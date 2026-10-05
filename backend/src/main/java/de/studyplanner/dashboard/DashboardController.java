package de.studyplanner.dashboard;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-Controller für das Dashboard (GET /api/dashboard).
 * Liefert alle Kennzahlen und Listen in einer einzigen Antwort, damit das Frontend nur eine Anfrage braucht.
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** GET /api/dashboard – alle Statistiken des angemeldeten Benutzers. */
    @GetMapping
    public DashboardResponse getDashboard() {
        return dashboardService.getDashboard();
    }

    /** GET /api/dashboard/progress – nur die Lern-Statistiken (für die Seite "Lernfortschritt"). */
    @GetMapping("/progress")
    public ProgressResponse getProgress() {
        return dashboardService.getProgress();
    }
}
