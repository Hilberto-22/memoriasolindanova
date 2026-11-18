import { Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Collection, Count, Facets } from '../../../api';
import { PortalIcon } from '../../../shared/components/portal-icon/portal-icon.component';
import { materialTypes } from '../../../shared/material-types';
import { emptyFacets, Filters, initialFilters } from '../catalog-filters';

@Component({
  selector:'filter-panel', imports:[ReactiveFormsModule, PortalIcon],
  templateUrl:'./filter-panel.component.html'
})
export class FilterPanel {
  readonly collections = input<Collection[]>([]);
  readonly facets = input<Facets>(emptyFacets);
  readonly counts = input<Count[]>([]);
  readonly changed = output<Filters>();
  readonly values = input<Filters>(initialFilters);
  readonly form = inject(FormBuilder).nonNullable.group(initialFilters);
  readonly types = materialTypes;
  readonly selected = signal<string[]>([]);
  readonly countsByType = computed(() => new Map(this.counts().map(item => [item.label,item.count])));
  readonly invalidPeriod = signal(false);
  expanded = typeof window !== 'undefined' && window.matchMedia('(min-width: 61rem)').matches;

  constructor() {
    effect(() => {
      this.form.reset(this.values(), {emitEvent:false});
      this.selected.set(this.values().type.split(',').filter(Boolean));
      this.invalidPeriod.set(false);
    });
  }

  toggle(type: string, checked: boolean) {
    this.selected.update(items => checked ? [...new Set([...items,type])] : items.filter(item => item !== type));
  }
  apply() {
    const values = this.form.getRawValue();
    const invalid = Boolean(values.from && values.to && Number(values.from) > Number(values.to));
    this.invalidPeriod.set(invalid);
    if (!invalid) this.changed.emit({...values,year:values.from || values.to ? '' : values.year,type:this.selected().join(',')});
  }
  clear() { this.form.reset(initialFilters); this.selected.set([]); this.apply(); }
}
