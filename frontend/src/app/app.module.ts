import {BrowserModule} from '@angular/platform-browser';
import {NgModule} from '@angular/core';

import {AppComponent} from './app.component';
import {MatButtonModule, MatCardModule, MatExpansionModule, MatToolbarModule} from '@angular/material';
import {RouterModule} from "@angular/router";
import {StatusComponent} from './status/status.component';
import {APP_ROUTES} from "./app.routes";
import {HttpClientModule} from "@angular/common/http";
import { BroadcastComponent } from './broadcast/broadcast.component';
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {FlexLayoutModule} from "@angular/flex-layout";

@NgModule({
  declarations: [
    AppComponent,
    StatusComponent,
    BroadcastComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    HttpClientModule,
    RouterModule.forRoot(APP_ROUTES),
    FlexLayoutModule,
    MatToolbarModule,
    MatCardModule,
    MatExpansionModule,
    MatButtonModule
  ],
  providers: [],
  bootstrap: [AppComponent]
})
export class AppModule {
}
