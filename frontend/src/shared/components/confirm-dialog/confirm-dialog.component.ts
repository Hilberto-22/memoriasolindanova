import { Component, input, output } from '@angular/core';

@Component({
  selector:'confirm-dialog',
  templateUrl: './confirm-dialog.component.html'
})
export class ConfirmDialog {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly action = input.required<string>();
  readonly confirmed = output<void>();
}
