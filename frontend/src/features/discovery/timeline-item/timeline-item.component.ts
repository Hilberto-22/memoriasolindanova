import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Memory } from '../../../api';

@Component({
  selector:'timeline-item', imports:[RouterLink],
  templateUrl: './timeline-item.component.html'
})
export class TimelineItem { readonly item = input.required<Memory>(); }
