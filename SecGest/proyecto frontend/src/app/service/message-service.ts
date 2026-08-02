import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { InternalNoteItem, MessageItem, ThreadDto } from '../model/message';

@Injectable({
  providedIn: 'root',
})
export class MessageService {
  // Microservicio "messages" (puerto 8086).
  private messagesUrl = 'http://localhost:8086/api/messages';
  private notesUrl = 'http://localhost:8086/api/notes';

  constructor(private http: HttpClient) {}

  getThread(taskId: number): Observable<ThreadDto> {
    return this.http.get<ThreadDto>(`${this.messagesUrl}/tasks/${taskId}`);
  }

  sendMessage(taskId: number, content: string): Observable<MessageItem> {
    return this.http.post<MessageItem>(`${this.messagesUrl}/tasks/${taskId}`, { content });
  }

  deleteMessage(taskId: number, messageId: number): Observable<void> {
    return this.http.delete<void>(`${this.messagesUrl}/tasks/${taskId}/${messageId}`);
  }

  markAsRead(taskId: number): Observable<{ updated: number }> {
    return this.http.put<{ updated: number }>(`${this.messagesUrl}/tasks/${taskId}/read`, {});
  }

  getUnreadCount(taskId: number): Observable<{ count: number }> {
    return this.http.get<{ count: number }>(`${this.messagesUrl}/tasks/${taskId}/unread-count`);
  }

  // Notas internas: solo el admin llega a usar estos metodos (la propia UI no
  // los ofrece a un cliente, y el backend los rechazaria de todas formas).
  getNotes(taskId: number): Observable<InternalNoteItem[]> {
    return this.http.get<InternalNoteItem[]>(`${this.notesUrl}/tasks/${taskId}`);
  }

  addNote(taskId: number, content: string): Observable<InternalNoteItem> {
    return this.http.post<InternalNoteItem>(`${this.notesUrl}/tasks/${taskId}`, { content });
  }
}
