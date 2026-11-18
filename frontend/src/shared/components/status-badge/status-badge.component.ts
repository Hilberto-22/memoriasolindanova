import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'status-badge',
  template: '<span class="status" [class.approved]="status() === \'APPROVED\'" [class.rejected]="status() === \'REJECTED\'">{{label()}}</span>'
})
export class StatusBadge {
  readonly status = input.required<string>();
  readonly label = computed(() => ({PENDING:'Pendente', APPROVED:'Publicada', REJECTED:'Recusada'})[this.status()] ?? this.status());
}
