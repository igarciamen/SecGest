import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PaymentOkComponent } from './payment-ok-component';

describe('PaymentOkComponent', () => {
  let fixture: ComponentFixture<PaymentOkComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PaymentOkComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(PaymentOkComponent);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });
});
