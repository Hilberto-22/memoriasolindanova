import { Component, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { PortalBrand } from '../../../shared/components/portal-brand/portal-brand.component';
import { PortalIcon } from '../../../shared/components/portal-icon/portal-icon.component';

@Component({
  selector:'admin-sidebar', imports:[RouterLink, RouterLinkActive, PortalBrand, PortalIcon],
  templateUrl:'./sidebar.component.html'
})
export class Sidebar {
  readonly chosen = output<void>();
  readonly links = [
    {path:'/admin/historia-da-cidade',label:'História da cidade',icon:'book'},
    {path:'/admin',label:'Dashboard',icon:'home'}, {path:'/admin/acervo',label:'Acervo',icon:'layers'},
    {path:'/admin/contribuicoes',label:'Contribuições',icon:'upload'}, {path:'/admin/conta',label:'Conta administrativa',icon:'lock'},
    {path:'/admin/colecoes',label:'Coleções',icon:'folder'}, {path:'/admin/mapa',label:'Mapa',icon:'pin'},
    {path:'/admin/linha-do-tempo',label:'Linha do tempo',icon:'calendar'}, {path:'/admin/relatorios',label:'Relatórios',icon:'chart'},
    {path:'/admin/configuracoes',label:'Configurações',icon:'settings'}
  ];
}
