import { Routes } from '@angular/router';

import { QUERY_ROUTES } from './features/query/query.routes';

export const routes: Routes = [
  { path: '', children: QUERY_ROUTES },
];
