import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TaskService } from '../../service/task-service';
import { Task } from '../../model/task';

@Component({
  selector: 'app-budget-admin-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './budget-admin-component.html',
  styleUrl: './budget-admin-component.css',
})
export class BudgetAdminComponent implements OnInit {
  tasks: Task[] = [];
  loading = true;
  errorMessage = '';
  successMessage = '';
  // Precio que el admin va escribiendo para cada tarea, indexado por id.
  priceInputs: Record<number, number | null> = {};
  submittingId: number | null = null;

  constructor(private taskService: TaskService) {}

  ngOnInit(): void {
    this.loadPending();
  }

  loadPending(): void {
    this.loading = true;
    this.taskService.getPendingTasks().subscribe({
      next: (tasks) => {
        this.tasks = tasks;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar las tareas pendientes.';
        this.loading = false;
      },
    });
  }

  submitBudget(task: Task): void {
    this.errorMessage = '';
    this.successMessage = '';
    const price = this.priceInputs[task.id];

    if (!price || price <= 0) {
      this.errorMessage = 'Indica un precio valido antes de presupuestar.';
      return;
    }

    this.submittingId = task.id;
    this.taskService.budgetTask(task.id, price).subscribe({
      next: () => {
        this.submittingId = null;
        this.successMessage = `Presupuesto enviado para "${task.title}".`;
        this.loadPending();
      },
      error: () => {
        this.submittingId = null;
        this.errorMessage = 'No se pudo guardar el presupuesto.';
      },
    });
  }
}
