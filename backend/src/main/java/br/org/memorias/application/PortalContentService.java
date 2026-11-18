package br.org.memorias.application;

import org.springframework.stereotype.Service;

import static br.org.memorias.application.InputChecks.bad;
import static br.org.memorias.application.InputChecks.optional;
import static br.org.memorias.application.InputChecks.required;
import br.org.memorias.domain.PortalContent;
import br.org.memorias.persistence.ArchiveRepository;
import br.org.memorias.persistence.PortalContentRepository;

@Service
public class PortalContentService {
    private final PortalContentRepository content;
    private final ArchiveRepository archive;
    private final EditorialImageSelection images;

    public PortalContentService(PortalContentRepository content, ArchiveRepository archive,
            EditorialImageSelection images) {
        this.content = content;
        this.archive = archive;
        this.images = images;
    }

    public PortalContent.View read() {
        var data = content.read();
        var memory = data.featuredMemory() == null ? null : archive.detail(data.featuredMemory());
        return new PortalContent.View(data, memory == null ? null
                : new PortalContent.Feature(memory.id(), memory.title(), memory.description(), memory.imageUrl()));
    }

    public void save(PortalContent input) {
        String email = optional(input.contactEmail(), 254, "E-mail de contato");
        if (!email.isEmpty() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw bad("Informe um e-mail válido.");
        content.save(new PortalContent(required(input.heroTitle(), 180, "Título principal"),
                required(input.heroDescription(), 800, "Descrição principal"),
                required(input.communityTitle(), 180, "Título do convite"),
                required(input.communityDescription(), 800, "Descrição do convite"),
                optional(input.about(), 4000, "Sobre o projeto"), optional(input.guardians(), 4000, "Guardiões"),
                email, images.validate(input.featuredMemory())));
    }
}
