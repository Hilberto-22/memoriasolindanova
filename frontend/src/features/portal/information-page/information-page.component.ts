import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Api } from '../../../api';
import { resource } from '../../../data-state';
import { EmptyState } from '../../../shared/components/empty-state/empty-state.component';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { PageTitle } from '../../../shared/components/page-title/page-title.component';

@Component({
  imports:[RouterLink, EmptyState, ErrorState, LoadingState, PageTitle],
  templateUrl:'./information-page.component.html'
})
export class InformationPage {
  private readonly api = inject(Api);
  readonly page = inject(ActivatedRoute).snapshot.data;
  readonly editorial = resource(() => this.api.portalContent());
  readonly description = computed(() => {
    const content = this.editorial.state().data?.content;
    if (this.page['section'] === 'about') return content?.about;
    if (this.page['section'] === 'guardians') return content?.guardians;
    return undefined;
  });
}
