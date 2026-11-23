import { Routes } from "@angular/router";
import { CityHistoryEditor } from "./features/admin/city-history-editor/city-history-editor.component";
import { CityHistoryPage } from "./features/portal/city-history-page/city-history-page.component";
import { requireAdmin } from "./core/auth/admin.guard";
import { requireGuest } from "./core/auth/guest.guard";
import { AdminLayout } from "./features/admin/admin-layout/admin-layout.component";
import { AdminInformation } from "./features/admin/admin-information/admin-information.component";
import { CollectionEditor } from "./features/admin/collection-editor/collection-editor.component";
import { DashboardPage } from "./features/admin/dashboard-page/dashboard-page.component";
import { LoginPage } from "./features/admin/login-page/login-page.component";
import { PortalEditor } from "./features/admin/portal-editor/portal-editor.component";
import { RecordEditor } from "./features/admin/record-editor/record-editor.component";
import { RecordOptionsEditor } from "./features/admin/record-options-editor/record-options-editor.component";
import { RecordManagement } from "./features/admin/record-management/record-management.component";
import { ReportsPage } from "./features/admin/reports-page/reports-page.component";
import { ReviewPage } from "./features/admin/review-page/review-page.component";
import { ContributionPage } from "./features/contributions/contribution-page/contribution-page.component";
import { MapPage } from "./features/discovery/map-page/map-page.component";
import { TimelinePage } from "./features/discovery/timeline-page/timeline-page.component";
import { CatalogPage } from "./features/portal/catalog-page/catalog-page.component";
import { CollectionsPage } from "./features/portal/collections-page/collections-page.component";
import { DetailPage } from "./features/portal/detail-page/detail-page.component";
import { Home } from "./features/portal/home/home.component";
import { InformationPage } from "./features/portal/information-page/information-page.component";
import { PublicLayout } from "./features/portal/public-layout/public-layout.component";

export const routes: Routes = [
  { path: 'entrar', component: LoginPage, canActivate: [requireGuest], title: 'Acesso administrativo — Maranhão de Memórias' },
  {
    path: "admin",
    component: AdminLayout,
    canActivateChild: [requireAdmin],
    children: [
      {
        path: "historia-da-cidade",
        component: CityHistoryEditor,
        title: "História da cidade — Administração",
      },
      {
        path: "",
        component: DashboardPage,
        title: "Dashboard — Maranhão de Memórias",
      },
      {
        path: "acervo",
        component: RecordManagement,
        title: "Acervo — Administração",
      },
      {
        path: "acervo/novo",
        component: ContributionPage,
        data: { admin: true },
        title: "Cadastrar registro — Administração",
      },
      {
        path: "acervo/:id/editar",
        component: RecordEditor,
        title: "Editar registro — Administração",
      },
      {
        path: "contribuicoes",
        component: ReviewPage,
        title: "Contribuições — Administração",
      },
      {
        path: "colecoes",
        component: CollectionEditor,
        title: "Coleções — Administração",
      },
      {
        path: "locais-e-origens",
        component: RecordOptionsEditor,
        title: "Locais e origens — Administração",
      },
      {
        path: "conta",
        component: AdminInformation,
        data: { heading: "Conta administrativa" },
        title: "Conta administrativa — Administração",
      },
      { path: 'usuarios', pathMatch: 'full', redirectTo: 'conta' },
      { path: "mapa", component: MapPage, title: "Mapa — Administração" },
      {
        path: "linha-do-tempo",
        component: TimelinePage,
        title: "Linha do Tempo — Administração",
      },
      {
        path: "relatorios",
        component: ReportsPage,
        title: "Relatórios — Administração",
      },
      {
        path: "configuracoes",
        component: PortalEditor,
        title: "Configurações — Administração",
      },
    ],
  },
  {
    path: "",
    component: PublicLayout,
    children: [
      {
        path: "historia-da-cidade",
        component: CityHistoryPage,
        title: "História da cidade — Maranhão de Memórias",
      },
      {
        path: "",
        pathMatch: "full",
        component: Home,
        title: "Maranhão de Memórias — Acervo Digital",
      },
      {
        path: "acervo",
        component: CatalogPage,
        title: "Explorar o Acervo — Maranhão de Memórias",
      },
      {
        path: "acervo/:id",
        component: DetailPage,
        title: "Registro — Maranhão de Memórias",
      },
      {
        path: "colecoes",
        component: CollectionsPage,
        title: "Coleções — Maranhão de Memórias",
      },
      {
        path: "mapa",
        component: MapPage,
        title: "Mapa das Memórias — Maranhão de Memórias",
      },
      {
        path: "linha-do-tempo",
        component: TimelinePage,
        title: "Linha do Tempo — Maranhão de Memórias",
      },
      {
        path: "contribuir",
        component: ContributionPage,
        title: "Compartilhe uma memória — Maranhão de Memórias",
      },
      {
        path: "guardioes",
        component: InformationPage,
        title: "Os Guardiões da Memória",
        data: {
          heading: "Os Guardiões da Memória",
          section: "guardians",
          link: "/contribuir",
          action: "Participar do acervo",
        },
      },
      {
        path: "sobre",
        component: InformationPage,
        title: "Sobre o Projeto — Maranhão de Memórias",
        data: {
          heading: "Sobre o Projeto",
          section: "about",
          link: "/acervo",
          action: "Explorar o acervo",
        },
      },
      {
        path: "contato",
        component: InformationPage,
        title: "Contato — Maranhão de Memórias",
        data: {
          heading: "Contato",
          section: "contact",
          link: "/contribuir",
          action: "Compartilhar uma memória",
        },
      },
      {
        path: "**",
        component: InformationPage,
        title: "Página não encontrada",
        data: {
          heading: "Página não encontrada",
          link: "/",
          action: "Voltar ao início",
        },
      },
    ],
  },
];
