import { Component, inject } from '@angular/core';
import { Api } from '../../../api';
import { resource } from '../../../data-state';
import { CollectionCard } from '../../../shared/components/collection-card/collection-card.component';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';
import { PageTitle } from '../../../shared/components/page-title/page-title.component';

@Component({
  imports: [CollectionCard, ErrorState, LoadingState, PageTitle],
  templateUrl: './collections-page.component.html'
})
export class CollectionsPage {
  private readonly api = inject(Api);
  readonly collections = resource(() => this.api.collections());
}
