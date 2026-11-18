import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Footer } from '../../../shared/components/footer/footer.component';
import { Header } from '../../../shared/components/header/header.component';

@Component({
  imports:[RouterOutlet, Header, Footer],
  template:'<public-header/><main id="conteudo" tabindex="-1"><router-outlet/></main><public-footer/>'
})
export class PublicLayout {}
