export interface Beneficio {
  id?: number;
  nome: string;
  descricao: string;
  valor: number;
  ativo: boolean;
  version?: number;
}

// Interface para ResponseEntity do Spring
export interface ResponseEntity<T> {
  body: T;
  headers: any;
  statusCode: string;
  statusCodeValue: number;
}

export interface TransferenciaRequest {
  fromId: number;
  toId: number;
  amount: number;
}

export interface TransferenciaResponse {
  toId: number;
  amount: number;
  success: boolean;
  lockType: string;
  message: string;
  fromId: number;
  timestamp: string;
}

export interface SaldoResponse {
  success: boolean;
  beneficioId: number;
  saldo: number;
  timestamp: string;
}

export interface VersaoResponse {
  success: boolean;
  beneficioId: number;
  versao: number;
  timestamp: string;
}

export interface ConflitoResponse {
  success: boolean;
  beneficioId: number;
  versaoInformada: number;
  temConflito: boolean;
  timestamp: string;
}