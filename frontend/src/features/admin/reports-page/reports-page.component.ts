import { Component, inject } from '@angular/core';
import { map, of } from 'rxjs';
import { Api } from '../../../api';
import { resource } from '../../../data-state';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LoadingState } from '../../../shared/components/loading-state/loading-state.component';

@Component({
  imports:[LoadingState, ErrorState],
  templateUrl: './reports-page.component.html'
})
export class ReportsPage {
  private readonly api = inject(Api);
  readonly data = resource(() => this.api.dashboard());
  download() {
    const rows = this.data.state().data?.byCollection ?? [];
    const csv = ['Coleção;Registros publicados', ...rows.map(row => '"' + row.title.replaceAll('"','""') + '";' + row.count)].join('\r\n');
    const url = URL.createObjectURL(new Blob(['\uFEFF' + csv], {type:'text/csv;charset=utf-8'}));
    const link = document.createElement('a');
    link.href = url;
    link.download = 'registros-por-colecao.csv';
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
}
