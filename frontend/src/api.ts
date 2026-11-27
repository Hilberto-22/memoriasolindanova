import { inject, Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { switchMap } from "rxjs";

export interface CollectionEdit {
  slug: string;
  title: string;
  description: string;
  coverMemory: number | null;
}
export interface Collection extends CollectionEdit {
  count: number;
  imageUrl: string | null;
}
export interface Count {
  label: string;
  count: number;
}
export interface CityHistoryInput {
  title: string;
  category: string;
  content: string;
  period: string;
  startDate: string | null;
  endDate: string | null;
  source: string;
  memoryId: number | null;
}
export interface CityHistoryEntry extends CityHistoryInput {
  id: number;
  memoryTitle: string | null;
  imageUrl: string | null;
}
export interface PortalContent {
  heroTitle: string;
  heroDescription: string;
  communityTitle: string;
  communityDescription: string;
  about: string;
  guardians: string;
  contactEmail: string;
  featuredMemory: number | null;
}
export interface PortalView {
  content: PortalContent;
  feature: {
    id: number;
    title: string;
    description: string;
    imageUrl: string | null;
  } | null;
}
export interface MemoryFile {
  id: number;
  name: string;
  contentType: string;
  url: string;
}
export interface Memory {
  id: number;
  title: string;
  type: string;
  category: string;
  collection: string;
  collectionSlug: string;
  description: string;
  period: string;
  year: number | null;
  place: string;
  source: string;
  credits: string;
  usage: string;
  latitude: number | null;
  longitude: number | null;
  imageUrl?: string;
  files?: MemoryFile[];
}
export interface Catalog {
  items: Memory[];
  total: number;
  page: number;
  size: number;
}
export interface Facets {
  types: string[];
  categories: string[];
  places: string[];
}
export interface RecordOptions {
  places: string[];
  sources: string[];
}
export interface RecordOptionInput {
  kind: 'PLACE' | 'SOURCE';
  value: string;
}
export interface RecordOption extends RecordOptionInput {
  id: number;
}
export interface Contribution {
  id: number;
  name: string;
  email: string;
  title: string;
  type: string;
  description: string;
  place: string;
  period: string;
  source: string;
  collection: string;
  status: string;
  createdAt: string;
  files: MemoryFile[];
  relatedMemory: number | null;
}
export interface Dashboard {
  published: number;
  pending: number;
  approved: number;
  collections: number;
  byCollection: Collection[];
  recent: Contribution[];
  collaborators: number;
  byType: Count[];
  byStatus: Count[];
  byDay: Count[];
}
@Injectable({ providedIn: "root" })
export class Api {
  private readonly http = inject(HttpClient);
  collections() {
    return this.http.get<Collection[]>("/api/collections");
  }
  facets() {
    return this.http.get<Facets>("/api/facets");
  }
  recordOptions() {
    return this.http.get<RecordOptions>("/api/admin/record-options");
  }
  recordOptionEntries() {
    return this.http.get<RecordOption[]>("/api/admin/record-options/entries");
  }
  saveRecordOption(payload: RecordOptionInput, id: number | null) {
    const url = "/api/admin/record-options/entries";
    return this.csrf().pipe(switchMap(() => id === null
      ? this.http.post<{id:number}>(url, payload)
      : this.http.put<void>(url + "/" + id, payload)));
  }
  deleteRecordOption(id: number) {
    return this.csrf().pipe(switchMap(() => this.http.delete<void>("/api/admin/record-options/entries/" + id)));
  }
  materialCounts() {
    return this.http.get<Count[]>("/api/material-counts");
  }
  portalContent() {
    return this.http.get<PortalView>("/api/portal-content");
  }
  savePortalContent(payload: PortalContent) {
    return this.csrf().pipe(
      switchMap(() =>
        this.http.put<void>("/api/admin/portal-content", payload),
      ),
    );
  }
  saveCollection(payload: CollectionEdit, editing: boolean) {
    const url = "/api/admin/collections";
    return this.csrf().pipe(
      switchMap(() =>
        editing
          ? this.http.put<void>(
              url + "/" + encodeURIComponent(payload.slug),
              payload,
            )
          : this.http.post<void>(url, payload),
      ),
    );
  }
  editMemory(id: number, payload: object, files: File[] = []) {
    let body: object = payload;
    if (files.length) {
      const multipart = new FormData();
      multipart.append('memory', new Blob([JSON.stringify(payload)], {type:'application/json'}));
      for (const file of files) multipart.append('files', file, file.name);
      body = multipart;
    }
    return this.csrf().pipe(
      switchMap(() =>
        this.http.put<void>("/api/admin/memories/" + id, body, {observe:'events', reportProgress:true}),
      ),
    );
  }
  deleteMemory(id: number) {
    return this.csrf().pipe(switchMap(() => this.http.delete<void>("/api/admin/memories/" + id)));
  }
  catalog(filters: Record<string, string | number> = {}) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) {
      if (value !== "") params = params.set(key, value);
    }
    return this.http.get<Catalog>("/api/memories", { params });
  }
  memory(id: string) {
    return this.http.get<Memory>("/api/memories/" + encodeURIComponent(id));
  }
  map() {
    return this.http.get<Memory[]>("/api/map");
  }
  timeline() {
    return this.http.get<Memory[]>("/api/timeline");
  }
  cityHistory() {
    return this.http.get<CityHistoryEntry[]>("/api/city-history");
  }
  saveCityHistory(payload: CityHistoryInput, id: number | null) {
    return this.csrf().pipe(switchMap(() => id === null
      ? this.http.post("/api/admin/city-history", payload)
      : this.http.put("/api/admin/city-history/" + id, payload)));
  }
  deleteCityHistory(id: number) {
    return this.csrf().pipe(switchMap(() => this.http.delete("/api/admin/city-history/" + id)));
  }
  contribute(payload: FormData, admin = false) {
    const path = admin ? "/api/admin/drafts" : "/api/contributions";
    return this.csrf().pipe(
      switchMap(() =>
        this.http.post<{ id: number }>(path, payload, {
          observe: "events",
          reportProgress: true,
        }),
      ),
    );
  }
  login(credentials: { username: string; password: string }) {
    const body = new HttpParams({ fromObject: credentials });
    return this.csrf().pipe(
      switchMap(() => this.http.post<void>("/api/session", body)),
    );
  }
  session() {
    return this.http.get<{ name: string }>("/api/session");
  }
  logout() {
    return this.csrf().pipe(
      switchMap(() => this.http.post<void>("/api/logout", null)),
    );
  }
  dashboard() {
    return this.http.get<Dashboard>("/api/admin/dashboard");
  }
  contributions() {
    return this.http.get<Contribution[]>("/api/admin/contributions");
  }
  review(id: number, payload: object) {
    return this.csrf().pipe(
      switchMap(() =>
        this.http.patch<void>("/api/admin/contributions/" + id, payload),
      ),
    );
  }
  private csrf() {
    return this.http.get("/api/csrf");
  }
}
