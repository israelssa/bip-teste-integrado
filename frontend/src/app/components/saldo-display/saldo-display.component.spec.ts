import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SaldoDisplayComponent } from './saldo-display.component';

describe('SaldoDisplayComponent', () => {
  let component: SaldoDisplayComponent;
  let fixture: ComponentFixture<SaldoDisplayComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SaldoDisplayComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SaldoDisplayComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
