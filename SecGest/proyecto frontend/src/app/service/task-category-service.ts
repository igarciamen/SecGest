import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Category, CategoryRequest } from '../model/category';

@Injectable({
  providedIn: 'root',
})
export class TaskCategoryService {
  // Microservicio "categories" (puerto 8083), independiente de "users" (8081) y "tasks" (8082).
  private categoriesUrl = 'http://localhost:8083/api/categories';

  constructor(private http: HttpClient) {}

  // Publico: solo categorias activas (catalogo de cara al cliente).
  getCategories(): Observable<Category[]> {
    return this.http.get<Category[]>(this.categoriesUrl);
  }

  // Solo admin: activas e inactivas, para poder gestionarlas todas.
  getAllCategoriesForAdmin(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.categoriesUrl}/all`);
  }

  createCategory(req: CategoryRequest): Observable<Category> {
    return this.http.post<Category>(this.categoriesUrl, req);
  }

  updateCategory(id: number, req: CategoryRequest): Observable<Category> {
    return this.http.put<Category>(`${this.categoriesUrl}/${id}`, req);
  }

  // Baja logica: la categoria deja de aparecer en el catalogo publico, pero
  // las tareas que ya la referencian siguen pudiendo mostrar su nombre.
  deactivateCategory(id: number): Observable<Category> {
    return this.http.delete<Category>(`${this.categoriesUrl}/${id}`);
  }

  activateCategory(id: number): Observable<Category> {
    return this.http.put<Category>(`${this.categoriesUrl}/${id}/activate`, {});
  }
}
