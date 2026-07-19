import { Routes } from '@angular/router';

import { QueryComponent } from './query.component';

export const QUERY_ROUTES: Routes = [
  { path: '', component: QueryComponent },
  { path: '**', redirectTo: '' },
];
