package br.org.memorias.application;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.org.memorias.domain.ArchiveModels.Collection;
import br.org.memorias.domain.ArchiveModels.Contribution;
import br.org.memorias.persistence.AnalyticsRepository;
import br.org.memorias.persistence.AnalyticsRepository.Count;
import br.org.memorias.persistence.CollectionRepository;
import br.org.memorias.persistence.ContributionRepository;

@Service
public class DashboardService {
    private final AnalyticsRepository analytics;
    private final CollectionRepository collections;
    private final ContributionRepository contributions;

    public DashboardService(AnalyticsRepository analytics, CollectionRepository collections,
            ContributionRepository contributions) {
        this.analytics = analytics;
        this.collections = collections;
        this.contributions = contributions;
    }

    public Dashboard dashboard() {
        var byCollection = collections.collections();
        var statuses = analytics.statuses();
        var firstDay = LocalDate.now(ZoneOffset.UTC).minusDays(29);
        var counts = analytics.submissionDates(firstDay.atStartOfDay().atOffset(ZoneOffset.UTC))
                .stream().collect(Collectors.groupingBy(day -> day, Collectors.counting()));
        var days = firstDay.datesUntil(firstDay.plusDays(30))
                .map(day -> new Count(day.toString(), counts.getOrDefault(day, 0L))).toList();
        var statusMap = statuses.stream().collect(Collectors.toMap(Count::label, Count::count));
        return new Dashboard(byCollection.stream().mapToLong(Collection::count).sum(),
                statusMap.getOrDefault("PENDING", 0L),
                statusMap.getOrDefault("APPROVED", 0L), byCollection.size(), analytics.collaborators(),
                byCollection, contributions.contributions(6), analytics.types(), statuses, days);
    }

    public record Dashboard(long published, long pending, long approved, int collections, long collaborators,
            List<Collection> byCollection, List<Contribution> recent,
            List<Count> byType, List<Count> byStatus, List<Count> byDay) {
    }
}
