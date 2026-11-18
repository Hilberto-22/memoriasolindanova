import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Api } from '../../../api';
import { resource } from '../../../data-state';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';

@Component({
  imports:[LoadingState, ErrorState],
  templateUrl: './admin-information.component.html'
})
export class AdminInformation {
  readonly title: string = inject(ActivatedRoute).snapshot.data['heading'];
  private readonly api = inject(Api);
  readonly user = resource(() => this.api.session());
}
