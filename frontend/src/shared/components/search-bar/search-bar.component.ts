import { Component, input, output } from '@angular/core';

@Component({
  selector: 'search-bar',
  templateUrl: './search-bar.component.html'
})
export class SearchBar {
  readonly value = input('');
  readonly label = input('Buscar no acervo');
  readonly searched = output<string>();
  submit(event: Event, value: string) { event.preventDefault(); this.searched.emit(value.trim()); }
}
