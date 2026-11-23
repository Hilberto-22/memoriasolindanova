package br.org.memorias.application;

import br.org.memorias.domain.ContributionModels.Submission;
import br.org.memorias.persistence.ArchiveRepository;
import br.org.memorias.persistence.CollectionRepository;
import br.org.memorias.persistence.ContributionWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import static br.org.memorias.application.InputChecks.*;

@Service
public class ContributionService {
    private static final Set<String> TYPES = Set.of("Fotografia", "Documento", "Relato", "Entrevista", "Áudio", "Vídeo", "Outro");
    private final ContributionWriter writer;
    private final CollectionRepository collections;
    private final ArchiveRepository archive;
    private final AttachmentService attachments;
    private final TransactionTemplate transaction;

    ContributionService(ContributionWriter writer, CollectionRepository collections, ArchiveRepository archive,
                        AttachmentService attachments, TransactionTemplate transaction) {
        this.writer = writer;
        this.collections = collections;
        this.archive = archive;
        this.attachments = attachments;
        this.transaction = transaction;
    }

    public long submit(Submission submission, List<MultipartFile> files) {
        return submit(submission, files, false);
    }

    public long submitDraft(Submission submission, List<MultipartFile> files) {
        return submit(submission, files, true);
    }

    private long submit(Submission submission, List<MultipartFile> files, boolean draft) {
        if (!draft && !Boolean.TRUE.equals(submission.consent())) throw bad("É necessário autorizar o envio e a publicação.");
        if (files.size() > 5) throw bad("Envie até cinco arquivos.");
        String name = draft ? optional(submission.name(), 150, "Nome") : required(submission.name(), 150, "Nome");
        String email = draft ? optional(submission.email(), 254, "E-mail") : required(submission.email(), 254, "E-mail");
        if (!email.isEmpty() && !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw bad("Informe um e-mail válido.");
        String title = required(submission.title(), 180, "Título");
        String type = required(submission.type(), 80, "Tipo de material");
        if (!TYPES.contains(type)) throw bad("Tipo de material inválido.");
        String description = draft ? optional(submission.description(), 6000, "Descrição") : required(submission.description(), 6000, "Descrição");
        String source = draft ? optional(submission.source(), 500, "Origem do material") : required(submission.source(), 500, "Origem do material");
        String period = optional(submission.period(), 150, "Data aproximada");
        String place = optional(submission.place(), 180, "Local");
        String collection = required(submission.collection(), 50, "Coleção");

        var createdKeys = new ArrayList<String>();
        try {
            return transaction.execute(status -> {
                if (!collections.exists(collection)) throw bad("Coleção inválida.");
                Long related = submission.relatedMemory();
                if (related != null && !archive.published(related)) throw bad("Registro relacionado inválido.");
                long id = writer.insert(collection, name, email, title, type, description, period, place, source,
                        Boolean.TRUE.equals(submission.consent()), related);
                for (MultipartFile file : files) attachments.save(id, file, createdKeys);
                return id;
            });
        } catch (RuntimeException failure) {
            attachments.remove(createdKeys);
            throw failure;
        }
    }
}
