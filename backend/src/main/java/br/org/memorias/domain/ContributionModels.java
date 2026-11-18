package br.org.memorias.domain;

public final class ContributionModels {
    private ContributionModels() {
    }

    public record Submission(String name, String email, String type, String title, String description,
            String period, String place, String collection, String source, Boolean consent, Long relatedMemory) {
    }

    public record Review(String status, String category, String credits, String usage, Integer year, Double latitude,
            Double longitude) {
    }
}
