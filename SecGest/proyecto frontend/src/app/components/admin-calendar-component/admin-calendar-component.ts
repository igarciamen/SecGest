import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskService } from '../../service/task-service';
import { Task } from '../../model/task';

interface CalendarDay {
  date: Date;
  inCurrentMonth: boolean;
  isToday: boolean;
  tasks: Task[];
}

@Component({
  selector: 'app-admin-calendar-component',
  imports: [CommonModule],
  templateUrl: './admin-calendar-component.html',
  styleUrl: './admin-calendar-component.css',
})
export class AdminCalendarComponent implements OnInit {
  currentMonth = new Date();
  currentMonth$label = '';
  days: CalendarDay[] = [];
  loading = false;
  errorMessage = '';

  weekDayLabels = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];

  constructor(private taskService: TaskService) {}

  ngOnInit(): void {
    this.load();
  }

  previousMonth(): void {
    this.currentMonth = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth() - 1, 1);
    this.load();
  }

  nextMonth(): void {
    this.currentMonth = new Date(this.currentMonth.getFullYear(), this.currentMonth.getMonth() + 1, 1);
    this.load();
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';

    const year = this.currentMonth.getFullYear();
    const month = this.currentMonth.getMonth();

    this.currentMonth$label = this.currentMonth.toLocaleDateString('es-ES', { month: 'long', year: 'numeric' });

    // Rango del mes completo, en formato ISO local (sin milisegundos ni zona,
    // igual que espera el backend: yyyy-MM-ddTHH:mm:ss).
    const from = this.toIsoLocal(new Date(year, month, 1, 0, 0, 0));
    const to = this.toIsoLocal(new Date(year, month + 1, 0, 23, 59, 59));

    this.taskService.getCalendarTasks(from, to).subscribe({
      next: (tasks) => {
        this.buildGrid(year, month, tasks);
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar las tareas del calendario.';
        this.loading = false;
      },
    });
  }

  private toIsoLocal(date: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
  }

  private buildGrid(year: number, month: number, tasks: Task[]): void {
    const firstOfMonth = new Date(year, month, 1);
    // Lunes = 0 ... Domingo = 6 (getDay() da 0=Domingo, se reindexa).
    const firstWeekday = (firstOfMonth.getDay() + 6) % 7;
    const gridStart = new Date(year, month, 1 - firstWeekday);

    const today = new Date();
    const days: CalendarDay[] = [];

    for (let i = 0; i < 42; i++) {
      const date = new Date(gridStart.getFullYear(), gridStart.getMonth(), gridStart.getDate() + i);
      const tasksForDay = tasks.filter((t) => t.dueDate && this.isSameDay(new Date(t.dueDate), date));

      days.push({
        date,
        inCurrentMonth: date.getMonth() === month,
        isToday: this.isSameDay(date, today),
        tasks: tasksForDay,
      });
    }

    this.days = days;
  }

  private isSameDay(a: Date, b: Date): boolean {
    return a.getFullYear() === b.getFullYear() && a.getMonth() === b.getMonth() && a.getDate() === b.getDate();
  }

  // Un vencimiento se resalta si esta en el pasado (y la tarea no se ha cerrado
  // aun), o si es en los proximos 3 dias -- asi el admin ve de un vistazo lo urgente.
taskUrgencyClass(task: Task): string {
  if (task.status === 'COMPLETADA' || task.status === 'RECHAZADA') {
    return 'cal-chip done';
  }
  if (!task.dueDate) {
    return 'cal-chip neutral';
  }
  const due = new Date(task.dueDate).getTime();
  const now = Date.now();
  const threeDaysMs = 3 * 24 * 60 * 60 * 1000;

  if (due < now) {
    return 'cal-chip overdue';
  }
  if (due - now <= threeDaysMs) {
    return 'cal-chip soon';
  }
  return 'cal-chip neutral';
}
}