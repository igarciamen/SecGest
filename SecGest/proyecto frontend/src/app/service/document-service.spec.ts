import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { DocumentService } from './document-service';
import { DocumentMeta } from '../model/document';

describe('DocumentService', () => {
  let service: DocumentService;
  let httpMock: HttpTestingController;

  const documentsUrl = 'http://localhost:8085/api/documents';

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [DocumentService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(DocumentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('uploadDocument hace POST con FormData a /tasks/{taskId}', (done) => {
    const created: DocumentMeta = {
      id: 1, taskId: 10, uploaderUserId: 5, uploaderRole: 'ROLE_USER',
      originalFilename: 'contrato.pdf', contentType: 'application/pdf',
      sizeBytes: 1234, uploadedAt: '2026-01-01T00:00:00',
    };
    const file = new File(['contenido'], 'contrato.pdf', { type: 'application/pdf' });

    service.uploadDocument(10, file).subscribe((doc) => {
      expect(doc.originalFilename).toBe('contrato.pdf');
      done();
    });

    const req = httpMock.expectOne(`${documentsUrl}/tasks/10`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body instanceof FormData).toBe(true);
    req.flush(created);
  });

  it('listDocuments hace GET a /tasks/{taskId}', (done) => {
    service.listDocuments(10).subscribe((docs) => {
      expect(docs.length).toBe(0);
      done();
    });

    const req = httpMock.expectOne(`${documentsUrl}/tasks/10`);
    expect(req.request.method).toBe('GET');
    req.flush([]);
  });

  it('downloadDocument hace GET a /{id}/download pidiendo un blob', (done) => {
    const fakeBlob = new Blob(['contenido'], { type: 'application/pdf' });

    service.downloadDocument(1).subscribe((blob) => {
      expect(blob).toBeTruthy();
      done();
    });

    const req = httpMock.expectOne(`${documentsUrl}/1/download`);
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(fakeBlob);
  });
});
