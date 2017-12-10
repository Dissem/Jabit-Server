import {Injectable} from '@angular/core';
import {Observable} from "rxjs/Observable";
import {HttpClient} from "@angular/common/http";
import {map} from "rxjs/operators";

@Injectable()
export class BackendService {

  private baseUrl = "http://localhost:8080";

  constructor(private http: HttpClient) {
  }

  getStatus(): Observable<string> {
    return this.http.get(`${this.baseUrl}/api/v1/status`).pipe(map(data => JSON.stringify(data, null, 2)));
  }

  getBroadcasts(address: string): Observable<Broadcasts> {
    return this.http.get<Broadcasts>(`${this.baseUrl}/api/v1/read/${address}`)
  }
}

export interface Sender {
  address: String;
  alias: String;
}

export interface Message {
  id: any;
  received: number;
  subject: string;
  body: string;
}

export interface Broadcasts {
  sender: Sender;
  messages: Message[]
}
