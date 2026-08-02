import { TestBed } from '@angular/core/testing';
import { ThemeService } from './theme-service';

describe('ThemeService', () => {
  let matchMediaSpy: jasmine.Spy;

  function mockPrefersDark(prefersDark: boolean): void {
    const fakeMediaQueryList = {
      matches: prefersDark,
      media: '(prefers-color-scheme: dark)',
      addListener: () => {},
      removeListener: () => {},
      addEventListener: () => {},
      removeEventListener: () => {},
      dispatchEvent: () => false,
      onchange: null,
    } as unknown as MediaQueryList;

    if (matchMediaSpy) {
      // Ya existe el spy (creado en beforeEach): solo actualizamos lo que devuelve.
      matchMediaSpy.and.returnValue(fakeMediaQueryList);
    } else {
      matchMediaSpy = spyOn(window, 'matchMedia').and.returnValue(fakeMediaQueryList);
    }
  }

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    // Por defecto, en cada test simulamos que el sistema operativo prefiere el tema claro,
    // para que los tests no dependan de la configuracion real de la maquina/navegador que ejecuta Karma.
    mockPrefersDark(false);
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    matchMediaSpy = undefined as unknown as jasmine.Spy;
  });

  function createService(): ThemeService {
    TestBed.configureTestingModule({ providers: [ThemeService] });
    return TestBed.inject(ThemeService);
  }

  it('sin preferencia guardada ni del sistema: usa "light" por defecto y lo aplica al <html>', () => {
    const service = createService();

    expect(service.getTheme()).toBe('light');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');

    console.log('=== theme: por defecto ===');
    console.log('Tema inicial:', service.getTheme());
  });

  it('sin preferencia guardada pero con el sistema en oscuro: respeta la preferencia del sistema', () => {
    mockPrefersDark(true);
    const service = createService();

    expect(service.getTheme()).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

    console.log('=== theme: preferencia del sistema (oscuro) ===');
    console.log('Tema inicial:', service.getTheme());
  });

  it('setTheme cambia el tema, lo persiste y lo aplica al <html>', () => {
    const service = createService();
    const emitted: string[] = [];
    service.theme$.subscribe((theme) => emitted.push(theme));

    service.setTheme('dark');

    expect(service.getTheme()).toBe('dark');
    expect(localStorage.getItem('theme')).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    // El BehaviorSubject emite primero el valor inicial ('light') y luego el nuevo ('dark').
    expect(emitted).toEqual(['light', 'dark']);

    console.log('=== theme: setTheme(dark) ===');
    console.log('Secuencia emitida  :', emitted);
    console.log('Guardado en storage:', localStorage.getItem('theme'));
  });

  it('toggleTheme alterna entre light y dark', () => {
    const service = createService();
    expect(service.getTheme()).toBe('light');

    service.toggleTheme();
    expect(service.getTheme()).toBe('dark');

    service.toggleTheme();
    expect(service.getTheme()).toBe('light');

    console.log('=== theme: toggleTheme ===');
    console.log('Tema tras dos toggles (vuelve al inicial):', service.getTheme());
  });

  it('con preferencia guardada en localStorage: la respeta al arrancar, por encima de la del sistema', () => {
    localStorage.setItem('theme', 'dark');
    // Aunque el sistema "prefiera" claro, debe ganar lo que el usuario eligio explicitamente.
    mockPrefersDark(false);

    const service = createService();

    expect(service.getTheme()).toBe('dark');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

    console.log('=== theme: preferencia guardada ===');
    console.log('Tema recuperado de localStorage:', service.getTheme());
  });
});