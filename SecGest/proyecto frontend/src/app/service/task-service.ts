import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { CompleteTaskRequest, CreateTaskRequest, RedsysFormResponse, Task, UpdateTaskRequest } from '../model/task';
import { Metrics } from '../model/metrics';

@Injectable({
  providedIn: 'root',
})
export class TaskService {
  // Microservicio "tasks" (puerto 8082).
  private tasksUrl = 'http://localhost:8082/api/tasks';

  constructor(private http: HttpClient) {}

  createTask(req: CreateTaskRequest): Observable<Task> {
    return this.http.post<Task>(this.tasksUrl, req);
  }

  getMyTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.tasksUrl}/mine`);
  }

  // Solo admin: tareas de todos los clientes pendientes de presupuestar.
  getPendingTasks(): Observable<Task[]> {
    return this.http.get<Task[]>(`${this.tasksUrl}/pending`);
  }

  budgetTask(id: number, price: number): Observable<Task> {
    return this.http.put<Task>(`${this.tasksUrl}/${id}/budget`, { price });
  }

  acceptTask(id: number): Observable<Task> {
    return this.http.put<Task>(`${this.tasksUrl}/${id}/accept`, {});
  }

  rejectTask(id: number): Observable<Task> {
    return this.http.put<Task>(`${this.tasksUrl}/${id}/reject`, {});
  }

  updateTask(id: number, req: UpdateTaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.tasksUrl}/${id}`, req);
  }

  payTask(id: number): Observable<Task> {
    return this.http.put<Task>(`${this.tasksUrl}/${id}/pay`, {});
  }

  // Pago real (TPV BBVA/Redsys, entorno de pruebas): devuelve los datos firmados
  // para redirigir al cliente a la pasarela, no cambia el estado de la tarea
  // todavia -- eso llega despues, via la notificacion de Redsys.
  startRedsysPayment(id: number): Observable<RedsysFormResponse> {
    return this.http.post<RedsysFormResponse>(`${this.tasksUrl}/${id}/pay/redsys/start`, {});
  }

  // Solo admin: todas las tareas, o filtradas por un estado concreto
  // (ej. 'ACEPTADA' para ver las pendientes de cobro).
  getAllTasksForAdmin(status?: string): Observable<Task[]> {
    const url = status ? `${this.tasksUrl}/all?status=${status}` : `${this.tasksUrl}/all`;
    return this.http.get<Task[]>(url);
  }

  // El admin marca una tarea PAGADA como entregada.
  deliverTask(id: number): Observable<Task> {
    return this.http.put<Task>(`${this.tasksUrl}/${id}/deliver`, {});
  }

  // El cliente confirma la recepcion de una tarea ENTREGADA; req puede ir vacio
  // (confirmar sin valorar) o con rating/ratingComment.
  completeTask(id: number, req?: CompleteTaskRequest): Observable<Task> {
    return this.http.put<Task>(`${this.tasksUrl}/${id}/complete`, req ?? {});
  }

  // Calendario del admin (Bloque 10): tareas con fecha limite en un rango dado.
getCalendarTasks(fromISO: string, toISO: string): Observable<Task[]> {
  return this.http.get<Task[]>(`${this.tasksUrl}/calendar?from=${fromISO}&to=${toISO}`);
}

// Metricas del panel de admin (Bloque 12).
getMetrics(): Observable<Metrics> {
  return this.http.get<Metrics>(`${this.tasksUrl}/metrics`);
}
}
