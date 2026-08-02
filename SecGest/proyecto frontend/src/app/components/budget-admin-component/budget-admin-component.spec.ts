import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';

import { BudgetAdminComponent } from './budget-admin-component';
import { TaskService } from '../../service/task-service';
import { Task } from '../../model/task';

describe('BudgetAdminComponent', () => {
  let fixture: ComponentFixture<BudgetAdminComponent>;
  let component: BudgetAdminComponent;
  let taskServiceStub: { getPendingTasks: jasmine.Spy; budgetTask: jasmine.Spy };

  const pending: Task = {
    id: 3, categoryId: 1, categoryName: 'Agenda', title: 'Agendar reunion',
    status: 'PENDIENTE_REVISION', createdAt: '2026-01-01T00:00:00',
  };

  beforeEach(async () => {
    taskServiceStub = {
      getPendingTasks: jasmine.createSpy().and.returnValue(of([pending])),
      budgetTask: jasmine.createSpy(),
    };

    await TestBed.configureTestingModule({
      imports: [BudgetAdminComponent, FormsModule],
      providers: [{ provide: TaskService, useValue: taskServiceStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(BudgetAdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create y cargar las tareas pendientes', () => {
    expect(component).toBeTruthy();
    expect(component.tasks.length).toBe(1);
  });

  it('submitBudget muestra error si no hay precio', () => {
    component.priceInputs[3] = null;
    component.submitBudget(pending);

    expect(component.errorMessage).toContain('precio valido');
    expect(taskServiceStub.budgetTask).not.toHaveBeenCalled();
  });

  it('submitBudget llama a budgetTask con el precio indicado', () => {
    taskServiceStub.budgetTask.and.returnValue(of({ ...pending, status: 'PRESUPUESTADA', price: 20 }));
    component.priceInputs[3] = 20;

    component.submitBudget(pending);

    expect(taskServiceStub.budgetTask).toHaveBeenCalledWith(3, 20);
    expect(component.successMessage).toContain('Agendar reunion');
  });

  it('submitBudget muestra error si falla la peticion', () => {
    taskServiceStub.budgetTask.and.returnValue(throwError(() => ({ status: 500 })));
    component.priceInputs[3] = 20;

    component.submitBudget(pending);

    expect(component.errorMessage).toContain('No se pudo guardar');
  });
});
