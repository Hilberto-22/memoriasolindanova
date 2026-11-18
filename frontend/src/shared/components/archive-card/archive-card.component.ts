import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Memory } from '../../../api';
import { Favorites } from '../../../core/favorites';
import { materialIcon } from '../../material-types';
import { PortalIcon } from '../portal-icon/portal-icon.component';

@Component({
  selector:'archive-card', imports:[RouterLink,PortalIcon],
  templateUrl:'./archive-card.component.html'
})
export class ArchiveCard {
  readonly item = input.required<Memory>();
  readonly favorites = inject(Favorites);
  readonly materialIcon = materialIcon;
}
