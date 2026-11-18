import { Component, computed, input } from '@angular/core';
import { Count } from '../../../api';
import { PortalIcon } from '../../../shared/components/portal-icon/portal-icon.component';

@Component({
  selector:'dashboard-trend', imports:[PortalIcon],
  templateUrl:'./dashboard-trend.component.html'
})
export class DashboardTrend {
  readonly days = input.required<Count[]>();
  readonly maximum = computed(() => Math.max(1,...this.days().map(item => item.count)));
  readonly total = computed(() => this.days().reduce((sum,item) => sum + item.count,0));
  readonly points = computed(() => this.days().map((item,index,days) => ({
    ...item, x:18 + 364 * index / Math.max(1,days.length - 1), y:128 - 104 * item.count / this.maximum()
  })));
  readonly line = computed(() => this.points().map(item => item.x + ',' + item.y).join(' '));
}
