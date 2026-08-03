import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { MapaOperacional } from '../models/mapa-operacional';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class MapaOperacionalApiService {
  private readonly http = inject(HttpClient);
  private readonly api = inject(ApiService);

  private get base(): string {
    return `${this.api.baseUrl}/v1/org`;
  }

  obter(data?: string): Observable<MapaOperacional> {
    let params = new HttpParams();
    if (data) {
      params = params.set('data', data);
    }
    return this.http.get<MapaOperacional>(`${this.base}/mapa-operacional`, { params });
  }
}
