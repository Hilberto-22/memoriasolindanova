import { ParamMap } from '@angular/router';
import { Facets } from '../../api';

export type Filters = {collection:string; category:string; type:string; year:string; from:string; to:string; place:string};
export const initialFilters: Filters = {collection:'',category:'',type:'',year:'',from:'',to:'',place:''};
export const emptyFacets: Facets = {types:[],categories:[],places:[]};

export function filtersOf(params: ParamMap): Filters {
  return {collection:params.get('collection') ?? '', category:params.get('category') ?? '', type:params.get('type') ?? '',
    year:params.get('year') ?? '', from:params.get('from') ?? '', to:params.get('to') ?? '', place:params.get('place') ?? ''};
}
