import { Routes } from '@angular/router';
import { SignupComponent } from './components/signup-component/signup-component';
import { LoginComponent } from './components/login-component/login-component';
import { CategoriesListComponent } from './components/categories-list-component/categories-list-component';
import { CategoryAdminComponent } from './components/category-admin-component/category-admin-component';
import { NewTaskComponent } from './components/new-task-component/new-task-component';
import { MyTasksComponent } from './components/my-tasks-component/my-tasks-component';
import { BudgetAdminComponent } from './components/budget-admin-component/budget-admin-component';
import { AdminTasksComponent } from './components/admin-tasks-component/admin-tasks-component';
import { PaymentOkComponent } from './components/payment-ok-component/payment-ok-component';
import { PaymentKoComponent } from './components/payment-ko-component/payment-ko-component';
import { AuthGuard } from './guards/auth-guard';
import { AdminCalendarComponent } from './components/admin-calendar-component/admin-calendar-component';
import { AdminDashboardComponent } from './components/admin-dashboard-component/admin-dashboard-component';


export const routes: Routes = [
  { path: '', redirectTo: 'categories', pathMatch: 'full' },
  { path: 'categories', component: CategoriesListComponent },
  // Publicas: Redsys redirige aqui al navegador del cliente tras el pago,
  // y en ese momento no podemos garantizar que la sesion siga activa/fresca.
  { path: 'payment/ok', component: PaymentOkComponent },
  { path: 'payment/ko', component: PaymentKoComponent },
  {
    path: 'admin/categories',
    component: CategoryAdminComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN'] },
  },
  {
    path: 'admin/tasks/pending',
    component: BudgetAdminComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN'] },
  },
  {
    path: 'admin/tasks/all',
    component: AdminTasksComponent,
    canActivate: [AuthGuard],
    data: { roles: ['ROLE_ADMIN'] },
  },
  // Sin roles en "data": solo exige sesion iniciada, cualquier usuario autenticado.
  { path: 'tasks/new', component: NewTaskComponent, canActivate: [AuthGuard] },
  { path: 'tasks/mine', component: MyTasksComponent, canActivate: [AuthGuard] },
  { path: 'signup', component: SignupComponent },
  { path: 'login', component: LoginComponent },

  {
  path: 'admin/calendar',
  component: AdminCalendarComponent,
  canActivate: [AuthGuard],
  data: { roles: ['ROLE_ADMIN'] },
},

{
  path: 'admin/dashboard',
  component: AdminDashboardComponent,
  canActivate: [AuthGuard],
  data: { roles: ['ROLE_ADMIN'] },
},

];
