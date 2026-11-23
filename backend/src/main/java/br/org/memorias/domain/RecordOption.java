package br.org.memorias.domain;

public record RecordOption(long id, String kind, String value) {
    public record Input(String kind, String value) {}
}
