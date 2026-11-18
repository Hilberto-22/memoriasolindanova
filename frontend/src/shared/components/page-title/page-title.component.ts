import { Component, input } from '@angular/core';

@Component({
  selector: 'page-title',
  templateUrl: './page-title.component.html'
})
export class PageTitle {
  readonly title = input.required<string>();
  readonly description = input('');
}
