import { ChangeDetectionStrategy, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BeneficioListComponent } from './components/beneficio-list/beneficio-list.component';
import { TransferenciaFormComponent } from './components/transferencia-form/transferencia-form.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    BeneficioListComponent,
    TransferenciaFormComponent
  ],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent {
  title = 'beneficio-frontend';
}