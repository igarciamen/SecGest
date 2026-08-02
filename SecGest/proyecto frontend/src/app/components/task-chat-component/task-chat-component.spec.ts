import { ComponentFixture, TestBed, fakeAsync, tick, discardPeriodicTasks } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { TaskChatComponent } from './task-chat-component';
import { MessageService } from '../../service/message-service';
import { AuthService } from '../../service/auth-service';
import { MessageItem, ThreadDto } from '../../model/message';

describe('TaskChatComponent', () => {
  let fixture: ComponentFixture<TaskChatComponent>;
  let component: TaskChatComponent;
  let messageServiceStub: {
    getThread: jasmine.Spy;
    sendMessage: jasmine.Spy;
    deleteMessage: jasmine.Spy;
    markAsRead: jasmine.Spy;
    getUnreadCount: jasmine.Spy;
  };
  let authServiceStub: { getUserId: jasmine.Spy };

  const emptyThread: ThreadDto = { taskId: 10, messages: [] };

  beforeEach(async () => {
    messageServiceStub = {
      getThread: jasmine.createSpy().and.returnValue(of(emptyThread)),
      sendMessage: jasmine.createSpy(),
      deleteMessage: jasmine.createSpy(),
      markAsRead: jasmine.createSpy().and.returnValue(of({ updated: 0 })),
      getUnreadCount: jasmine.createSpy().and.returnValue(of({ count: 2 })),
    };
    authServiceStub = {
      getUserId: jasmine.createSpy().and.returnValue(5),
    };

    await TestBed.configureTestingModule({
      imports: [TaskChatComponent],
      providers: [
        { provide: MessageService, useValue: messageServiceStub },
        { provide: AuthService, useValue: authServiceStub },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TaskChatComponent);
    component = fixture.componentInstance;
    component.taskId = 10;
    fixture.detectChanges();
  });

  it('should create y cargar el contador de no leidos al iniciar, sin desplegar', () => {
    expect(component).toBeTruthy();
    expect(messageServiceStub.getUnreadCount).toHaveBeenCalledWith(10);
    expect(component.unreadCount).toBe(2);
    expect(messageServiceStub.getThread).not.toHaveBeenCalled();
  });

  it('toggle despliega, carga el hilo y marca como leido', fakeAsync(() => {
    component.toggle();
    tick(0);

    expect(component.expanded).toBe(true);
    expect(messageServiceStub.getThread).toHaveBeenCalledWith(10);
    expect(messageServiceStub.markAsRead).toHaveBeenCalledWith(10);
    expect(component.unreadCount).toBe(0);

    discardPeriodicTasks();
  }));

  it('toggle otra vez colapsa y detiene el refresco', fakeAsync(() => {
    component.toggle();
    tick(0);
    component.toggle();

    expect(component.expanded).toBe(false);

    discardPeriodicTasks();
  }));

  it('send manda el mensaje y lo añade al hilo local', fakeAsync(() => {
    component.toggle();
    tick(0);

    const created: MessageItem = {
      id: 1, senderId: 5, senderRole: 'ROLE_USER', content: 'Hola', createdAt: '2026-01-01T00:00:00',
    };
    messageServiceStub.sendMessage.and.returnValue(of(created));
    component.newMessage = 'Hola';

    component.send();

    expect(messageServiceStub.sendMessage).toHaveBeenCalledWith(10, 'Hola');
    expect(component.thread?.messages.length).toBe(1);
    expect(component.newMessage).toBe('');

    discardPeriodicTasks();
  }));

  it('send no hace nada si el mensaje esta vacio', () => {
    component.newMessage = '   ';

    component.send();

    expect(messageServiceStub.sendMessage).not.toHaveBeenCalled();
  });

  it('remove borra un mensaje propio del hilo local', fakeAsync(() => {
    component.toggle();
    tick(0);
    component.thread = { taskId: 10, messages: [
      { id: 1, senderId: 5, senderRole: 'ROLE_USER', content: 'Hola', createdAt: '2026-01-01T00:00:00' },
    ] };
    messageServiceStub.deleteMessage.and.returnValue(of(undefined));

    component.remove(component.thread.messages[0]);

    expect(messageServiceStub.deleteMessage).toHaveBeenCalledWith(10, 1);
    expect(component.thread.messages.length).toBe(0);

    discardPeriodicTasks();
  }));

  it('isMine compara el senderId con el usuario actual', () => {
    const mine: MessageItem = { id: 1, senderId: 5, senderRole: 'ROLE_USER', content: 'x', createdAt: '2026-01-01T00:00:00' };
    const theirs: MessageItem = { id: 2, senderId: 1, senderRole: 'ROLE_ADMIN', content: 'y', createdAt: '2026-01-01T00:00:00' };

    expect(component.isMine(mine)).toBe(true);
    expect(component.isMine(theirs)).toBe(false);
  });
});
