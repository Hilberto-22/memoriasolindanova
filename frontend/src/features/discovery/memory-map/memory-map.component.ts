import { AfterViewInit, Component, effect, ElementRef, input, OnDestroy, signal, viewChild } from '@angular/core';
import { Memory } from '../../../api';
import { ErrorState } from '../../../shared/components/error-state/error-state.component';
import { LeafletLayer, LeafletMap, Leaflet, loadLeaflet } from '../map/leaflet-loader';

@Component({
  selector:'memory-map', imports:[ErrorState],
  templateUrl: './memory-map.component.html'
})
export class MemoryMap implements AfterViewInit, OnDestroy {
  readonly items = input<Memory[]>([]);
  readonly preview = input(false);
  readonly failed = signal(false);
  private readonly canvas = viewChild.required<ElementRef<HTMLElement>>('canvas');
  private readonly library = signal<Leaflet | null>(null);
  private map?: LeafletMap;
  private layer?: LeafletLayer;
  private destroyed = false;
  constructor() {
    effect(() => {
      const library = this.library();
      const items = this.items();
      if (library && this.map && this.layer) this.renderMarkers(library, this.map, this.layer, items);
    });
  }
  ngAfterViewInit() { void this.initialize(); }
  async initialize() {
    this.failed.set(false);
    try {
      const library = await loadLeaflet();
      if (this.destroyed || this.map) return;
      const options = this.preview() ? {zoomControl:false, scrollWheelZoom:false, keyboard:false, dragging:false,
        doubleClickZoom:false, touchZoom:false, boxZoom:false} : {scrollWheelZoom:false};
      this.map = library.map(this.canvas().nativeElement, options).setView([-2.99, -44.99], 11);
      library.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom:19, attribution:'&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
      }).addTo(this.map);
      this.layer = library.layerGroup().addTo(this.map);
      this.library.set(library);
    } catch { if (!this.destroyed) this.failed.set(true); }
  }
  private renderMarkers(library: Leaflet, map: LeafletMap, layer: LeafletLayer, items: Memory[]) {
    layer.clearLayers();
    const points: [number, number][] = [];
    const places = new Map<string, {point: [number, number]; memories: Memory[]}>();
    for (const item of items) {
      if (item.latitude == null || item.longitude == null) continue;
      const point: [number, number] = [item.latitude, item.longitude];
      const key = point.join(',');
      const place = places.get(key) ?? {point, memories: []};
      place.memories.push(item);
      places.set(key, place);
    }
    for (const {point, memories} of places.values()) {
      const list = document.createElement('ul');
      for (const item of memories) {
        const row = document.createElement('li');
        const link = document.createElement('a');
        link.href = '/acervo/' + item.id;
        link.textContent = item.title;
        row.append(link);
        list.append(row);
      }
      library.circleMarker(point, {color:'#103C2B', fillColor:'#D9B56C', fillOpacity:1, radius:8})
        .bindPopup(list).addTo(layer);
      points.push(point);
    }
    if (points.length) map.fitBounds(points, {padding:[28,28], maxZoom:15});
  }
  ngOnDestroy() { this.destroyed = true; this.map?.remove(); }
}
