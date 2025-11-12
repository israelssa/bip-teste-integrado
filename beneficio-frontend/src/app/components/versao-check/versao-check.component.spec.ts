import { ComponentFixture, TestBed } from '@angular/core/testing';

import { VersaoCheckComponent } from './versao-check.component';

describe('VersaoCheckComponent', () => {
  let component: VersaoCheckComponent;
  let fixture: ComponentFixture<VersaoCheckComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [VersaoCheckComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(VersaoCheckComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
