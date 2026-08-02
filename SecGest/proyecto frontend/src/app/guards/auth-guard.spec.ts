import { Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { of } from 'rxjs';

import { AuthGuard } from './auth-guard';

import { UserInfo } from '../model/user-info';
import { AuthService } from '../service/auth-service';

describe('AuthGuard', () => {
  let router: jasmine.SpyObj<Router>;

  const routeWith = (roles?: string[]) =>
    ({ data: roles ? { roles } : {} } as any as ActivatedRouteSnapshot);
  const state = { url: '/protected' } as RouterStateSnapshot;

  beforeEach(() => {
    router = jasmine.createSpyObj('Router', ['navigate']);
  });

  it('sin sesion: redirige a /login y deniega', (done) => {
    const auth = { isAuthenticated: () => false } as unknown as AuthService;
    const guard = new AuthGuard(auth, router);

    guard.canActivate(routeWith(['ROLE_ADMIN']), state).subscribe((ok) => {
      expect(ok).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(
        ['/login'], { queryParams: { returnUrl: '/protected' } });

      console.log('=== guard: sin sesion ===');
      console.log('Permite?', ok, '-> redirige a /login');
      done();
    });
  });

  it('ruta sin roles requeridos: permite', (done) => {
    const auth = { isAuthenticated: () => true } as unknown as AuthService;
    const guard = new AuthGuard(auth, router);

    guard.canActivate(routeWith(), state).subscribe((ok) => {
      expect(ok).toBe(true);
      console.log('=== guard: sin roles requeridos ===');
      console.log('Permite?', ok);
      done();
    });
  });

  it('con el rol requerido: permite', (done) => {
    const user: UserInfo = { id: 1, username: 'marco', email: 'm@m.com', roles: ['ROLE_ADMIN'] };
    const auth = {
      isAuthenticated: () => true,
      getRoles: () => ['ROLE_ADMIN'],
      userInfo$: of(user),
      fetchUserInfo: () => of(user),
    } as unknown as AuthService;
    const guard = new AuthGuard(auth, router);

    guard.canActivate(routeWith(['ROLE_ADMIN']), state).subscribe((ok) => {
      expect(ok).toBe(true);
      expect(router.navigate).not.toHaveBeenCalled();
      console.log('=== guard: rol correcto ===');
      console.log('Permite?', ok);
      done();
    });
  });

  it('sin el rol requerido: deniega y va a /categories', (done) => {
    const user: UserInfo = { id: 1, username: 'marco', email: 'm@m.com', roles: ['ROLE_USER'] };
    const auth = {
      isAuthenticated: () => true,
      getRoles: () => ['ROLE_USER'],
      userInfo$: of(user),
      fetchUserInfo: () => of(user),
    } as unknown as AuthService;
    const guard = new AuthGuard(auth, router);

    guard.canActivate(routeWith(['ROLE_ADMIN']), state).subscribe((ok) => {
      expect(ok).toBe(false);
      expect(router.navigate).toHaveBeenCalledWith(['/categories']);
      console.log('=== guard: rol insuficiente ===');
      console.log('Permite?', ok, '-> redirige a /categories');
      done();
    });
  });
});
