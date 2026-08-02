import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskService } from '../../service/task-service';
import { Metrics } from '../../model/metrics';

@Component({
  selector: 'app-admin-dashboard-component',
  imports: [CommonModule],
  templateUrl: './admin-dashboard-component.html',
  styleUrl: './admin-dashboard-component.css',
})
export class AdminDashboardComponent implements OnInit {
  metrics: Metrics | null = null;
  loading = true;
  errorMessage = '';

  // Orden fijo, para que el desglose por estado no salte de sitio cada vez
  // que cambian los datos (Object.keys() no garantiza orden estable).
  statusOrder = [
    'PENDIENTE_REVISION', 'PRESUPUESTADA', 'ACEPTADA', 'PAGADA',
    'ASIGNADA', 'EN_PROCESO', 'ENTREGADA', 'COMPLETADA', 'RECHAZADA',
  ];

  constructor(private taskService: TaskService) {}

  ngOnInit(): void {
    this.taskService.getMetrics().subscribe({
      next: (metrics) => {
        this.metrics = metrics;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar las métricas.';
        this.loading = false;
      },
    });
  }

  get totalTasks(): number {
    if (!this.metrics) return 0;
    return Object.values(this.metrics.tasksByStatus).reduce((sum, n) => sum + n, 0);
  }

  countFor(status: string): number {
    return this.metrics?.tasksByStatus[status] ?? 0;
  }
}