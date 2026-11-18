package br.org.memorias.application;

import br.org.memorias.domain.ContributionModels.Review;
import br.org.memorias.domain.PublicationMetadata;
import br.org.memorias.persistence.ContributionWriter;
import br.org.memorias.persistence.PublicationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ModerationService {
    private final ContributionWriter contributions;
    private final PublicationRepository publications;

    ModerationService(ContributionWriter contributions, PublicationRepository publications) {
        this.contributions = contributions;
        this.publications = publications;
    }

    @Transactional
    public void review(long id, String administrator, Review review) {
        String status = review.status();
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) throw InputChecks.bad("Decisão inválida.");
        if (!contributions.decide(id, administrator, status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A contribuição já foi revisada ou não existe.");
        }
        if ("APPROVED".equals(status)) {
            var data = new PublicationMetadata(review.category(), review.credits(), review.usage(),
                    review.year(), review.latitude(), review.longitude());
            publications.publish(id, data.category(), data.credits(), data.usage(), data.year(), data.latitude(), data.longitude());
        }
    }
}
