package br.org.memorias.domain;

public record PortalContent(String heroTitle, String heroDescription, String communityTitle,
        String communityDescription,
        String about, String guardians, String contactEmail, Long featuredMemory) {
    public record Feature(long id, String title, String description, String imageUrl) {
    }

    public record View(PortalContent content, Feature feature) {
    }
}
