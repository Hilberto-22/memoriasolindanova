import { Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Collection } from '../../../api';
import { PortalIcon } from '../portal-icon/portal-icon.component';

@Component({
  selector:'collection-card', imports:[RouterLink, PortalIcon],
  templateUrl:'./collection-card.component.html'
})
export class CollectionCard {
  readonly item = input.required<Collection>();
  readonly icon = computed(() => ({
    cidade:'home', educacao:'book', cultura:'audio', trabalho:'folder', pessoas:'people', documentos:'document'
  })[this.item().slug] ?? 'layers');
}
