import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subscription, interval, startWith, switchMap, catchError, EMPTY } from 'rxjs';
import { MessageService } from '../../service/message-service';
import { AuthService } from '../../service/auth-service';
import { MessageItem, ThreadDto } from '../../model/message';

// Componente reutilizable, mismo espiritu que TaskDocumentsComponent: se
// incrusta en la tarjeta de la tarea, tanto en "Mis tareas" como en "Todas las
// tareas" del admin. El contador de no leidos se carga ya al iniciar (para que
// el boton lo muestre sin desplegar nada); el hilo completo, y el refresco
// periodico, solo arrancan cuando el usuario despliega el chat.
@Component({
  selector: 'app-task-chat-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './task-chat-component.html',
  styleUrl: './task-chat-component.css',
})
export class TaskChatComponent implements OnInit, OnDestroy {
  @Input({ required: true }) taskId!: number;

  thread: ThreadDto | null = null;
  unreadCount = 0;
  newMessage = '';
  expanded = false;
  loading = false;
  errorMessage = '';

  private pollSub?: Subscription;
  private readonly pollMs = 5000;

  constructor(
    private messageService: MessageService,
    private auth: AuthService,
  ) {}

  ngOnInit(): void {
    this.loadUnreadCountSilently();
  }

  ngOnDestroy(): void {
    this.pollSub?.unsubscribe();
  }

  private loadUnreadCountSilently(): void {
    this.messageService.getUnreadCount(this.taskId).subscribe({
      next: (res) => (this.unreadCount = res.count),
      error: () => {}, // el contador no es critico, si falla simplemente se queda a 0
    });
  }

  toggle(): void {
    this.expanded = !this.expanded;
    if (this.expanded) {
      this.startPolling();
    } else {
      this.pollSub?.unsubscribe();
    }
  }

  private startPolling(): void {
    this.loading = true;
    this.pollSub = interval(this.pollMs)
      .pipe(
        startWith(0),
        switchMap(() =>
          this.messageService.getThread(this.taskId).pipe(
            catchError(() => {
              if (!this.thread) {
                this.errorMessage = 'No se pudo cargar la conversación.';
              }
              this.loading = false;
              return EMPTY;
            }),
          ),
        ),
      )
      .subscribe((thread) => {
        this.thread = thread;
        this.loading = false;
        this.errorMessage = '';
        // Al abrir/refrescar el chat, marcamos como leido lo del otro lado.
        this.messageService.markAsRead(this.taskId).subscribe({
          next: () => (this.unreadCount = 0),
          error: () => {},
        });
      });
  }

  send(): void {
    const content = this.newMessage.trim();
    if (!content) {
      return;
    }
    this.messageService.sendMessage(this.taskId, content).subscribe({
      next: (message) => {
        this.thread?.messages.push(message);
        this.newMessage = '';
      },
      error: () => {
        this.errorMessage = 'No se pudo enviar el mensaje.';
      },
    });
  }

  remove(message: MessageItem): void {
    this.messageService.deleteMessage(this.taskId, message.id).subscribe({
      next: () => {
        if (this.thread) {
          this.thread.messages = this.thread.messages.filter((m) => m.id !== message.id);
        }
      },
      error: () => {
        this.errorMessage = 'No se pudo borrar el mensaje.';
      },
    });
  }

  isMine(message: MessageItem): boolean {
    return message.senderId === this.auth.getUserId();
  }
}
