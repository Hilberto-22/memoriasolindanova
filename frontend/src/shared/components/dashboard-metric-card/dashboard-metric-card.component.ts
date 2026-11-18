import { Component, input } from '@angular/core';
import { PortalIcon } from '../portal-icon/portal-icon.component';

@Component({
  selector:'dashboard-metric', imports:[PortalIcon],
  templateUrl:'./dashboard-metric-card.component.html'
})
export class DashboardMetricCard {
  readonly label = input.required<string>();
  readonly value = input.required<number>();
  readonly icon = input.required<string>();
  readonly tone = input('');
}
