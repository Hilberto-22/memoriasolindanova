package br.org.memorias.domain;

import static br.org.memorias.application.InputChecks.*;

public record PublicationMetadata(String category, String credits, String usage, Integer year, Double latitude, Double longitude) {
    public PublicationMetadata {
        category = required(category, 100, "Categoria");
        credits = required(credits, 250, "Créditos");
        usage = required(usage, 500, "Condições de uso");
        if (year != null && (year < 1 || year > 2200)) throw bad("Ano inválido.");
        if ((latitude == null) != (longitude == null)
                || (latitude != null && (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || Math.abs(latitude) > 90 || Math.abs(longitude) > 180))) {
            throw bad("Informe latitude e longitude válidas em conjunto.");
        }
    }
}
