import { Component, inject } from "@angular/core";
import { RouterLink } from "@angular/router";
import { Api } from "../../../api";
import { resource } from "../../../data-state";
import { MemoryMap } from "../../discovery/memory-map/memory-map.component";
import { CollectionCard } from "../../../shared/components/collection-card/collection-card.component";
import { EmptyState } from "../../../shared/components/empty-state/empty-state.component";
import { ErrorState } from "../../../shared/components/error-state/error-state.component";
import { LoadingState } from "../../../shared/components/loading-state/loading-state.component";
import { PortalIcon } from "../../../shared/components/portal-icon/portal-icon.component";
import { materialIcon } from "../../../shared/material-types";

@Component({
  imports: [
    RouterLink,
    CollectionCard,
    EmptyState,
    ErrorState,
    LoadingState,
    MemoryMap,
    PortalIcon,
  ],
  templateUrl: "./home.component.html",
})
export class Home {
  private readonly api = inject(Api);
  readonly editorial = resource(() => this.api.portalContent());
  readonly collections = resource(() => this.api.collections());
  readonly recent = resource(() => this.api.catalog({ size: 3 }));
  readonly places = resource(() => this.api.map());
  readonly dates = resource(() => this.api.timeline());
  readonly materialIcon = materialIcon;
}
