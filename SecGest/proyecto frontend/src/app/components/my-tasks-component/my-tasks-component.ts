import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TaskService } from '../../service/task-service';
import { Task } from '../../model/task';
import { TaskDocumentsComponent } from '../task-documents-component/task-documents-component';
import { TaskChatComponent } from '../task-chat-component/task-chat-component';

@Component({
  selector: 'app-my-tasks-component',
  imports: [CommonModule, RouterLink, ReactiveFormsModule, FormsModule, TaskDocumentsComponent, TaskChatComponent],
  templateUrl: './my-tasks-component.html',
  styleUrl: './my-tasks-component.css',
})
export class MyTasksComponent implements OnInit {
  tasks: Task[] = [];
  loading = true;
  errorMessage = '';
  successMessage = '';

  // Id de la tarea que se esta editando en linea ahora mismo (null = ninguna).
  editingId: number | null = null;
  editForm: FormGroup;
  actionInProgressId: number | null = null;

  // Id de la tarea ENTREGADA para la que se esta mostrando el formulario de
  // valoracion ahora mismo (null = ninguno desplegado).
  ratingTaskId: number | null = null;
  selectedRating = 0;
  ratingComment = '';

  constructor(
    private taskService: TaskService,
    private fb: FormBuilder,
  ) {
    this.editForm = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(120)]],
      description: ['', [Validators.maxLength(1000)]],
    });
  }

  ngOnInit(): void {
    this.loadTasks();
  }

  loadTasks(): void {
    this.loading = true;
    this.taskService.getMyTasks().subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar tus tareas.';
        this.loading = false;
      },
    });
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'PENDIENTE_REVISION': return 'bg-secondary';
      case 'RECHAZADA': return 'bg-danger';
      case 'COMPLETADA': return 'bg-success';
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

  accept(task: Task): void {
    this.clearMessages();
    this.actionInProgressId = task.id;
    this.taskService.acceptTask(task.id).subscribe({
      next: () => {
        this.actionInProgressId = null;
        this.successMessage = `Has aceptado el presupuesto de "${task.title}".`;
        this.loadTasks();
      },
      error: () => {
        this.actionInProgressId = null;
        this.errorMessage = 'No se pudo aceptar el presupuesto.';
      },
    });
  }

  reject(task: Task): void {
    this.clearMessages();
    this.actionInProgressId = task.id;
    this.taskService.rejectTask(task.id).subscribe({
      next: () => {
        this.actionInProgressId = null;
        this.successMessage = `Presupuesto de "${task.title}" rechazado. Puedes editar y esperar un nuevo presupuesto.`;
        this.loadTasks();
      },
      error: () => {
        this.actionInProgressId = null;
        this.errorMessage = 'No se pudo rechazar el presupuesto.';
      },
    });
  }

  pay(task: Task): void {
    this.clearMessages();
    this.actionInProgressId = task.id;
    this.taskService.payTask(task.id).subscribe({
      next: () => {
        this.actionInProgressId = null;
        this.successMessage = `Pago de "${task.title}" realizado (simulado). Te hemos enviado el recibo por email.`;
        this.loadTasks();
      },
      error: () => {
        this.actionInProgressId = null;
        this.errorMessage = 'No se pudo procesar el pago.';
      },
    });
  }

  // Pago real con el TPV (Redsys/BBVA, entorno de pruebas): pide los datos
  // firmados a tasks y, con ellos, construye un <form> oculto y lo envia --
  // eso es lo que redirige de verdad el navegador a la pasarela de pago.
  payWithRedsys(task: Task): void {
    this.clearMessages();
    this.actionInProgressId = task.id;
    this.taskService.startRedsysPayment(task.id).subscribe({
      next: (formData) => {
        this.actionInProgressId = null;
        const form = document.createElement('form');
        form.method = 'POST';
        form.action = formData.actionUrl;

        this.addHiddenField(form, 'Ds_SignatureVersion', formData.dsSignatureVersion);
        this.addHiddenField(form, 'Ds_MerchantParameters', formData.dsMerchantParameters);
        this.addHiddenField(form, 'Ds_Signature', formData.dsSignature);

        document.body.appendChild(form);
        form.submit();
      },
      error: () => {
        this.actionInProgressId = null;
        this.errorMessage = 'No se pudo iniciar el pago con el TPV.';
      },
    });
  }

  private addHiddenField(form: HTMLFormElement, name: string, value: string): void {
    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = name;
    input.value = value;
    form.appendChild(input);
  }

  startRating(task: Task): void {
    this.clearMessages();
    this.ratingTaskId = task.id;
    this.selectedRating = 0;
    this.ratingComment = '';
  }

  cancelRating(): void {
    this.ratingTaskId = null;
  }

  setRating(stars: number): void {
    this.selectedRating = stars;
  }

  // Confirma la recepcion con la valoracion elegida (si el cliente selecciono
  // alguna estrella; si no, se confirma igualmente sin valorar).
  confirmCompletion(task: Task): void {
    this.clearMessages();
    this.actionInProgressId = task.id;
    const req = this.selectedRating > 0
      ? { rating: this.selectedRating, ratingComment: this.ratingComment || undefined }
      : undefined;

    this.taskService.completeTask(task.id, req).subscribe({
      next: () => {
        this.actionInProgressId = null;
        this.ratingTaskId = null;
        this.successMessage = `Encargo "${task.title}" completado. ¡Gracias!`;
        this.loadTasks();
      },
      error: () => {
        this.actionInProgressId = null;
        this.errorMessage = 'No se pudo confirmar la recepcion.';
      },
    });
  }

  startEdit(task: Task): void {
    this.clearMessages();
    this.editingId = task.id;
    this.editForm.setValue({
      title: task.title,
      description: task.description ?? '',
    });
  }

  cancelEdit(): void {
    this.editingId = null;
    this.editForm.reset();
  }

  saveEdit(task: Task): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    const { title, description } = this.editForm.value;
    this.actionInProgressId = task.id;
    this.taskService
      .updateTask(task.id, {
        categoryId: task.categoryId,
        title,
        description,
        dueDate: task.dueDate,
        contactPhone: task.contactPhone,
        relevantUrl: task.relevantUrl,
        confidentialityLevel: task.confidentialityLevel,
      })
      .subscribe({
        next: () => {
          this.actionInProgressId = null;
          this.editingId = null;
          this.successMessage = 'Encargo actualizado.';
          this.loadTasks();
        },
        error: () => {
          this.actionInProgressId = null;
          this.errorMessage = 'No se pudo guardar la edicion.';
        },
      });
  }

  private clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
  }
}
