import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { CategoriesListComponent } from './categories-list-component';
import { TaskCategoryService } from '../../service/task-category-service';
import { Category } from '../../model/category';

describe('CategoriesListComponent', () => {
  let fixture: ComponentFixture<CategoriesListComponent>;
  let component: CategoriesListComponent;

  it('should create', async () => {
    const categoryServiceStub = { getCategories: () => of([] as Category[]) };

    await TestBed.configureTestingModule({
      imports: [CategoriesListComponent],
      providers: [{ provide: TaskCategoryService, useValue: categoryServiceStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoriesListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component).toBeTruthy();
  });

  it('carga las categorias publicas al iniciar', async () => {
    const fake: Category[] = [

  { id: 1, name: 'Agenda', description: 'Gestion de citas', active: true },
  { id: 2, name: 'Redaccion', description: 'Cartas y documentos', active: true },
];
 
    const categoryServiceStub = { getCategories: () => of(fake) };

    await TestBed.configureTestingModule({
      imports: [CategoriesListComponent],
      providers: [{ provide: TaskCategoryService, useValue: categoryServiceStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoriesListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.categories.length).toBe(2);
    expect(component.loading).toBe(false);

    console.log('=== categories-list: carga correcta ===');
    console.log('Categorias cargadas:', component.categories.length);
  });

  it('muestra un mensaje de error si falla la carga', async () => {
    const categoryServiceStub = { getCategories: () => throwError(() => new Error('fallo de red')) };

    await TestBed.configureTestingModule({
      imports: [CategoriesListComponent],
      providers: [{ provide: TaskCategoryService, useValue: categoryServiceStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoriesListComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.errorMessage).toContain('No se pudieron cargar');
    expect(component.loading).toBe(false);

    console.log('=== categories-list: error de carga ===');
    console.log('Mensaje mostrado:', component.errorMessage);
  });
});
