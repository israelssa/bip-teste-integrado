import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BeneficioService } from '../../services/beneficio.service';
import { Beneficio } from '../../interfaces/beneficio.interface';

@Component({
  selector: 'app-transferencia-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  providers: [DecimalPipe],
  templateUrl: './transferencia-form.component.html',
  styleUrls: ['./transferencia-form.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class TransferenciaFormComponent implements OnInit {
  transferenciaForm: FormGroup;
  beneficios: Beneficio[] = [];
  processando = false;
  resultadoTransferencia: any = null;
  verificacaoResultado: any = null;

  constructor(
    private fb: FormBuilder,
    private beneficioService: BeneficioService,
    private decimalPipe: DecimalPipe
  ) {
    this.transferenciaForm = this.fb.group({
      fromId: ['', Validators.required],
      toId: ['', Validators.required],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      lockType: ['optimistic', Validators.required]
    });
  }

  ngOnInit() {
    this.carregarBeneficios();
  }

  carregarBeneficios() {
    this.beneficioService.listarBeneficios().subscribe({
      next: (data) => {
        this.beneficios = data;
      },
      error: (error) => {
        console.error('Erro ao carregar benefícios:', error);
      }
    });
  }

  get beneficiosAtivos(): Beneficio[] {
    return this.beneficios.filter(b => b.ativo);
  }

  verificarTransferencia() {
    const fromId = this.transferenciaForm.get('fromId')?.value;
    const amount = this.transferenciaForm.get('amount')?.value;

    if (fromId && amount) {
      this.beneficioService.verificarTransferencia(fromId, amount).subscribe({
        next: (result) => {
          this.verificacaoResultado = result;
        },
        error: (error) => {
          this.verificacaoResultado = { transferenciaPossivel: false };
        }
      });
    }
  }

  onSubmit() {
    if (this.transferenciaForm.valid) {
      this.processando = true;
      this.resultadoTransferencia = null;
      this.verificacaoResultado = null;

      const { fromId, toId, amount, lockType } = this.transferenciaForm.value;

      let transferenciaObservable;

      switch (lockType) {
        case 'pessimistic':
          transferenciaObservable = this.beneficioService.transferirPessimistic(fromId, toId, amount);
          break;
        case 'mixed':
          transferenciaObservable = this.beneficioService.transferirMixed(fromId, toId, amount);
          break;
        default:
          transferenciaObservable = this.beneficioService.transferirOptimistic(fromId, toId, amount);
      }

      transferenciaObservable.subscribe((result) => {
          console.log('✅ transferenciaObservable.subscribe: ', result);
          this.resultadoTransferencia = result;
          this.processando = false;
          console.log('✅ this.processando: ', this.processando);
          alert("Transferência realizada com sucesso! Atualize a Lista.");
          this.carregarBeneficios();
        },
        error => {
          this.resultadoTransferencia = {
            success: false,
            message: error.error?.message || 'Erro ao processar transferência'
          };
          this.processando = false;
        }
      );
    }
  }

  formatarNumero(valor: number): string {
    return this.decimalPipe.transform(valor, '1.2-2') || '0.00';
  }
}