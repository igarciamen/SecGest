import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DocumentMeta } from '../model/document';

@Injectable({
  providedIn: 'root',
})
export class DocumentService {
  // Microservicio "documents" (puerto 8085).
  private documentsUrl = 'http://localhost:8085/api/documents';

  constructor(private http: HttpClient) {}

  uploadDocument(taskId: number, file: File): Observable<DocumentMeta> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<DocumentMeta>(`${this.documentsUrl}/tasks/${taskId}`, formData);
  }

  listDocuments(taskId: number): Observable<DocumentMeta[]> {
    return this.http.get<DocumentMeta[]>(`${this.documentsUrl}/tasks/${taskId}`);
  }

  // El interceptor ya existente añade el token; pedimos el fichero como blob
  // para poder generar la descarga en el navegador sin cambiar de pagina.
  downloadDocument(id: number): Observable<Blob> {
    return this.http.get(`${this.documentsUrl}/${id}/download`, { responseType: 'blob' });
  }

  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${this.documentsUrl}/${id}`);
  }
}
