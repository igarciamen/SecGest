import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { AdminCalendarComponent } from './admin-calendar-component';
import { TaskService } from '../../service/task-service';
import { Task } from '../../model/task';

describe('AdminCalendarComponent', () => {
  let fixture: ComponentFixture<AdminCalendarComponent>;
  let component: AdminCalendarComponent;
  let taskServiceStub: { getCalendarTasks: jasmine.Spy };

  beforeEach(async () => {
    taskServiceStub = {
      getCalendarTasks: jasmine.createSpy().and.returnValue(of([])),
    };

    await TestBed.configureTestingModule({
      imports: [AdminCalendarComponent],
      providers: [{ provide: TaskService, useValue: taskServiceStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminCalendarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create y cargar el mes actual al iniciar', () => {
    expect(component).toBeTruthy();
    expect(taskServiceStub.getCalendarTasks).toHaveBeenCalled();
    expect(component.days.length).toBe(42); // 6 semanas x 7 dias
  });

  it('previousMonth retrocede un mes y recarga', () => {
    const initialMonth = component.currentMonth.getMonth();
    taskServiceStub.getCalendarTasks.calls.reset();

    component.previousMonth();

    const expectedMonth = (initialMonth + 11) % 12;
    expect(component.currentMonth.getMonth()).toBe(expectedMonth);
    expect(taskServiceStub.getCalendarTasks).toHaveBeenCalled();
  });

  it('nextMonth avanza un mes y recarga', () => {
    const initialMonth = component.currentMonth.getMonth();
    taskServiceStub.getCalendarTasks.calls.reset();

    component.nextMonth();

    const expectedMonth = (initialMonth + 1) % 12;
    expect(component.currentMonth.getMonth()).toBe(expectedMonth);
    expect(taskServiceStub.getCalendarTasks).toHaveBeenCalled();
  });

  it('load muestra un mensaje de error si falla', () => {
    taskServiceStub.getCalendarTasks.and.returnValue(throwError(() => ({ status: 500 })));

    component.load();

    expect(component.errorMessage).toContain('No se pudieron cargar');
  });


  it('taskUrgencyClass marca en rojo una tarea vencida sin cerrar', () => {
  const overdue: Task = {
    id: 1, categoryId: 1, categoryName: 'Agenda', title: 'Vencida',
    status: 'ACEPTADA', createdAt: '2026-01-01T00:00:00',
    dueDate: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(),
  };

  expect(component.taskUrgencyClass(overdue)).toContain('cal-chip');
  expect(component.taskUrgencyClass(overdue)).toContain('overdue');
});

it('taskUrgencyClass marca en ambar una tarea que vence en menos de 3 dias', () => {
  const soon: Task = {
    id: 2, categoryId: 1, categoryName: 'Agenda', title: 'Proxima',
    status: 'ACEPTADA', createdAt: '2026-01-01T00:00:00',
    dueDate: new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString(),
  };

  expect(component.taskUrgencyClass(soon)).toContain('cal-chip');
  expect(component.taskUrgencyClass(soon)).toContain('soon');
});

it('taskUrgencyClass marca en verde una tarea completada, aunque este vencida', () => {
  const completed: Task = {
    id: 3, categoryId: 1, categoryName: 'Agenda', title: 'Completada',
    status: 'COMPLETADA', createdAt: '2026-01-01T00:00:00',
    dueDate: new Date(Date.now() - 10 * 24 * 60 * 60 * 1000).toISOString(),
  };

  expect(component.taskUrgencyClass(completed)).toContain('cal-chip');
  expect(component.taskUrgencyClass(completed)).toContain('done');
});

it('taskUrgencyClass devuelve un color neutro para una tarea lejana', () => {
  const farAway: Task = {
    id: 4, categoryId: 1, categoryName: 'Agenda', title: 'Lejana',
    status: 'ACEPTADA', createdAt: '2026-01-01T00:00:00',
    dueDate: new Date(Date.now() + 20 * 24 * 60 * 60 * 1000).toISOString(),
  };

  expect(component.taskUrgencyClass(farAway)).toContain('cal-chip');
  expect(component.taskUrgencyClass(farAway)).toContain('neutral');
});


});