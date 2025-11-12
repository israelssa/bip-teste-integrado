import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule, DecimalPipe, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { BeneficioService } from '../../services/beneficio.service';

@Component({
  selector: 'app-saldo-display',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  providers: [DecimalPipe, DatePipe], // ✅ Adicione os pipes como providers
  templateUrl: './saldo-display.component.html',
  styleUrls: ['./saldo-display.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SaldoDisplayComponent {
  consultaForm: FormGroup;
  carregando = false;
  resultadoConsulta: any = null;
  historicoConsultas: any[] = [];

  constructor(
    private fb: FormBuilder,
    private beneficioService: BeneficioService,
    private decimalPipe: DecimalPipe,
    private datePipe: DatePipe
  ) {
    this.consultaForm = this.fb.group({
      beneficioId: ['', Validators.required]
    });
  }

  consultarSaldo() {
    if (this.consultaForm.valid) {
      this.carregando = true;
      const beneficioId = this.consultaForm.get('beneficioId')?.value;

      this.beneficioService.consultarSaldo(beneficioId).subscribe({
        next: (result) => {
          this.resultadoConsulta = result;
          this.carregando = false;
          
          if (result.success) {
            this.historicoConsultas.unshift({
              beneficioId: result.beneficioId,
              saldo: result.saldo,
              timestamp: new Date(result.timestamp)
            });
            
            if (this.historicoConsultas.length > 5) {
              this.historicoConsultas = this.historicoConsultas.slice(0, 5);
            }
          }
        },
        error: (error) => {
          this.resultadoConsulta = {
            success: false,
            message: error.error?.message || 'Erro ao consultar saldo'
          };
          this.carregando = false;
        }
      });
    }
  }

  // Método auxiliar para formatar números
  formatarNumero(valor: number): string {
    return this.decimalPipe.transform(valor, '1.2-2') || '0.00';
  }

  // Método auxiliar para formatar datas
  formatarData(data: string): string {
    return this.datePipe.transform(data, 'dd/MM/yyyy HH:mm:ss') || '';
  }

  formatarHora(data: string): string {
    return this.datePipe.transform(data, 'HH:mm:ss') || '';
  }
}