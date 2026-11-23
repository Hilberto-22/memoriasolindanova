package br.org.memorias.web;

import br.org.memorias.application.ContributionService;
import br.org.memorias.application.ModerationService;
import br.org.memorias.domain.ArchiveModels.Contribution;
import br.org.memorias.domain.ContributionModels.Review;
import br.org.memorias.domain.ContributionModels.Submission;
import br.org.memorias.persistence.ContributionRepository;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
class ContributionController {
    private final ContributionService intake;
    private final ContributionRepository contributions;
    private final ModerationService moderation;

    ContributionController(ContributionService intake, ContributionRepository contributions, ModerationService moderation) {
        this.intake = intake;
        this.contributions = contributions;
        this.moderation = moderation;
    }

    @PostMapping(path = "/contributions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Long> submit(@RequestPart("memory") Submission submission,
                             @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return Map.of("id", intake.submit(submission, files == null ? List.of() : files));
    }

    @PostMapping(path = "/admin/drafts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    Map<String, Long> draft(@RequestPart("memory") Submission submission,
                            @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return Map.of("id", intake.submitDraft(submission, files == null ? List.of() : files));
    }

    @GetMapping("/admin/contributions")
    List<Contribution> contributions() { return contributions.contributions(200); }

    @PatchMapping("/admin/contributions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void review(@PathVariable long id, @RequestBody Review review, Principal principal) {
        moderation.review(id, principal.getName(), review);
    }
}
