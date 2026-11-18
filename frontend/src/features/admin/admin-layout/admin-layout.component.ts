import { Component, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { Api } from '../../../api';
import { resource } from '../../../data-state';
import { PortalIcon } from '../../../shared/components/portal-icon/portal-icon.component';
import { SearchBar } from '../../../shared/components/search-bar/search-bar.component';
import { Sidebar } from '../sidebar/sidebar.component';

@Component({
  imports:[RouterOutlet, RouterLink, Sidebar, PortalIcon, SearchBar],
  templateUrl:'./admin-layout.component.html'
})
export class AdminLayout {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  readonly user = resource(() => this.api.session());
  readonly error = signal('');
  readonly open = signal(false);
  readonly loggingOut = signal(false);
  search(query:string) { void this.router.navigate(['/admin/acervo'], {queryParams:{q:query || null}}); }
  logout() {
    if (this.loggingOut()) return;
    this.error.set('');
    this.loggingOut.set(true);
    this.api.logout().subscribe({next:() => void this.router.navigate(['/entrar'], {replaceUrl:true}),
      error:() => {
        this.loggingOut.set(false);
        this.error.set('Não foi possível encerrar a sessão. Tente novamente.');
      }});
  }
}
