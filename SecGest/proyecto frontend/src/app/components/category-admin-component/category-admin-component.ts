import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { TaskCategoryService } from '../../service/task-category-service';
import { Category } from '../../model/category';

@Component({
  selector: 'app-category-admin-component',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './category-admin-component.html',
  styleUrl: './category-admin-component.css',
})
export class CategoryAdminComponent implements OnInit {
  categories: Category[] = [];
  categoryForm: FormGroup;
  editingId: number | null = null;
  errorMessage = '';
  loading = false;

  constructor(
    private fb: FormBuilder,
    private categoryService: TaskCategoryService,
  ) {
this.categoryForm = this.fb.group({
  name: ['', [Validators.required, Validators.maxLength(80)]],
  description: ['', [Validators.maxLength(255)]],
  icon: ['', [Validators.maxLength(60)]],
  colorHex: ['#007b5e'],
  imageUrl: ['', [Validators.maxLength(500)]],
  basePrice: [null],
  estimatedMinutes: [null],
});
  }

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.categoryService.getAllCategoriesForAdmin().subscribe({
      next: (categories) => (this.categories = categories),
      error: () => (this.errorMessage = 'No se pudieron cargar las categorias.'),
    });
  }

onSubmit(): void {
  this.errorMessage = '';
  if (this.categoryForm.invalid) {
    this.categoryForm.markAllAsTouched();
    return;
  }

  this.loading = true;
  const { name, description, icon, colorHex, imageUrl, basePrice, estimatedMinutes } = this.categoryForm.value;
  const payload = {
    name,
    description,
    icon: icon || undefined,
    colorHex: colorHex || undefined,
    imageUrl: imageUrl || undefined,
    basePrice: basePrice ?? undefined,
    estimatedMinutes: estimatedMinutes ?? undefined,
  };
  const request$ = this.editingId
    ? this.categoryService.updateCategory(this.editingId, payload)
    : this.categoryService.createCategory(payload);

  request$.subscribe({
    next: () => {
      this.loading = false;
      this.cancelEdit();
      this.loadCategories();
    },
    error: (err) => {
      this.loading = false;
      if (err.status === 409) {
        this.errorMessage = 'Ya existe una categoria con ese nombre.';
      } else if (err.status === 403) {
        this.errorMessage = 'No tienes permisos de administrador.';
      } else {
        this.errorMessage = 'No se pudo guardar la categoria.';
      }
    },
  });
}

startEdit(category: Category): void {
  this.editingId = category.id;
  this.categoryForm.setValue({
    name: category.name,
    description: category.description ?? '',
    icon: category.icon ?? '',
    colorHex: category.colorHex ?? '#007b5e',
    imageUrl: category.imageUrl ?? '',
    basePrice: category.basePrice ?? null,
    estimatedMinutes: category.estimatedMinutes ?? null,
  });
}

  cancelEdit(): void {
    this.editingId = null;
    this.categoryForm.reset({ colorHex: '#007b5e' });
  }

  toggleActive(category: Category): void {
    const action$ = category.active
      ? this.categoryService.deactivateCategory(category.id)
      : this.categoryService.activateCategory(category.id);

    action$.subscribe({
      next: () => this.loadCategories(),
      error: () => (this.errorMessage = 'No se pudo cambiar el estado de la categoria.'),
    });
  }

  get name() { return this.categoryForm.get('name')!; }
  get description() { return this.categoryForm.get('description')!; }
}
