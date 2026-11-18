import { Component, input } from '@angular/core';

@Component({selector:'empty-state', template:'<div class="state" role="status">{{message()}}</div>'})
export class EmptyState { readonly message = input('Nenhum registro encontrado.'); }
