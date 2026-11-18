package br.org.memorias.web;

import br.org.memorias.application.DashboardService;
import br.org.memorias.persistence.AnalyticsRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
class DashboardController {
    private final DashboardService dashboard;
    private final AnalyticsRepository analytics;

    DashboardController(DashboardService dashboard, AnalyticsRepository analytics) {
        this.dashboard = dashboard;
        this.analytics = analytics;
    }

    @GetMapping("/admin/dashboard")
    DashboardService.Dashboard dashboard() { return dashboard.dashboard(); }

    @GetMapping("/material-counts")
    java.util.List<AnalyticsRepository.Count> materialCounts() { return analytics.types(); }
}
