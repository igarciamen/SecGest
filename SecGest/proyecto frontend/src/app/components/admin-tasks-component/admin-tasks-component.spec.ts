import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { provideHttpClient } from '@angular/common/http';

import { AdminTasksComponent } from './admin-tasks-component';
import { TaskService } from '../../service/task-service';
import { Task } from '../../model/task';

describe('AdminTasksComponent', () => {
  let fixture: ComponentFixture<AdminTasksComponent>;
  let component: AdminTasksComponent;
  let taskServiceStub: { getAllTasksForAdmin: jasmine.Spy; deliverTask: jasmine.Spy };

  const paid: Task = {
    id: 6, categoryId: 1, categoryName: 'Agenda', title: 'Tarea pagada',
    status: 'PAGADA', createdAt: '2026-01-01T00:00:00', price: 30,
  };

  beforeEach(async () => {
    taskServiceStub = {
      getAllTasksForAdmin: jasmine.createSpy().and.returnValue(of([paid])),
      deliverTask: jasmine.createSpy(),
    };

    await TestBed.configureTestingModule({
      imports: [AdminTasksComponent, FormsModule],
      providers: [{ provide: TaskService, useValue: taskServiceStub }, provideHttpClient()],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminTasksComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create y cargar todas las tareas sin filtro', () => {
    expect(component).toBeTruthy();
    expect(taskServiceStub.getAllTasksForAdmin).toHaveBeenCalledWith(undefined);
    expect(component.tasks.length).toBe(1);
  });

  it('load con un filtro de estado se lo pasa al servicio', () => {
    component.statusFilter = 'ACEPTADA';
    component.load();

    expect(taskServiceStub.getAllTasksForAdmin).toHaveBeenCalledWith('ACEPTADA');
  });

  it('muestra un mensaje de error si falla la carga', () => {
    taskServiceStub.getAllTasksForAdmin.and.returnValue(throwError(() => ({ status: 500 })));

    component.load();

    expect(component.errorMessage).toContain('No se pudieron cargar');
    expect(component.loading).toBe(false);
  });

  it('deliver llama a deliverTask y recarga el listado', () => {
    taskServiceStub.deliverTask.and.returnValue(of({ ...paid, status: 'ENTREGADA' }));

    component.deliver(paid);

    expect(taskServiceStub.deliverTask).toHaveBeenCalledWith(6);
    expect(component.successMessage).toContain('Tarea pagada');
  });

  it('deliver muestra un error si falla', () => {
    taskServiceStub.deliverTask.and.returnValue(throwError(() => ({ status: 500 })));

    component.deliver(paid);

    expect(component.errorMessage).toContain('No se pudo marcar');
  });
});
