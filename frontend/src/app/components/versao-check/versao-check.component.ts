import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { BeneficioService } from '../../services/beneficio.service';

@Component({
  selector: 'app-versao-check',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  templateUrl: './versao-check.component.html',
  styleUrls: ['./versao-check.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class VersaoCheckComponent {
  abaAtiva = 'versao';
  
  versaoForm: FormGroup;
  conflitoForm: FormGroup;
  
  carregandoVersao = false;
  carregandoConflito = false;
  
  resultadoVersao: any = null;
  resultadoConflito: any = null;

  constructor(
    private fb: FormBuilder,
    private beneficioService: BeneficioService
  ) {
    this.versaoForm = this.fb.group({
      beneficioId: ['', Validators.required]
    });

    this.conflitoForm = this.fb.group({
      beneficioId: ['', Validators.required],
      versao: ['', Validators.required]
    });
  }

  obterVersao() {
    if (this.versaoForm.valid) {
      this.carregandoVersao = true;
      const beneficioId = this.versaoForm.get('beneficioId')?.value;

      this.beneficioService.obterVersao(beneficioId).subscribe({
        next: (result) => {
          this.resultadoVersao = result;
          this.carregandoVersao = false;
        },
        error: (error) => {
          this.resultadoVersao = {
            success: false,
            message: error.error?.message || 'Erro ao obter versão'
          };
          this.carregandoVersao = false;
        }
      });
    }
  }

  verificarConflito() {
    if (this.conflitoForm.valid) {
      this.carregandoConflito = true;
      const beneficioId = this.conflitoForm.get('beneficioId')?.value;
      const versao = this.conflitoForm.get('versao')?.value;

      this.beneficioService.verificarConflito(beneficioId, versao).subscribe({
        next: (result) => {
          this.resultadoConflito = result;
          this.carregandoConflito = false;
        },
        error: (error) => {
          this.resultadoConflito = {
            success: false,
            message: error.error?.message || 'Erro ao verificar conflito'
          };
          this.carregandoConflito = false;
        }
      });
    }
  }
}