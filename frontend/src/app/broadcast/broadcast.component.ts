import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {Observable} from "rxjs/Observable";
import {BackendService, Broadcasts} from "../backend.service";
import {map, switchMap} from "rxjs/operators";

@Component({
  selector: 'app-broadcast',
  templateUrl: './broadcast.component.html',
  styleUrls: ['./broadcast.component.scss']
})
export class BroadcastComponent implements OnInit {

  broadcasts$: Observable<Broadcasts>;

  constructor(private route: ActivatedRoute, private backend: BackendService) {
  }

  ngOnInit() {
    this.broadcasts$ = this.route.params
      .pipe(map(p => p['address']))
      .pipe(switchMap(address => this.backend.getBroadcasts(address)));
  }

}
