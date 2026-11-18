import { Component, signal } from "@angular/core";

@Component({
  selector: "institutional-logos",
  templateUrl: "./institutional-logos.component.html",
})
export class InstitutionalLogos {
  readonly logos = [
    {
      path: "logo_centro_educa_mais.jpeg",
      name: "Centro Educa Mais Professor José Ribamar Santos Silva",
    },
    { path: "logo_maranhao.jpeg", name: "Governo do Maranhão / SEDUC" },
    { path: "ure_viana.jpeg", name: "URE Viana" },
  ];
  readonly unavailable = signal<string[]>([]);
  hide(path: string) {
    this.unavailable.update((paths) => [...paths, path]);
  }
}
