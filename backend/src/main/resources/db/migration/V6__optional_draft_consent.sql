-- Public contributions still require consent in ContributionService.
-- Administrative drafts may omit it; preserve the actual choice instead of assuming consent.
alter table contributions drop constraint contributions_consent_check;
