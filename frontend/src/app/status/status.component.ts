import {Component, OnInit} from '@angular/core';
import {HttpClient} from '@angular/common/http';

@Component({
  selector: 'app-status',
  templateUrl: './status.component.html',
  styleUrls: ['./status.component.scss']
})
export class StatusComponent implements OnInit {

  status: string;

  constructor(private http: HttpClient) {
  }

  ngOnInit() {
    this.http.get('http://localhost:8080/status').subscribe(data => {
      this.status = JSON.stringify(data, null, 2);
    });
  }

}
