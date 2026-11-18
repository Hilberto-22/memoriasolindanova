import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { InstitutionalLogos } from '../institutional-logos/institutional-logos.component';
import { PortalBrand } from '../portal-brand/portal-brand.component';
import { PortalIcon } from '../portal-icon/portal-icon.component';
import { navigation } from '../../navigation';

@Component({
  selector:'public-header',
  imports:[RouterLink, RouterLinkActive, InstitutionalLogos, PortalBrand, PortalIcon],
  templateUrl:'./header.component.html'
})
export class Header {
  readonly links = navigation;
  readonly open = signal(false);
}
