import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { 
  Beneficio, 
  ResponseEntity,
  TransferenciaResponse, 
  SaldoResponse, 
  VersaoResponse, 
  ConflitoResponse 
} from '../interfaces/beneficio.interface';

@Injectable({
  providedIn: 'root'
})
export class BeneficioService {
  private apiUrl = 'http://localhost:8080/api/v1/beneficios';

  constructor(private http: HttpClient) { }

  /**
   * Requisição para o endpoint: GET /api/v1/beneficios
   * Retorna: ResponseEntity<List<Beneficio>>
   */
  listarBeneficios(): Observable<Beneficio[]> {
    return this.http.get<Beneficio[]>(this.apiUrl)
      .pipe(
        map((response: Beneficio[]) => {
          return response || [];
        })
      );
  }

  /**
   * Requisição alternativa que retorna a ResponseEntity completa
   * Útil se você precisar acessar headers ou status code
   */
  listarBeneficiosCompleto(): Observable<ResponseEntity<Beneficio[]>> {
    return this.http.get<ResponseEntity<Beneficio[]>>(this.apiUrl);
  }

  /**
   * Transferência com OPTIMISTIC LOCKING
   */
  transferirOptimistic(fromId: number, toId: number, amount: number): Observable<TransferenciaResponse> {
    const params = new HttpParams()
      .set('fromId', fromId.toString())
      .set('toId', toId.toString())
      .set('amount', amount.toString());

    return this.http.post<TransferenciaResponse>(`${this.apiUrl}/transferir`, null, { params })
      .pipe(
        map((response: TransferenciaResponse) => {
          return response || [];
        })
      );
  }

  /**
   * Transferência com PESSIMISTIC LOCKING
   */
  transferirPessimistic(fromId: number, toId: number, amount: number): Observable<TransferenciaResponse> {
    const params = new HttpParams()
      .set('fromId', fromId.toString())
      .set('toId', toId.toString())
      .set('amount', amount.toString());
    
    return this.http.post<TransferenciaResponse>(`${this.apiUrl}/transferir/pessimistic`, null, { params });
  }

  /**
   * Transferência com MIXED LOCKING
   */
  transferirMixed(fromId: number, toId: number, amount: number): Observable<TransferenciaResponse> {
    const params = new HttpParams()
      .set('fromId', fromId.toString())
      .set('toId', toId.toString())
      .set('amount', amount.toString());
    
    return this.http.post<TransferenciaResponse>(`${this.apiUrl}/transferir/mixed`, null, { params });
  }

  /**
   * Consultar saldo de um benefício
   */
  consultarSaldo(beneficioId: number): Observable<SaldoResponse> {
    return this.http.get<SaldoResponse>(`${this.apiUrl}/${beneficioId}/saldo`);
  }

  /**
   * Obter versão atual de um benefício
   */
  obterVersao(beneficioId: number): Observable<VersaoResponse> {
    return this.http.get<VersaoResponse>(`${this.apiUrl}/${beneficioId}/versao`);
  }

  /**
   * Verificar conflito de versão
   */
  verificarConflito(beneficioId: number, versao: number): Observable<ConflitoResponse> {
    const params = new HttpParams()
      .set('beneficioId', beneficioId.toString())
      .set('versao', versao.toString());
    
    return this.http.get<ConflitoResponse>(`${this.apiUrl}/verificar-conflito`, { params });
  }

  /**
   * Verificar se transferência é possível
   */
  verificarTransferencia(fromId: number, amount: number): Observable<any> {
    const params = new HttpParams()
      .set('fromId', fromId.toString())
      .set('amount', amount.toString());
    
    return this.http.get(`${this.apiUrl}/verificar-transferencia`, { params });
  }

  /**
   * Método auxiliar para debug - mostra a estrutura completa da resposta
   */
  debugListarBeneficios(): void {
    this.listarBeneficiosCompleto().subscribe({
      next: (response) => {
        console.log('🔍 DEBUG - Resposta completa:', response);
        console.log('📊 Status Code:', response.statusCodeValue);
        console.log('📦 Body:', response.body);
        console.log('📋 Headers:', response.headers);
      },
      error: (error) => {
        console.error('❌ Erro na requisição:', error);
      }
    });
  }
}