import {Component} from '@angular/core';
import {BackendService} from "../backend.service";
import {Observable} from "rxjs/Observable";

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html',
  styleUrls: ['./status.component.scss']
})
export class StatusComponent {

  status$: Observable<string>;

  constructor(backend: BackendService) {
    this.status$ = backend.getStatus();
  }

}
