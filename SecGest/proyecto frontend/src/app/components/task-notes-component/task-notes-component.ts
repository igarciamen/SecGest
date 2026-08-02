import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MessageService } from '../../service/message-service';
import { InternalNoteItem } from '../../model/message';

// Solo se usa en la vista de admin (ni siquiera se incrusta en "Mis tareas"):
// el cliente no debe saber que esto existe. Sin polling -- son notas privadas,
// no hace falta sensacion de "en vivo".
@Component({
  selector: 'app-task-notes-component',
  imports: [CommonModule, FormsModule],
  templateUrl: './task-notes-component.html',
  styleUrl: './task-notes-component.css',
})
export class TaskNotesComponent implements OnInit {
  @Input({ required: true }) taskId!: number;

  notes: InternalNoteItem[] = [];
  newNote = '';
  expanded = false;
  loading = false;
  errorMessage = '';

  constructor(private messageService: MessageService) {}

  ngOnInit(): void {
    this.loadSilently();
  }

  private loadSilently(): void {
    this.messageService.getNotes(this.taskId).subscribe({
      next: (notes) => (this.notes = notes),
      error: () => {},
    });
  }

  toggle(): void {
    this.expanded = !this.expanded;
  }

  addNote(): void {
    const content = this.newNote.trim();
    if (!content) {
      return;
    }
    this.loading = true;
    this.messageService.addNote(this.taskId, content).subscribe({
      next: (note) => {
        this.notes = [note, ...this.notes];
        this.newNote = '';
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudo guardar la nota.';
        this.loading = false;
      },
    });
  }
}
