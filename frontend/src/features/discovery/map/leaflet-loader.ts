export interface LeafletLayer { addTo(target: LeafletMap | LeafletLayer): LeafletLayer; bindPopup(node: HTMLElement): LeafletLayer; clearLayers(): void; }
export interface LeafletMap { setView(point: [number, number], zoom: number): LeafletMap; fitBounds(points: [number, number][], options: object): void; remove(): void; }
export interface Leaflet {
  map(node: HTMLElement, options?: object): LeafletMap;
  tileLayer(url: string, options: object): LeafletLayer;
  layerGroup(): LeafletLayer;
  circleMarker(point: [number, number], options: object): LeafletLayer;
}
let leafletLoader: Promise<Leaflet> | undefined;

export function loadLeaflet(): Promise<Leaflet> {
  if (leafletLoader) return leafletLoader;
  leafletLoader = new Promise<Leaflet>((resolve, reject) => {
    const stylesheet = document.createElement('link');
    stylesheet.rel = 'stylesheet';
    stylesheet.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
    stylesheet.integrity = 'sha256-p4NxAoJBhIIN+hmNHrzRCf9tD/miZyoHS5obTRR9BMY=';
    stylesheet.crossOrigin = 'anonymous';
    const script = document.createElement('script');
    script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
    script.integrity = 'sha256-20nQCchB9co0qIjJZRGuk2/Z9VM+kNiyxNV1lvTlZBo=';
    script.crossOrigin = 'anonymous';
    script.onload = () => {
      const library = (window as Window & {L?: Leaflet}).L;
      if (library) resolve(library);
      else { stylesheet.remove(); script.remove(); reject(new Error('Leaflet indisponível')); }
    };
    script.onerror = () => { stylesheet.remove(); script.remove(); reject(new Error('Leaflet indisponível')); };
    document.head.append(stylesheet, script);
  }).catch(error => { leafletLoader = undefined; throw error; });
  return leafletLoader;
}
