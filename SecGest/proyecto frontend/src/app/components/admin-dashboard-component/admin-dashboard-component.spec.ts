import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AdminDashboardComponent } from './admin-dashboard-component';
import { TaskService } from '../../service/task-service';
import { Metrics } from '../../model/metrics';

describe('AdminDashboardComponent', () => {
  let fixture: ComponentFixture<AdminDashboardComponent>;
  let component: AdminDashboardComponent;
  let taskServiceStub: { getMetrics: jasmine.Spy };

  const metrics: Metrics = {
    tasksByStatus: { PAGADA: 2, COMPLETADA: 1, PENDIENTE_REVISION: 3 },
    totalRevenue: 65,
    upcomingDueCount: 1,
    averageRating: 4.5,
  };

  beforeEach(async () => {
    taskServiceStub = {
      getMetrics: jasmine.createSpy().and.returnValue(of(metrics)),
    };

    await TestBed.configureTestingModule({
      imports: [AdminDashboardComponent],
      providers: [{ provide: TaskService, useValue: taskServiceStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminDashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create y cargar las metricas al iniciar', () => {
    expect(component).toBeTruthy();
    expect(taskServiceStub.getMetrics).toHaveBeenCalled();
    expect(component.metrics).toEqual(metrics);
  });

  it('totalTasks suma todos los estados', () => {
    expect(component.totalTasks).toBe(6); // 2 + 1 + 3
  });

  it('countFor devuelve el conteo de un estado concreto, o 0 si no existe', () => {
    expect(component.countFor('PAGADA')).toBe(2);
    expect(component.countFor('RECHAZADA')).toBe(0);
  });

  it('muestra un mensaje de error si falla la carga', () => {
    taskServiceStub.getMetrics.and.returnValue(throwError(() => ({ status: 500 })));

    component.ngOnInit();

    expect(component.errorMessage).toContain('No se pudieron cargar');
    expect(component.loading).toBe(false);
  });
});