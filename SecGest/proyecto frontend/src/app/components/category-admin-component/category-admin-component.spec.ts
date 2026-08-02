import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { ReactiveFormsModule } from '@angular/forms';

import { CategoryAdminComponent } from './category-admin-component';
import { TaskCategoryService } from '../../service/task-category-service';
import { Category } from '../../model/category';

describe('CategoryAdminComponent', () => {
  let fixture: ComponentFixture<CategoryAdminComponent>;
  let component: CategoryAdminComponent;
  let categoryServiceStub: {
    getAllCategoriesForAdmin: jasmine.Spy;
    createCategory: jasmine.Spy;
    updateCategory: jasmine.Spy;
    deactivateCategory: jasmine.Spy;
    activateCategory: jasmine.Spy;
  };

  beforeEach(async () => {
    categoryServiceStub = {
      getAllCategoriesForAdmin: jasmine.createSpy().and.returnValue(of([] as Category[])),
      createCategory: jasmine.createSpy(),
      updateCategory: jasmine.createSpy(),
      deactivateCategory: jasmine.createSpy(),
      activateCategory: jasmine.createSpy(),
    };

    await TestBed.configureTestingModule({
      imports: [CategoryAdminComponent, ReactiveFormsModule],
      providers: [{ provide: TaskCategoryService, useValue: categoryServiceStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(CategoryAdminComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('el formulario es invalido sin nombre', () => {
    component.categoryForm.patchValue({ name: '' });
    expect(component.categoryForm.invalid).toBe(true);
  });

  it('onSubmit crea una categoria nueva cuando no se esta editando', () => {
    categoryServiceStub.createCategory.and.returnValue(
      of({ id: 1, name: 'Agenda', active: true } as Category),
    );

    component.categoryForm.patchValue({ name: 'Agenda', description: 'Gestion de citas' });
    component.onSubmit();

    expect(categoryServiceStub.createCategory).toHaveBeenCalled();
    expect(component.editingId).toBeNull();
  });

  it('startEdit rellena el formulario (incluidos los campos nuevos) y onSubmit llama a updateCategory', () => {
    const existing: Category = {
      id: 7, name: 'Redaccion', description: 'Cartas', active: true,
      icon: 'bi-pencil', colorHex: '#3366ff', basePrice: 12, estimatedMinutes: 20,
    };
    categoryServiceStub.updateCategory.and.returnValue(of(existing));

    component.startEdit(existing);
    expect(component.categoryForm.value.icon).toBe('bi-pencil');
    expect(component.categoryForm.value.basePrice).toBe(12);

    component.onSubmit();

    expect(categoryServiceStub.updateCategory).toHaveBeenCalledWith(7, jasmine.objectContaining({
      name: 'Redaccion',
      icon: 'bi-pencil',
    }));
  });

  it('onSubmit muestra mensaje de error si el nombre ya existe (409)', () => {
    categoryServiceStub.createCategory.and.returnValue(throwError(() => ({ status: 409 })));

    component.categoryForm.patchValue({ name: 'Agenda' });
    component.onSubmit();

    expect(component.errorMessage).toContain('Ya existe');
  });

  it('toggleActive desactiva una categoria activa', () => {
    const category: Category = { id: 3, name: 'Agenda', active: true };
    categoryServiceStub.deactivateCategory.and.returnValue(of({ ...category, active: false }));

    component.toggleActive(category);

    expect(categoryServiceStub.deactivateCategory).toHaveBeenCalledWith(3);
    expect(categoryServiceStub.activateCategory).not.toHaveBeenCalled();
  });

  it('toggleActive reactiva una categoria inactiva', () => {
    const category: Category = { id: 3, name: 'Agenda', active: false };
    categoryServiceStub.activateCategory.and.returnValue(of({ ...category, active: true }));

    component.toggleActive(category);

    expect(categoryServiceStub.activateCategory).toHaveBeenCalledWith(3);
    expect(categoryServiceStub.deactivateCategory).not.toHaveBeenCalled();
  });
});
