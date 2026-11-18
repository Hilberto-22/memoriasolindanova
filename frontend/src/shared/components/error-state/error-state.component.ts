import { Component, input, output } from '@angular/core';

@Component({
  selector:'error-state',
  templateUrl: './error-state.component.html'
})
export class ErrorState {
  readonly message = input('Não foi possível carregar os dados.');
  readonly retry = output<void>();
}
