import { Component, input, output } from '@angular/core';

@Component({
  selector:'app-pagination',
  templateUrl: './pagination.component.html'
})
export class Pagination {
  readonly page = input.required<number>();
  readonly total = input.required<number>();
  readonly changed = output<number>();
}
