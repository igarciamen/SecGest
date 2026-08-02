import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TaskService } from '../../service/task-service';
import { Task, TaskStatus } from '../../model/task';
import { TaskDocumentsComponent } from '../task-documents-component/task-documents-component';
import { TaskChatComponent } from '../task-chat-component/task-chat-component';
import { TaskNotesComponent } from '../task-notes-component/task-notes-component';

@Component({
  selector: 'app-admin-tasks-component',
  imports: [CommonModule, FormsModule, TaskDocumentsComponent, TaskChatComponent, TaskNotesComponent],
  templateUrl: './admin-tasks-component.html',
  styleUrl: './admin-tasks-component.css',
})
export class AdminTasksComponent implements OnInit {
  tasks: Task[] = [];
  loading = true;
  errorMessage = '';
  successMessage = '';
  actionInProgressId: number | null = null;

  // '' = sin filtro (todas). Se ofrecen los estados mas relevantes para el admin.
  statusFilter: TaskStatus | '' = '';
  statusOptions: TaskStatus[] = [
    'PENDIENTE_REVISION', 'PRESUPUESTADA', 'ACEPTADA', 'PAGADA',
    'ASIGNADA', 'EN_PROCESO', 'ENTREGADA', 'COMPLETADA', 'RECHAZADA',
  ];

  constructor(private taskService: TaskService) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.taskService.getAllTasksForAdmin(this.statusFilter || undefined).subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar las tareas.';
        this.loading = false;
      },
    });
  }

  deliver(task: Task): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.actionInProgressId = task.id;
    this.taskService.deliverTask(task.id).subscribe({
      next: () => {
        this.actionInProgressId = null;
        this.successMessage = `"${task.title}" marcada como entregada.`;
        this.load();
      },
      error: () => {
        this.actionInProgressId = null;
        this.errorMessage = 'No se pudo marcar la tarea como entregada.';
      },
    });
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'PENDIENTE_REVISION': return 'bg-secondary';
      case 'RECHAZADA': return 'bg-danger';
      case 'COMPLETADA': return 'bg-success';
      case 'PAGADA': return 'bg-success';
      case 'ACEPTADA': return 'bg-primary';
      case 'ENTREGADA': return 'bg-info text-dark';
      default: return 'bg-info text-dark';
    }
  }
  spineClass(status: string): string {
  switch (status) {
    case 'PENDIENTE_REVISION': return 'spine-neutral';
    case 'RECHAZADA': return 'spine-rejected';
    case 'COMPLETADA': return 'spine-done';
    case 'PRESUPUESTADA': return 'spine-progress';
    case 'ACEPTADA':
    case 'PAGADA':
    case 'ENTREGADA': return 'spine-milestone';
    default: return 'spine-progress';
  }
}
}
