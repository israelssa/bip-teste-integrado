import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BeneficioService } from '../../services/beneficio.service';
import { Beneficio, TransferenciaResponse } from '../../interfaces/beneficio.interface';
import { ComunicacaoService } from '../../services/comunicacao.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-transferencia-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatInputModule,
    MatRadioModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './transferencia-form.component.html',
  styleUrls: ['./transferencia-form.component.css']
})
export class TransferenciaFormComponent implements OnInit, OnDestroy {
  transferenciaForm: FormGroup;
  beneficiosAtivos: Beneficio[] = [];
  processando = false;
  mensagemResultado: { 
    tipo: 'success' | 'error'; 
    texto: string; 
    detalhes?: any;
    response?: TransferenciaResponse; // Mudado de transferencias para response
  } | null = null;
  
  carregandoBeneficios = false;
  private subscription = new Subscription();

  // Mapeamento dos tipos de lock para métodos e descrições
  private lockStrategies = {
    optimistic: {
      method: 'transferirOptimistic',
      description: 'Optimistic Locking',
      endpoint: '/transferir'
    },
    pessimistic: {
      method: 'transferirPessimistic',
      description: 'Pessimistic Locking',
      endpoint: '/transferir/pessimistic'
    },
    mixed: {
      method: 'transferirMixed',
      description: 'Mixed Locking',
      endpoint: '/transferir/mixed'
    }
  };

  constructor(
    private fb: FormBuilder,
    private beneficioService: BeneficioService,
    private snackBar: MatSnackBar,
    private comunicacaoService: ComunicacaoService,
    private cdr: ChangeDetectorRef  // Adicionado ChangeDetectorRef
  ) {
    this.transferenciaForm = this.fb.group({
      fromId: [null, Validators.required],
      toId: [null, Validators.required],
      amount: [null, [Validators.required, Validators.min(0.01)]],
      lockType: ['optimistic', Validators.required]
    });
  }

  ngOnInit() {
    this.carregarBeneficiosAtivos();
    
    // Recarrega benefícios quando uma transferência é realizada
    this.subscription.add(
      this.comunicacaoService.transferenciaRealizada$.subscribe(() => {
        console.log('📋 Transferência detectada, recarregando benefícios...');
        this.carregarBeneficiosAtivos();
      })
    );

    // Validação personalizada para origem e destino diferentes
    this.subscription.add(
      this.transferenciaForm.get('fromId')?.valueChanges.subscribe(() => {
        this.validarOrigemDestino();
      })
    );

    this.subscription.add(
      this.transferenciaForm.get('toId')?.valueChanges.subscribe(() => {
        this.validarOrigemDestino();
      })
    );
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }

  validarOrigemDestino() {
    const fromId = this.transferenciaForm.get('fromId')?.value;
    const toId = this.transferenciaForm.get('toId')?.value;
    
    if (fromId && toId && fromId === toId) {
      this.transferenciaForm.get('toId')?.setErrors({ mesmaOrigem: true });
    } else {
      const errors = this.transferenciaForm.get('toId')?.errors;
      if (errors) {
        delete errors['mesmaOrigem'];
        this.transferenciaForm.get('toId')?.setErrors(Object.keys(errors).length ? errors : null);
      }
    }
  }

  carregarBeneficiosAtivos() {
    this.carregandoBeneficios = true;
    
    this.beneficioService.listarBeneficios().subscribe({
      next: (beneficios) => {
        this.beneficiosAtivos = beneficios.filter(b => b.ativo);
        this.carregandoBeneficios = false;
        
        // Valida se os benefícios selecionados ainda existem
        this.validarCamposSelecionados();
        this.cdr.markForCheck(); // Corrigido: removido o ? e adicionado ponto
      },
      error: (error) => {
        console.error('Erro ao carregar benefícios:', error);
        this.carregandoBeneficios = false;
        this.mostrarNotificacao('Erro ao carregar benefícios ativos', 'error');
        this.cdr.markForCheck(); // Corrigido: adicionado markForCheck no erro também
      }
    });
  }

  validarCamposSelecionados() {
    const fromId = this.transferenciaForm.get('fromId')?.value;
    const toId = this.transferenciaForm.get('toId')?.value;
    
    if (fromId && !this.beneficiosAtivos.some(b => b.id === fromId)) {
      this.transferenciaForm.patchValue({ fromId: null });
    }
    
    if (toId && !this.beneficiosAtivos.some(b => b.id === toId)) {
      this.transferenciaForm.patchValue({ toId: null });
    }
  }

  onSubmit() {
    if (this.transferenciaForm.invalid) {
      this.marcarCamposComoTocados();
      this.mostrarNotificacao('Preencha todos os campos corretamente', 'error');
      return;
    }

    const formValue = this.transferenciaForm.value;
    
    // Validação adicional: origem e destino não podem ser iguais
    if (formValue.fromId === formValue.toId) {
      this.mostrarNotificacao('Origem e destino não podem ser o mesmo benefício', 'error');
      return;
    }

    this.processando = true;
    this.mensagemResultado = null;

    console.log('🔄 Iniciando transferência:', {
      fromId: formValue.fromId,
      toId: formValue.toId,
      amount: formValue.amount,
      lockType: formValue.lockType,
      strategy: this.lockStrategies[formValue.lockType as keyof typeof this.lockStrategies].description
    });

    // Seleciona o método apropriado baseado no tipo de lock
    let transferenciaObservable;
    
    switch(formValue.lockType) {
      case 'pessimistic':
        transferenciaObservable = this.beneficioService.transferirPessimistic(
          formValue.fromId,
          formValue.toId,
          formValue.amount
        );
        break;
      case 'mixed':
        transferenciaObservable = this.beneficioService.transferirMixed(
          formValue.fromId,
          formValue.toId,
          formValue.amount
        );
        break;
      case 'optimistic':
      default:
        transferenciaObservable = this.beneficioService.transferirOptimistic(
          formValue.fromId,
          formValue.toId,
          formValue.amount
        );
        break;
    }

    transferenciaObservable.subscribe({
      next: (response: TransferenciaResponse) => {
        console.log('✅ Transferência concluída:', response);
        
        // Atualiza a lista de benefícios
        this.carregarBeneficiosAtivos();
        
        // Notifica outros componentes sobre a transferência
        this.comunicacaoService.notificarTransferenciaRealizada();
        
        // Prepara mensagem de sucesso com detalhes
        const strategyInfo = this.lockStrategies[formValue.lockType as keyof typeof this.lockStrategies];
        
        this.mensagemResultado = {
          tipo: 'success',
          texto: 'Transferência realizada com sucesso!',
          detalhes: {
            origem: this.getNomeBeneficio(formValue.fromId),
            destino: this.getNomeBeneficio(formValue.toId),
            valor: formValue.amount,
            strategy: strategyInfo.description,
            endpoint: strategyInfo.endpoint,
            timestamp: new Date().toLocaleString()
          },
          response: response // Inclui a resposta completa
        };
        
        this.mostrarNotificacao('Transferência realizada com sucesso!', 'success');
        
        // Opcional: Limpa os campos após sucesso
        // this.transferenciaForm.patchValue({
        //   fromId: null,
        //   toId: null,
        //   amount: null
        // });
        
        this.processando = false;
        this.cdr.markForCheck();
      },
      error: (error) => {
        console.error('❌ Erro na transferência:', error);
        
        let mensagemErro = 'Erro ao realizar transferência';
        let detalhesErro = error.error || error;
        
        // Trata diferentes tipos de erro baseado no status code
        if (error.status === 409) {
          mensagemErro = 'Conflito de versão (Optimistic Lock). Outra operação modificou o benefício. Tente novamente.';
        } else if (error.status === 400) {
          mensagemErro = error.error?.message || 'Dados inválidos para transferência';
        } else if (error.status === 404) {
          mensagemErro = 'Benefício não encontrado';
        } else if (error.status === 422) {
          mensagemErro = 'Saldo insuficiente para transferência';
        } else if (error.status === 500) {
          mensagemErro = 'Erro interno do servidor ao processar transferência';
        }
        
        this.mensagemResultado = {
          tipo: 'error',
          texto: mensagemErro,
          detalhes: {
            message: detalhesErro.message || detalhesErro,
            status: error.status,
            statusText: error.statusText,
            lockType: formValue.lockType,
            strategy: this.lockStrategies[formValue.lockType as keyof typeof this.lockStrategies]?.description
          }
        };
        
        this.mostrarNotificacao(mensagemErro, 'error');
        this.processando = false;
        
        // Recarrega benefícios para garantir dados atualizados
        this.carregarBeneficiosAtivos();
        this.cdr.markForCheck();
      }
    });
  }

  getNomeBeneficio(id: number): string {
    const beneficio = this.beneficiosAtivos.find(b => b.id === id);
    return beneficio ? beneficio.nome : id.toString();
  }

  marcarCamposComoTocados() {
    Object.keys(this.transferenciaForm.controls).forEach(key => {
      const control = this.transferenciaForm.get(key);
      control?.markAsTouched();
    });
  }

  mostrarNotificacao(mensagem: string, tipo: 'success' | 'error') {
    this.snackBar.open(mensagem, 'Fechar', {
      duration: 5000,
      panelClass: tipo === 'success' ? ['snackbar-success'] : ['snackbar-error'],
      horizontalPosition: 'center',
      verticalPosition: 'top'
    });
  }

  formatarNumero(valor: number): string {
    return valor.toFixed(2).replace('.', ',');
  }

  limparMensagem() {
    this.mensagemResultado = null;
    this.cdr.markForCheck();
  }

  // Método para testar cada tipo de lock
  testarLockType(tipo: 'optimistic' | 'pessimistic' | 'mixed') {
    this.transferenciaForm.patchValue({ lockType: tipo });
    console.log(`🔧 Testando ${this.lockStrategies[tipo].description}`);
  }

  limparFormulario(): void {
    this.transferenciaForm.reset({
      fromId: null,
      toId: null,
      amount: null,
      lockType: 'optimistic'
    });
    this.limparMensagem();
    this.mostrarNotificacao('Formulário limpo!', 'success');
  }

}