import { Component, computed, input } from '@angular/core';
import { Count } from '../../../api';
import { PortalIcon } from '../../../shared/components/portal-icon/portal-icon.component';

@Component({
  selector:'dashboard-distribution', imports:[PortalIcon],
  templateUrl:'./dashboard-distribution.component.html'
})
export class DashboardDistribution {
  readonly heading = input.required<string>();
  readonly icon = input.required<string>();
  readonly rows = input.required<Count[]>();
  readonly total = computed(() => this.rows().reduce((sum,row) => sum + row.count,0));
  readonly legend = computed(() => {
    const colors = ['#145430','#a97616','#815132','#78905a','#b04d37','#6f6350','#ceb36f'];
    return this.rows().map((row,index) => ({...row, color:colors[index % colors.length],
      percent:this.total() ? 100 * row.count / this.total() : 0}));
  });
  readonly gradient = computed(() => {
    let start = 0;
    const segments = this.legend().map(row => {
      const end = start + row.percent;
      const segment = row.color + ' ' + start + '% ' + end + '%';
      start = end;
      return segment;
    });
    return segments.length ? 'conic-gradient(' + segments.join(', ') + ')' : '#e8dfce';
  });
}
