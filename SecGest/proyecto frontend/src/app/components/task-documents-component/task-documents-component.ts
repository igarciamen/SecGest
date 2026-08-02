import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DocumentService } from '../../service/document-service';
import { AuthService } from '../../service/auth-service';
import { DocumentMeta } from '../../model/document';

// Componente reutilizable: se incrusta tanto en "Mis tareas" (cliente) como en
// "Todas las tareas" (admin) para no duplicar la logica de adjuntos en dos sitios.
@Component({
  selector: 'app-task-documents-component',
  imports: [CommonModule],
  templateUrl: './task-documents-component.html',
  styleUrl: './task-documents-component.css',
})
export class TaskDocumentsComponent implements OnInit {
  @Input({ required: true }) taskId!: number;

  documents: DocumentMeta[] = [];
  loading = false;
  errorMessage = '';
  uploading = false;
  expanded = false;
  deletingId: number | null = null;

  constructor(
    private documentService: DocumentService,
    private auth: AuthService,
  ) {}

  ngOnInit(): void {
    // Cargamos los documentos al iniciar para poder mostrar el contador (badge)
    // en el botón desde el primer momento.
    this.loadDocumentsSilently();
  }

  /**
   * Carga los documentos sin mostrar el loader (para el badge del botón)
   */
  private loadDocumentsSilently(): void {
    this.loading = true;
    this.errorMessage = '';
    
    this.documentService.listDocuments(this.taskId).subscribe({
      next: (docs) => {
        this.documents = docs;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar los adjuntos.';
        this.loading = false;
      },
    });
  }

  toggle(): void {
    this.expanded = !this.expanded;
    // Ya no es necesario cargar aquí porque se cargó en ngOnInit
  }

  load(): void {
    this.loading = true;
    this.errorMessage = '';
    this.documentService.listDocuments(this.taskId).subscribe({
      next: (docs) => {
        this.documents = docs;
        this.loading = false;
      },
      error: () => {
        this.errorMessage = 'No se pudieron cargar los adjuntos.';
        this.loading = false;
      },
    });
  }

  /**
   * Genera un color HSL consistente basado en el ID del documento.
   * Garantiza que cada documento tenga un color diferente y atractivo.
   */
  getDocumentColor(docId: number): string {
    const hue = (docId * 137.5) % 360; // Buena distribución de colores
    const saturation = 85;
    const lightness = 58;
    return `hsl(${hue}, ${saturation}%, ${lightness}%)`;
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.uploading = true;
    this.errorMessage = '';
    this.documentService.uploadDocument(this.taskId, file).subscribe({
      next: () => {
        this.uploading = false;
        input.value = '';
        this.load(); // recargamos la lista después de subir
      },
      error: (err) => {
        this.uploading = false;
        input.value = '';
        if (err.status === 413) {
          this.errorMessage = 'El fichero supera el tamaño máximo permitido (10 MB).';
        } else if (err.status === 415) {
          this.errorMessage = 'Ese tipo de fichero no está permitido.';
        } else {
          this.errorMessage = 'No se pudo subir el fichero.';
        }
      },
    });
  }

  download(doc: DocumentMeta): void {
    this.documentService.downloadDocument(doc.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.originalFilename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => {
        this.errorMessage = 'No se pudo descargar el fichero.';
      },
    });
  }

  // Solo se ofrece borrar lo que uno mismo subio, o si eres admin
  canDelete(doc: DocumentMeta): boolean {
    return this.auth.hasRole('ROLE_ADMIN') || this.auth.getUserId() === doc.uploaderUserId;
  }

  delete(doc: DocumentMeta): void {
    const confirmado = confirm(`¿Eliminar "${doc.originalFilename}"? Esta acción no se puede deshacer.`);
    if (!confirmado) {
      return;
    }

    this.deletingId = doc.id;
    this.documentService.deleteDocument(doc.id).subscribe({
      next: () => {
        this.deletingId = null;
        this.documents = this.documents.filter((d) => d.id !== doc.id);
      },
      error: () => {
        this.deletingId = null;
        this.errorMessage = 'No se pudo eliminar el fichero.';
      },
    });
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }
}