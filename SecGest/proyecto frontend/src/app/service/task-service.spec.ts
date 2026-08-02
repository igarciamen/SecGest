import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TaskService } from './task-service';
import { Task } from '../model/task';

describe('TaskService', () => {
  let service: TaskService;
  let httpMock: HttpTestingController;

  const tasksUrl = 'http://localhost:8082/api/tasks';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [TaskService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(TaskService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('createTask hace POST con el body correcto', (done) => {
    const created: Task = {
      id: 1, categoryId: 1, categoryName: 'Agenda', title: 'Agendar reunion',
      status: 'PENDIENTE_REVISION', createdAt: '2026-01-01T00:00:00',
    };

    service.createTask({ categoryId: 1, title: 'Agendar reunion' }).subscribe((task) => {
      expect(task.id).toBe(1);
      done();
    });

    const req = httpMock.expectOne(tasksUrl);
    expect(req.request.method).toBe('POST');
    req.flush(created);
  });

  it('getMyTasks hace GET a /mine', (done) => {
    service.getMyTasks().subscribe((tasks) => {
      expect(tasks.length).toBe(0);
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/mine`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getPendingTasks hace GET a /pending', (done) => {
    service.getPendingTasks().subscribe((tasks) => {
      expect(tasks.length).toBe(0);
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/pending`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('budgetTask hace PUT a /{id}/budget con el precio', (done) => {
    const budgeted: Task = {
      id: 5, categoryId: 1, categoryName: 'Agenda', title: 'Agendar reunion',
      status: 'PRESUPUESTADA', createdAt: '2026-01-01T00:00:00', price: 20,
    };

    service.budgetTask(5, 20).subscribe((task) => {
      expect(task.status).toBe('PRESUPUESTADA');
      expect(task.price).toBe(20);
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/5/budget`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ price: 20 });
    req.flush(budgeted);
  });

  it('acceptTask hace PUT a /{id}/accept', (done) => {
    const accepted: Task = {
      id: 5, categoryId: 1, categoryName: 'Agenda', title: 'Agendar reunion',
      status: 'ACEPTADA', createdAt: '2026-01-01T00:00:00', price: 20,
    };

    service.acceptTask(5).subscribe((task) => {
      expect(task.status).toBe('ACEPTADA');
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/5/accept`);
    expect(req.request.method).toBe('PUT');
    req.flush(accepted);
  });

  it('rejectTask hace PUT a /{id}/reject', (done) => {
    const rejected: Task = {
      id: 5, categoryId: 1, categoryName: 'Agenda', title: 'Agendar reunion',
      status: 'PENDIENTE_REVISION', createdAt: '2026-01-01T00:00:00',
    };

    service.rejectTask(5).subscribe((task) => {
      expect(task.status).toBe('PENDIENTE_REVISION');
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/5/reject`);
    expect(req.request.method).toBe('PUT');
    req.flush(rejected);
  });

  it('updateTask hace PUT a /{id} con los campos editados', (done) => {
    const updated: Task = {
      id: 5, categoryId: 1, categoryName: 'Agenda', title: 'Titulo editado',
      status: 'PENDIENTE_REVISION', createdAt: '2026-01-01T00:00:00',
    };

    service.updateTask(5, { categoryId: 1, title: 'Titulo editado' }).subscribe((task) => {
      expect(task.title).toBe('Titulo editado');
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/5`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ categoryId: 1, title: 'Titulo editado' });
    req.flush(updated);
  });

  it('payTask hace PUT a /{id}/pay', (done) => {
    const paid: Task = {
      id: 5, categoryId: 1, categoryName: 'Agenda', title: 'Agendar reunion',
      status: 'PAGADA', createdAt: '2026-01-01T00:00:00', price: 20,
    };

    service.payTask(5).subscribe((task) => {
      expect(task.status).toBe('PAGADA');
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/5/pay`);
    expect(req.request.method).toBe('PUT');
    req.flush(paid);
  });

  it('startRedsysPayment hace POST a /{id}/pay/redsys/start', (done) => {
    const formData = {
      actionUrl: 'https://sis-t.redsys.es:25443/sis/realizarPago',
      dsSignatureVersion: 'HMAC_SHA256_V1',
      dsMerchantParameters: 'params-base64',
      dsSignature: 'firma-base64',
    };

    service.startRedsysPayment(5).subscribe((form) => {
      expect(form.actionUrl).toContain('redsys.es');
      expect(form.dsSignature).toBe('firma-base64');
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/5/pay/redsys/start`);
    expect(req.request.method).toBe('POST');
    req.flush(formData);
  });

  it('getAllTasksForAdmin sin filtro hace GET a /all', (done) => {
    service.getAllTasksForAdmin().subscribe((tasks) => {
      expect(tasks.length).toBe(0);
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/all`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('getAllTasksForAdmin con filtro anade el query param status', (done) => {
    service.getAllTasksForAdmin('ACEPTADA').subscribe((tasks) => {
      expect(tasks.length).toBe(0);
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/all?status=ACEPTADA`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('deliverTask hace PUT a /{id}/deliver', (done) => {
    const delivered: Task = {
      id: 5, categoryId: 1, categoryName: 'Agenda', title: 'Agendar reunion',
      status: 'ENTREGADA', createdAt: '2026-01-01T00:00:00',
    };

    service.deliverTask(5).subscribe((task) => {
      expect(task.status).toBe('ENTREGADA');
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/5/deliver`);
    expect(req.request.method).toBe('PUT');
    req.flush(delivered);
  });

  it('completeTask hace PUT a /{id}/complete con la valoracion', (done) => {
    const completed: Task = {
      id: 5, categoryId: 1, categoryName: 'Agenda', title: 'Agendar reunion',
      status: 'COMPLETADA', createdAt: '2026-01-01T00:00:00', rating: 5,
    };

    service.completeTask(5, { rating: 5, ratingComment: 'Genial' }).subscribe((task) => {
      expect(task.status).toBe('COMPLETADA');
      done();
    });

    const req = httpMock.expectOne(`${tasksUrl}/5/complete`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ rating: 5, ratingComment: 'Genial' });
    req.flush(completed);
  });

  it('completeTask sin argumentos manda un body vacio', (done) => {
    const completed: Task = {
      id: 5, categoryId: 1, categoryName: 'Agenda', title: 'Agendar reunion',
      status: 'COMPLETADA', createdAt: '2026-01-01T00:00:00',
    };

    service.completeTask(5).subscribe(() => done());

    const req = httpMock.expectOne(`${tasksUrl}/5/complete`);
    expect(req.request.body).toEqual({});
    req.flush(completed);
  });


  it('getCalendarTasks hace GET a /calendar con from y to', (done) => {
  service.getCalendarTasks('2026-07-01T00:00:00', '2026-07-31T23:59:59').subscribe((tasks) => {
    expect(tasks.length).toBe(0);
    done();
  });

  const req = httpMock.expectOne(`${tasksUrl}/calendar?from=2026-07-01T00:00:00&to=2026-07-31T23:59:59`);
  expect(req.request.method).toBe('GET');
  req.flush([]);
});

it('getMetrics hace GET a /metrics', (done) => {
  const metrics = {
    tasksByStatus: { PAGADA: 2, COMPLETADA: 1 },
    totalRevenue: 65,
    upcomingDueCount: 1,
    averageRating: 4.5,
  };

  service.getMetrics().subscribe((res) => {
    expect(res.totalRevenue).toBe(65);
    expect(res.tasksByStatus['PAGADA']).toBe(2);
    done();
  });

  const req = httpMock.expectOne(`${tasksUrl}/metrics`);
  expect(req.request.method).toBe('GET');
  req.flush(metrics);
});
});
