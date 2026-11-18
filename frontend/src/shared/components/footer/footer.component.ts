import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { InstitutionalLogos } from '../institutional-logos/institutional-logos.component';
import { PortalBrand } from '../portal-brand/portal-brand.component';

@Component({
  selector:'public-footer',
  imports:[RouterLink, InstitutionalLogos, PortalBrand],
  templateUrl:'./footer.component.html'
})
export class Footer {}
