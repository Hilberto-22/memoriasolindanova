import { Injectable, signal } from '@angular/core';

function storedIds(): number[] {
  try {
    const value: unknown = JSON.parse(localStorage.getItem('memorias-favorites') ?? '[]');
    return Array.isArray(value) ? value.filter((item): item is number => Number.isSafeInteger(item)) : [];
  } catch { return []; }
}

@Injectable({providedIn:'root'})
export class Favorites {
  readonly ids = signal<number[]>(storedIds());
  toggle(id:number) {
    this.ids.update(items => items.includes(id) ? items.filter(item => item !== id) : [...items,id]);
    try { localStorage.setItem('memorias-favorites',JSON.stringify(this.ids())); } catch { /* Favoritos continuam disponíveis nesta sessão. */ }
  }
}
