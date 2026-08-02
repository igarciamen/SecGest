import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PaymentKoComponent } from './payment-ko-component';

describe('PaymentKoComponent', () => {
  let fixture: ComponentFixture<PaymentKoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentKoComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentKoComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
