import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { MessageService } from '../service/message-service';

import { InternalNoteItem, MessageItem, ThreadDto } from '../model/message';

describe('MessageService', () => {
  let service: MessageService;
  let httpMock: HttpTestingController;

  const messagesUrl = 'http://localhost:8086/api/messages';
  const notesUrl = 'http://localhost:8086/api/notes';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MessageService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(MessageService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getThread hace GET a /tasks/{taskId}', (done) => {
    const thread: ThreadDto = { taskId: 10, messages: [] };

    service.getThread(10).subscribe((res) => {
      expect(res.taskId).toBe(10);
      done();
    });

    const req = httpMock.expectOne(`${messagesUrl}/tasks/10`);
    expect(req.request.method).toBe('GET');
    req.flush(thread);
  });

  it('sendMessage hace POST con el contenido', (done) => {
    const created: MessageItem = {
      id: 1, senderId: 5, senderRole: 'ROLE_USER', content: 'Hola', createdAt: '2026-01-01T00:00:00',
    };

    service.sendMessage(10, 'Hola').subscribe((msg) => {
      expect(msg.content).toBe('Hola');
      done();
    });

    const req = httpMock.expectOne(`${messagesUrl}/tasks/10`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ content: 'Hola' });
    req.flush(created);
  });

  it('deleteMessage hace DELETE a /tasks/{taskId}/{messageId}', (done) => {
    service.deleteMessage(10, 1).subscribe(() => done());

    const req = httpMock.expectOne(`${messagesUrl}/tasks/10/1`);
    expect(req.request.method).toBe('DELETE');
    req.flush(null);
  });

  it('markAsRead hace PUT a /tasks/{taskId}/read', (done) => {
    service.markAsRead(10).subscribe((res) => {
      expect(res.updated).toBe(2);
      done();
    });

    const req = httpMock.expectOne(`${messagesUrl}/tasks/10/read`);
    expect(req.request.method).toBe('PUT');
    req.flush({ updated: 2 });
  });

  it('getUnreadCount hace GET a /tasks/{taskId}/unread-count', (done) => {
    service.getUnreadCount(10).subscribe((res) => {
      expect(res.count).toBe(3);
      done();
    });

    const req = httpMock.expectOne(`${messagesUrl}/tasks/10/unread-count`);
    expect(req.request.method).toBe('GET');
    req.flush({ count: 3 });
  });

  it('getNotes hace GET a /notes/tasks/{taskId}', (done) => {
    service.getNotes(10).subscribe((notes) => {
      expect(notes.length).toBe(0);
      done();
    });

    const req = httpMock.expectOne(`${notesUrl}/tasks/10`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('addNote hace POST a /notes/tasks/{taskId}', (done) => {
    const created: InternalNoteItem = { id: 1, authorUserId: 1, content: 'Nota', createdAt: '2026-01-01T00:00:00' };

    service.addNote(10, 'Nota').subscribe((note) => {
      expect(note.content).toBe('Nota');
      done();
    });

    const req = httpMock.expectOne(`${notesUrl}/tasks/10`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ content: 'Nota' });
    req.flush(created);
  });
});
