import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ReactiveFormsModule } from '@angular/forms';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';

import { MyTasksComponent } from './my-tasks-component';
import { TaskService } from '../../service/task-service';
import { Task } from '../../model/task';

describe('MyTasksComponent', () => {
  let fixture: ComponentFixture<MyTasksComponent>;
  let component: MyTasksComponent;
  let taskServiceStub: {
    getMyTasks: jasmine.Spy;
    acceptTask: jasmine.Spy;
    rejectTask: jasmine.Spy;
    updateTask: jasmine.Spy;
    payTask: jasmine.Spy;
    startRedsysPayment: jasmine.Spy;
    completeTask: jasmine.Spy;
  };

  const budgeted: Task = {
    id: 3, categoryId: 1, categoryName: 'Agenda', title: 'Agendar reunion',
    status: 'PRESUPUESTADA', createdAt: '2026-01-01T00:00:00', price: 20,
  };
  const accepted: Task = {
    id: 6, categoryId: 1, categoryName: 'Agenda', title: 'Tarea aceptada',
    status: 'ACEPTADA', createdAt: '2026-01-01T00:00:00', price: 30,
  };
  const pending: Task = {
    id: 4, categoryId: 1, categoryName: 'Agenda', title: 'Otra tarea',
    status: 'PENDIENTE_REVISION', createdAt: '2026-01-01T00:00:00',
  };
  const delivered: Task = {
    id: 7, categoryId: 1, categoryName: 'Agenda', title: 'Tarea entregada',
    status: 'ENTREGADA', createdAt: '2026-01-01T00:00:00', price: 25,
  };

  beforeEach(async () => {
    taskServiceStub = {
      getMyTasks: jasmine.createSpy().and.returnValue(of([budgeted, pending, accepted, delivered])),
      acceptTask: jasmine.createSpy(),
      rejectTask: jasmine.createSpy(),
      updateTask: jasmine.createSpy(),
      payTask: jasmine.createSpy(),
      startRedsysPayment: jasmine.createSpy(),
      completeTask: jasmine.createSpy(),
    };

    await TestBed.configureTestingModule({
      imports: [MyTasksComponent, ReactiveFormsModule],
      providers: [{ provide: TaskService, useValue: taskServiceStub }, provideRouter([]), provideHttpClient()],
    }).compileComponents();

    fixture = TestBed.createComponent(MyTasksComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create y cargar las tareas', () => {
    expect(component).toBeTruthy();
    expect(component.tasks.length).toBe(4);
  });

  it('accept llama a acceptTask y recarga el listado', () => {
    taskServiceStub.acceptTask.and.returnValue(of({ ...budgeted, status: 'ACEPTADA' }));

    component.accept(budgeted);

    expect(taskServiceStub.acceptTask).toHaveBeenCalledWith(3);
    expect(component.successMessage).toContain('Agendar reunion');
  });

  it('accept muestra error si falla la peticion', () => {
    taskServiceStub.acceptTask.and.returnValue(throwError(() => ({ status: 500 })));

    component.accept(budgeted);

    expect(component.errorMessage).toContain('No se pudo aceptar');
  });

  it('reject llama a rejectTask y recarga el listado', () => {
    taskServiceStub.rejectTask.and.returnValue(of({ ...budgeted, status: 'PENDIENTE_REVISION', price: undefined }));

    component.reject(budgeted);

    expect(taskServiceStub.rejectTask).toHaveBeenCalledWith(3);
    expect(component.successMessage).toContain('rechazado');
  });

  it('startEdit rellena el formulario y saveEdit llama a updateTask', () => {
    taskServiceStub.updateTask.and.returnValue(of({ ...pending, title: 'Titulo nuevo' }));

    component.startEdit(pending);
    expect(component.editForm.value.title).toBe('Otra tarea');

    component.editForm.patchValue({ title: 'Titulo nuevo' });
    component.saveEdit(pending);

    expect(taskServiceStub.updateTask).toHaveBeenCalledWith(4, jasmine.objectContaining({ title: 'Titulo nuevo' }));
    expect(component.editingId).toBeNull();
  });

  it('saveEdit no llama al servicio si el formulario es invalido', () => {
    component.startEdit(pending);
    component.editForm.patchValue({ title: '' });

    component.saveEdit(pending);

    expect(taskServiceStub.updateTask).not.toHaveBeenCalled();
  });

  it('cancelEdit limpia el modo edicion', () => {
    component.startEdit(pending);
    expect(component.editingId).toBe(4);

    component.cancelEdit();

    expect(component.editingId).toBeNull();
  });

  it('pay llama a payTask y recarga el listado', () => {
    taskServiceStub.payTask.and.returnValue(of({ ...accepted, status: 'PAGADA' }));

    component.pay(accepted);

    expect(taskServiceStub.payTask).toHaveBeenCalledWith(6);
    expect(component.successMessage).toContain('Tarea aceptada');
  });

  it('pay muestra error si falla la peticion', () => {
    taskServiceStub.payTask.and.returnValue(throwError(() => ({ status: 500 })));

    component.pay(accepted);

    expect(component.errorMessage).toContain('No se pudo procesar el pago');
  });

  it('payWithRedsys construye y envia un formulario oculto a la pasarela', () => {
    // Interceptamos submit() para que el test no navegue de verdad fuera de la
    // pagina (estamos en un navegador real via Karma, no en un DOM simulado).
    const submitSpy = spyOn(HTMLFormElement.prototype, 'submit');

    taskServiceStub.startRedsysPayment.and.returnValue(of({
      actionUrl: 'https://sis-t.redsys.es:25443/sis/realizarPago',
      dsSignatureVersion: 'HMAC_SHA256_V1',
      dsMerchantParameters: 'params-base64',
      dsSignature: 'firma-base64',
    }));

    component.payWithRedsys(accepted);

    expect(taskServiceStub.startRedsysPayment).toHaveBeenCalledWith(6);
    expect(submitSpy).toHaveBeenCalled();

    const form = document.querySelector('form[action*="redsys.es"]') as HTMLFormElement;
    expect(form).toBeTruthy();
    expect((form.elements.namedItem('Ds_Signature') as HTMLInputElement).value).toBe('firma-base64');

    document.body.removeChild(form);
  });

  it('payWithRedsys muestra error si falla al iniciar el pago', () => {
    taskServiceStub.startRedsysPayment.and.returnValue(throwError(() => ({ status: 500 })));

    component.payWithRedsys(accepted);

    expect(component.errorMessage).toContain('No se pudo iniciar el pago');
  });

  it('startRating abre el formulario de valoracion en blanco', () => {
    component.startRating(delivered);

    expect(component.ratingTaskId).toBe(7);
    expect(component.selectedRating).toBe(0);
    expect(component.ratingComment).toBe('');
  });

  it('cancelRating cierra el formulario', () => {
    component.startRating(delivered);
    component.cancelRating();

    expect(component.ratingTaskId).toBeNull();
  });

  it('setRating guarda la puntuacion elegida', () => {
    component.setRating(4);

    expect(component.selectedRating).toBe(4);
  });

  it('confirmCompletion con estrellas elegidas manda rating y comentario', () => {
    taskServiceStub.completeTask.and.returnValue(of({ ...delivered, status: 'COMPLETADA', rating: 4 }));
    component.startRating(delivered);
    component.setRating(4);
    component.ratingComment = 'Muy bien';

    component.confirmCompletion(delivered);

    expect(taskServiceStub.completeTask).toHaveBeenCalledWith(7, { rating: 4, ratingComment: 'Muy bien' });
    expect(component.successMessage).toContain('completado');
    expect(component.ratingTaskId).toBeNull();
  });

  it('confirmCompletion sin estrellas confirma sin valorar', () => {
    taskServiceStub.completeTask.and.returnValue(of({ ...delivered, status: 'COMPLETADA' }));
    component.startRating(delivered);

    component.confirmCompletion(delivered);

    expect(taskServiceStub.completeTask).toHaveBeenCalledWith(7, undefined);
  });

  it('confirmCompletion muestra un error si falla la peticion', () => {
    taskServiceStub.completeTask.and.returnValue(throwError(() => ({ status: 500 })));
    component.startRating(delivered);

    component.confirmCompletion(delivered);

    expect(component.errorMessage).toContain('No se pudo confirmar');
  });
});
