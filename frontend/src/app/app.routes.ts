import {Route} from "@angular/router";
import {StatusComponent} from "./status/status.component";
import {BroadcastComponent} from "./broadcast/broadcast.component";

export const APP_ROUTES: Route[] = [
  {
    path: 'status',
    component: StatusComponent
  },
  {
    path: 'broadcasts/:address',
    component: BroadcastComponent
  },
  {path: '', redirectTo: 'status', pathMatch: 'full'}
];
