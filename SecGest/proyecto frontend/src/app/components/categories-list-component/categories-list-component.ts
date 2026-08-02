import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TaskCategoryService } from '../../service/task-category-service';
import { Category } from '../../model/category';

@Component({
  selector: 'app-categories-list-component',
  imports: [CommonModule],
  templateUrl: './categories-list-component.html',
  styleUrl: './categories-list-component.css',
})
export class CategoriesListComponent implements OnInit {
  categories: Category[] = [];
  loading = true;
  errorMessage = '';

  constructor(private categoryService: TaskCategoryService) {}

  ngOnInit(): void {
    this.categoryService.getCategories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar las categorias.';
        this.loading = false;
      },
    });
  }
}
