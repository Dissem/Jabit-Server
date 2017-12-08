import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from "@angular/router";
import {HttpClient} from "@angular/common/http";
import {Observable} from "rxjs/Observable";

@Component({
  selector: 'app-broadcast',
  templateUrl: './broadcast.component.html',
  styleUrls: ['./broadcast.component.scss']
})
export class BroadcastComponent implements OnInit {

  broadcasts$: Observable<Broadcasts>;

  constructor(private route: ActivatedRoute, private http: HttpClient) {
  }

  ngOnInit() {
    let address = this.route.snapshot.params['address'];
    this.broadcasts$ = this.http.get<Broadcasts>('http://localhost:8080/read/' + address)
  }

}

class Sender {
  address: String;
  alias: String;
}

class Message {
  id: any;
  received: number;
  subject: string;
  body: string;
}

class Broadcasts {
  sender: Sender;
  messages: Message[]
}
