import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector:'app-root', imports:[RouterOutlet],
  template:'<a class="skip" href="#conteudo">Pular para o conteúdo</a><router-outlet/>'
})
export class App {}
