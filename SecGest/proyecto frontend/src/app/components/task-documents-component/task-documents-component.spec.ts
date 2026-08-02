import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { TaskDocumentsComponent } from './task-documents-component';
import { DocumentService } from '../../service/document-service';
import { AuthService } from '../../service/auth-service';
import { DocumentMeta } from '../../model/document';

describe('TaskDocumentsComponent', () => {
  let fixture: ComponentFixture<TaskDocumentsComponent>;
  let component: TaskDocumentsComponent;

  let documentServiceStub: {
    listDocuments: jasmine.Spy;
    uploadDocument: jasmine.Spy;
    downloadDocument: jasmine.Spy;
    deleteDocument: jasmine.Spy;
  };

  let authServiceStub: { hasRole: jasmine.Spy; getUserId: jasmine.Spy };

  const doc: DocumentMeta = {
    id: 1,
    taskId: 10,
    uploaderUserId: 5,
    uploaderRole: 'ROLE_USER',
    originalFilename: 'contrato.pdf',
    contentType: 'application/pdf',
    sizeBytes: 2048,
    uploadedAt: '2026-01-01T00:00:00',
  };

  function createStubs(currentUserId: number, isAdmin: boolean) {
    documentServiceStub = {
      listDocuments: jasmine.createSpy().and.returnValue(of([doc])),
      uploadDocument: jasmine.createSpy(),
      downloadDocument: jasmine.createSpy(),
      deleteDocument: jasmine.createSpy(),
    };

    authServiceStub = {
      hasRole: jasmine.createSpy().and.returnValue(isAdmin),
      getUserId: jasmine.createSpy().and.returnValue(currentUserId),
    };
  }

  async function configureModule() {
    await TestBed.configureTestingModule({
      imports: [TaskDocumentsComponent],
      providers: [
        { provide: DocumentService, useValue: documentServiceStub },
        { provide: AuthService, useValue: authServiceStub },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TaskDocumentsComponent);
    component = fixture.componentInstance;
    component.taskId = 10;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    createStubs(5, false); // default: same user who uploaded the doc
    await configureModule();
  });

it('should create y carga los documentos ya al iniciar (para poder colorear el boton)', () => {
  expect(component).toBeTruthy();
  expect(documentServiceStub.listDocuments).toHaveBeenCalledWith(10);
  expect(component.documents.length).toBe(1);
});

  it('toggle despliega y carga los documentos la primera vez', () => {
    component.toggle();

    expect(component.expanded).toBe(true);
    expect(documentServiceStub.listDocuments).toHaveBeenCalledWith(10);
    expect(component.documents.length).toBe(1);
  });

  it('toggle no vuelve a pedir los documentos si ya estaban cargados', () => {
    component.toggle();
    documentServiceStub.listDocuments.calls.reset();

    component.toggle(); // colapsa
    component.toggle(); // vuelve a desplegar

    expect(documentServiceStub.listDocuments).not.toHaveBeenCalled();
  });

  it('load muestra un mensaje de error si falla', () => {
    documentServiceStub.listDocuments.and.returnValue(throwError(() => ({ status: 500 })));

    component.load();

    expect(component.errorMessage).toContain('No se pudieron cargar');
  });

  it('formatSize da un resultado legible en B/KB/MB', () => {
    expect(component.formatSize(500)).toBe('500 B');
    expect(component.formatSize(2048)).toBe('2.0 KB');
    expect(component.formatSize(5 * 1024 * 1024)).toBe('5.0 MB');
  });

  it('onFileSelected sube el fichero elegido y recarga el listado', () => {
    documentServiceStub.uploadDocument.and.returnValue(of(doc));
    const file = new File(['contenido'], 'nuevo.pdf', { type: 'application/pdf' });
    const input = document.createElement('input');
    input.type = 'file';
    Object.defineProperty(input, 'files', { value: [file] });
    const event = { target: input } as unknown as Event;

    component.onFileSelected(event);

    expect(documentServiceStub.uploadDocument).toHaveBeenCalledWith(10, file);
    expect(documentServiceStub.listDocuments).toHaveBeenCalled();
  });

  it('onFileSelected muestra un error especifico si el fichero es demasiado grande (413)', () => {
    documentServiceStub.uploadDocument.and.returnValue(throwError(() => ({ status: 413 })));
    const file = new File(['x'], 'grande.pdf', { type: 'application/pdf' });
    const input = document.createElement('input');
    Object.defineProperty(input, 'files', { value: [file] });
    const event = { target: input } as unknown as Event;

    component.onFileSelected(event);

    expect(component.errorMessage).toContain('tamaño máximo');
  });

  it('canDelete es true si el usuario actual subio el documento', () => {
    expect(component.canDelete(doc)).toBe(true);
  });

  it('canDelete es true si el usuario actual es admin, aunque no lo subiera el', async () => {
    TestBed.resetTestingModule();           // ← important
    createStubs(999, true);
    await configureModule();

    expect(component.canDelete(doc)).toBe(true);
  });

  it('canDelete es false si no es ni el subidor ni admin', async () => {
    TestBed.resetTestingModule();           // ← important
    createStubs(999, false);
    await configureModule();

    expect(component.canDelete(doc)).toBe(false);
  });

  it('delete no llama al servicio si el usuario cancela la confirmacion', () => {
    spyOn(window, 'confirm').and.returnValue(false);

    component.delete(doc);

    expect(documentServiceStub.deleteDocument).not.toHaveBeenCalled();
  });

  it('delete elimina y lo quita de la lista local si el usuario confirma', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    documentServiceStub.deleteDocument.and.returnValue(of(undefined));
    component.documents = [doc];

    component.delete(doc);

    expect(documentServiceStub.deleteDocument).toHaveBeenCalledWith(1);
    expect(component.documents.length).toBe(0);
  });

  it('delete muestra un mensaje de error si falla la peticion', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    documentServiceStub.deleteDocument.and.returnValue(throwError(() => ({ status: 500 })));

    component.delete(doc);

    expect(component.errorMessage).toContain('No se pudo eliminar');
  });
});