import { Component, input } from '@angular/core';

@Component({
  selector: 'portal-icon',
  templateUrl: './portal-icon.component.html',
  host: {'aria-hidden':'true'}
})
export class PortalIcon {
  readonly name = input.required<string>();
}
