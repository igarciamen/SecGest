import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { TaskNotesComponent } from './task-notes-component';
import { MessageService } from '../../service/message-service';
import { InternalNoteItem } from '../../model/message';

describe('TaskNotesComponent', () => {
  let fixture: ComponentFixture<TaskNotesComponent>;
  let component: TaskNotesComponent;
  let messageServiceStub: { getNotes: jasmine.Spy; addNote: jasmine.Spy };

  const existingNote: InternalNoteItem = { id: 1, authorUserId: 1, content: 'Nota vieja', createdAt: '2026-01-01T00:00:00' };

  beforeEach(async () => {
    messageServiceStub = {
      getNotes: jasmine.createSpy().and.returnValue(of([existingNote])),
      addNote: jasmine.createSpy(),
    };

    await TestBed.configureTestingModule({
      imports: [TaskNotesComponent],
      providers: [{ provide: MessageService, useValue: messageServiceStub }],
    }).compileComponents();

    fixture = TestBed.createComponent(TaskNotesComponent);
    component = fixture.componentInstance;
    component.taskId = 10;
    fixture.detectChanges();
  });

  it('should create y cargar las notas al iniciar', () => {
    expect(component).toBeTruthy();
    expect(messageServiceStub.getNotes).toHaveBeenCalledWith(10);
    expect(component.notes.length).toBe(1);
  });

  it('toggle despliega/colapsa sin volver a pedir nada', () => {
    messageServiceStub.getNotes.calls.reset();

    component.toggle();

    expect(component.expanded).toBe(true);
    expect(messageServiceStub.getNotes).not.toHaveBeenCalled();
  });

  it('addNote guarda la nota y la añade al principio de la lista', () => {
    const created: InternalNoteItem = { id: 2, authorUserId: 1, content: 'Nota nueva', createdAt: '2026-01-02T00:00:00' };
    messageServiceStub.addNote.and.returnValue(of(created));
    component.newNote = 'Nota nueva';

    component.addNote();

    expect(messageServiceStub.addNote).toHaveBeenCalledWith(10, 'Nota nueva');
    expect(component.notes[0].content).toBe('Nota nueva');
    expect(component.newNote).toBe('');
  });

  it('addNote no hace nada si esta vacia', () => {
    component.newNote = '   ';

    component.addNote();

    expect(messageServiceStub.addNote).not.toHaveBeenCalled();
  });

  it('addNote muestra un error si falla', () => {
    messageServiceStub.addNote.and.returnValue(throwError(() => ({ status: 500 })));
    component.newNote = 'Nota';

    component.addNote();

    expect(component.errorMessage).toContain('No se pudo guardar');
  });
});
