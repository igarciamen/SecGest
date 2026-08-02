import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TaskCategoryService } from './task-category-service';
import { Category } from '../model/category';

describe('TaskCategoryService', () => {
  let service: TaskCategoryService;
  let httpMock: HttpTestingController;

  const categoriesUrl = 'http://localhost:8083/api/categories';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TaskCategoryService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(TaskCategoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('getCategories hace GET publico (solo activas)', (done) => {
    const fake: Category[] = [
      { id: 1, name: 'Agenda', description: 'Gestion de citas', active: true },
    ];

    service.getCategories().subscribe((categories) => {
      expect(categories.length).toBe(1);
      done();
    });

    const req = httpMock.expectOne(categoriesUrl);
    expect(req.request.method).toBe('GET');
    req.flush(fake);
  });

  it('getAllCategoriesForAdmin hace GET a /all', (done) => {
    const fake: Category[] = [
      { id: 1, name: 'Agenda', active: true },
      { id: 2, name: 'Vieja', active: false },
    ];

    service.getAllCategoriesForAdmin().subscribe((categories) => {
      expect(categories.length).toBe(2);
      done();
    });

    const req = httpMock.expectOne(`${categoriesUrl}/all`);
    expect(req.request.method).toBe('GET');
    req.flush(fake);
  });

  it('createCategory hace POST con el body correcto', (done) => {
    const created: Category = { id: 3, name: 'Transcripcion', active: true };

    service.createCategory({ name: 'Transcripcion', description: 'Audio a texto' }).subscribe((cat) => {
      expect(cat.id).toBe(3);
      done();
    });

    const req = httpMock.expectOne(categoriesUrl);
    expect(req.request.method).toBe('POST');
    req.flush(created);
  });

  it('updateCategory hace PUT a /categories/{id}', (done) => {
    const updated: Category = { id: 5, name: 'Agenda', description: 'Nueva descripcion', active: true };

    service.updateCategory(5, { name: 'Agenda', description: 'Nueva descripcion' }).subscribe((cat) => {
      expect(cat.description).toBe('Nueva descripcion');
      done();
    });

    const req = httpMock.expectOne(`${categoriesUrl}/5`);
    expect(req.request.method).toBe('PUT');
    req.flush(updated);
  });

  it('deactivateCategory hace DELETE a /categories/{id}', (done) => {
    const deactivated: Category = { id: 7, name: 'Agenda', active: false };

    service.deactivateCategory(7).subscribe((cat) => {
      expect(cat.active).toBe(false);
      done();
    });

    const req = httpMock.expectOne(`${categoriesUrl}/7`);
    expect(req.request.method).toBe('DELETE');
    req.flush(deactivated);
  });

  it('activateCategory hace PUT a /categories/{id}/activate', (done) => {
    const activated: Category = { id: 7, name: 'Agenda', active: true };

    service.activateCategory(7).subscribe((cat) => {
      expect(cat.active).toBe(true);
      done();
    });

    const req = httpMock.expectOne(`${categoriesUrl}/7/activate`);
    expect(req.request.method).toBe('PUT');
    req.flush(activated);
  });
});
