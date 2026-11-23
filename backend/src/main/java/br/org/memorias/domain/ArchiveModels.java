package br.org.memorias.domain;

import java.time.OffsetDateTime;
import java.util.List;

public final class ArchiveModels {
    private ArchiveModels() {
    }

    public record Collection(String slug, String title, long count, String imageUrl, String description,
            Long coverMemory) {
    }

    public record Facets(List<String> types, List<String> categories, List<String> places) {
    }

    public record RecordOptions(List<String> places, List<String> sources) {
    }

    public record Page(List<Memory> items, long total, int page, int size) {
    }

    public record MemoryFile(long id, String name, String contentType, String url) {
    }

    public record StoredFile(String key, String name, String type) {
    }

    public record Memory(long id, @com.fasterxml.jackson.annotation.JsonIgnore long contributionId, String title,
            String type,
            String category, String collection, String collectionSlug, String description, String period,
            Integer year, String place, String source, String credits, String usage, Double latitude,
            Double longitude, String imageUrl, List<MemoryFile> files) {
        public Memory withFiles(List<MemoryFile> files) {
            return new Memory(id, contributionId, title, type, category, collection, collectionSlug, description,
                    period, year, place, source, credits, usage, latitude, longitude, imageUrl, files);
        }
    }

    public record Contribution(long id, String name, String email, String title, String type, String description,
            String place, String period, String source, String collection, String status,
            OffsetDateTime createdAt, Long relatedMemory, List<MemoryFile> files) {
        public Contribution withFiles(List<MemoryFile> files) {
            return new Contribution(id, name, email, title, type, description, place, period, source,
                    collection, status, createdAt, relatedMemory, files);
        }
    }
}
