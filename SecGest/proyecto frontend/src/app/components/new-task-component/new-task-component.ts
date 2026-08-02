import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TaskService } from '../../service/task-service';
import { TaskCategoryService } from '../../service/task-category-service';
import { Category } from '../../model/category';

@Component({
  selector: 'app-new-task-component',
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './new-task-component.html',
  styleUrl: './new-task-component.css',
})
export class NewTaskComponent implements OnInit {
  categories: Category[] = [];
  taskForm: FormGroup;
  errorMessage = '';
  loading = false;

  constructor(
    private fb: FormBuilder,
    private taskService: TaskService,
    private categoryService: TaskCategoryService,
    private router: Router,
  ) {
    this.taskForm = this.fb.group({
      categoryId: ['', [Validators.required]],
      title: ['', [Validators.required, Validators.maxLength(120)]],
      description: ['', [Validators.maxLength(1000)]],
      dueDate: [''],
      contactPhone: ['', [Validators.maxLength(30)]],
      relevantUrl: ['', [Validators.maxLength(500)]],
      confidentialityLevel: ['NORMAL'],
    });
  }

  ngOnInit(): void {
    this.categoryService.getCategories().subscribe({
      next: (categories) => (this.categories = categories),
      error: () => (this.errorMessage = 'No se pudieron cargar las categorias.'),
    });
  }

  onSubmit(): void {
    this.errorMessage = '';
    if (this.taskForm.invalid) {
      this.taskForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    const { categoryId, title, description, dueDate, contactPhone, relevantUrl, confidentialityLevel } =
      this.taskForm.value;

    this.taskService
      .createTask({
        categoryId: Number(categoryId),
        title,
        description,
        dueDate: dueDate || undefined,
        contactPhone: contactPhone || undefined,
        relevantUrl: relevantUrl || undefined,
        confidentialityLevel,
      })
      .subscribe({
        next: () => {
          this.loading = false;
          this.router.navigateByUrl('/tasks/mine');
        },
        error: () => {
          this.loading = false;
          this.errorMessage = 'No se pudo crear la tarea. Intentalo de nuevo.';
        },
      });
  }

  get categoryId() { return this.taskForm.get('categoryId')!; }
  get title() { return this.taskForm.get('title')!; }
  get description() { return this.taskForm.get('description')!; }
}
