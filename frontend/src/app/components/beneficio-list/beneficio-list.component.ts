import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BeneficioService } from '../../services/beneficio.service';
import { Beneficio } from '../../interfaces/beneficio.interface';
import { ComunicacaoService } from '../../services/comunicacao.service';

@Component({
  selector: 'app-beneficio-list',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './beneficio-list.component.html',
  styleUrls: ['./beneficio-list.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class BeneficioListComponent implements OnInit {
  beneficios: Beneficio[] = [];
  carregando = false;
  erro = '';
  displayedColumns: string[] = ['id', 'nome', 'descricao', 'saldo', 'status', 'versao'];

  constructor(
    private beneficioService: BeneficioService,
    private cdr: ChangeDetectorRef,
   private comunicacaoService: ComunicacaoService
  ) {}

  ngOnInit() {
     this.carregarBeneficios();
  
    // Atualiza a lista quando uma transferência é realizada
    this.comunicacaoService.transferenciaRealizada$.subscribe(() => {
      console.log('📋 Transferência detectada, atualizando lista...');
      this.carregarBeneficios();
    });
  }

  carregarBeneficios() {
    this.carregando = true;
    this.erro = '';
    
    console.log('🔄 Iniciando carregamento de benefícios...');

    this.beneficioService.listarBeneficios().subscribe({
      next: (beneficios: Beneficio[]) => {
        this.beneficios = beneficios;
        this.carregando = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        this.erro = `Erro ao carregar benefícios: ${error.message}`;
        this.carregando = false;
        this.cdr.markForCheck();
      }
    });
  }

  // Método para debug
  debugRequisicao() {
    this.beneficioService.debugListarBeneficios();
  }

  get beneficiosAtivos(): number {
    return this.beneficios.filter(b => b.ativo).length;
  }

  get beneficiosInativos(): number {
    return this.beneficios.filter(b => !b.ativo).length;
  }
}