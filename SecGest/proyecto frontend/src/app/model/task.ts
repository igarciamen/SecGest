export type TaskStatus =
  | 'PENDIENTE_REVISION'
  | 'PRESUPUESTADA'
  | 'ACEPTADA'
  | 'RECHAZADA'
  | 'PAGADA'
  | 'ASIGNADA'
  | 'EN_PROCESO'
  | 'ENTREGADA'
  | 'COMPLETADA';

export type ConfidentialityLevel = 'NORMAL' | 'SENSIBLE';

export interface Task {
  id: number;
  categoryId: number;
  categoryName: string;
  title: string;
  description?: string;
  status: TaskStatus;
  createdAt: string;
  dueDate?: string;
  contactPhone?: string;
  relevantUrl?: string;
  confidentialityLevel?: ConfidentialityLevel;
  price?: number;
  priceHistory?: number[];
  deliveredAt?: string;
  completedAt?: string;
  rating?: number;
  ratingComment?: string;
}

export interface CreateTaskRequest {
  categoryId: number;
  title: string;
  description?: string;
  dueDate?: string;
  contactPhone?: string;
  relevantUrl?: string;
  confidentialityLevel?: ConfidentialityLevel;
}

// Mismos campos que CreateTaskRequest: se usa para editar una tarea
// mientras esta en PENDIENTE_REVISION (tras un rechazo, o recien creada).
export type UpdateTaskRequest = CreateTaskRequest;

// Datos firmados que devuelve tasks para poder redirigir al cliente a la
// pasarela real de Redsys (TPV BBVA, entorno de pruebas).
export interface RedsysFormResponse {
  actionUrl: string;
  dsSignatureVersion: string;
  dsMerchantParameters: string;
  dsSignature: string;
}

// El cliente puede confirmar sin valorar (ambos campos opcionales).
export interface CompleteTaskRequest {
  rating?: number;
  ratingComment?: string;
}
