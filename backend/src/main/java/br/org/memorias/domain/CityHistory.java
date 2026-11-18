package br.org.memorias.domain;

import java.time.LocalDate;

public final class CityHistory {
    private CityHistory() {}

    public record Input(String title, String category, String content, String period,
            LocalDate startDate, LocalDate endDate, String source, Long memoryId) {}

    public record Entry(long id, String title, String category, String content, String period,
            LocalDate startDate, LocalDate endDate, String source, Long memoryId,
            String memoryTitle, String imageUrl) {}
}
