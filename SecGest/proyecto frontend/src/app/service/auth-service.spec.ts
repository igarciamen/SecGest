import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth-service';
import { UserInfo } from '../model/user-info';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  const authUrl = 'http://localhost:8081/api/auth';
  const userUrl = 'http://localhost:8081/api/user';

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('login guarda el token y emite el usuario', (done) => {
    const fakeUser: UserInfo = {
id: 2, username: 'marco', email: 'marco@mail.com', roles: ['ROLE_ADMIN'],    };

    service.login('isabel', '123456').subscribe((user) => {
      expect(user).toEqual(fakeUser);
      expect(service.getToken()).toBe('token-abc');
      expect(service.isAuthenticated()).toBe(true);
      expect(service.hasRole('ROLE_ADMIN')).toBe(true);

      console.log('=== login OK ===');
      console.log('Token guardado :', service.getToken());
      console.log('Usuario emitido:', user?.username, user?.roles);
      done();
    });

    // 1) El login hace POST /login y devolvemos un token simulado.
    const loginReq = httpMock.expectOne(`${authUrl}/login`);
    expect(loginReq.request.method).toBe('POST');
    loginReq.flush({ token: 'token-abc' });

    // 2) Tras guardar el token, encadena GET /me; devolvemos el usuario simulado.
    const meReq = httpMock.expectOne(`${userUrl}/me`);
    expect(meReq.request.method).toBe('GET');
    meReq.flush(fakeUser);
  });

  it('logout limpia el token y emite null', () => {
    localStorage.setItem('authToken', 'token-abc');

    let emitted: UserInfo | null = {} as any;
    service.userInfo$.subscribe((u) => (emitted = u));

    service.logout();

    expect(service.getToken()).toBeNull();
    expect(service.isAuthenticated()).toBe(false);
    expect(emitted).toBeNull();

    console.log('=== logout OK ===');
    console.log('Token tras logout:', service.getToken());
    console.log('Usuario emitido  :', emitted);
  });

  it('hasRole y getRoles reflejan los roles del usuario', (done) => {
    const fakeUser: UserInfo = {
      id: 2, username: 'marco', email: 'marco@mail.com', roles: ['ROLE_ADMIN'],
    };

    service.login('marco', '123456').subscribe(() => {
+      expect(service.getRoles()).toEqual(['ROLE_ADMIN']);
+      expect(service.hasRole('ROLE_ADMIN')).toBe(true);
+      expect(service.hasRole('ROLE_USER')).toBe(false);
       expect(service.getUserId()).toBe(2);

      console.log('=== hasRole / getRoles ===');
      console.log('Roles               :', service.getRoles());
      console.log('hasRole(ROLE_ADMIN) :', service.hasRole('ROLE_ADMIN'));
      console.log('hasRole(ROLE_USER)  :', service.hasRole('ROLE_USER'));
      console.log('getUserId           :', service.getUserId());
      done();
    });

    httpMock.expectOne(`${authUrl}/login`).flush({ token: 'token-xyz' });
    httpMock.expectOne(`${userUrl}/me`).flush(fakeUser);
  });
});
