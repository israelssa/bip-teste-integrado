import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BeneficioListComponent } from './beneficio-list.component';

describe('BeneficioListComponent', () => {
  let component: BeneficioListComponent;
  let fixture: ComponentFixture<BeneficioListComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BeneficioListComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BeneficioListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
