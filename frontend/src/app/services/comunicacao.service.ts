import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ComunicacaoService {
  private transferenciaRealizadaSource = new Subject<void>();
  
  transferenciaRealizada$ = this.transferenciaRealizadaSource.asObservable();
  
  notificarTransferenciaRealizada() {
    this.transferenciaRealizadaSource.next();
  }
}