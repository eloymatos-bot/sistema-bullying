import { Routes } from '@angular/router';
import { LoginComponent } from './features/auth/pages/login/login.component';
import { HomeComponent } from './features/home/pages/home/home.component';
import { FutListComponent } from './features/cases/pages/case-list/case-list.component';
import { FutCreateComponent } from './features/cases/pages/case-create/case-create.component';
import { FutDetailComponent } from './features/cases/pages/case-detail/case-detail.component';
import { sessionGuard, adminGuard } from './core/api';
import { PendingComponent } from './shared/components/pending.component';
export const routes:Routes=[
 {path:'',redirectTo:'home',pathMatch:'full'},
 {path:'login',component:LoginComponent},
 {path:'',canActivateChild:[sessionGuard],children:[
  {path:'home',component:HomeComponent},
  {path:'cases',component:FutListComponent},
  {path:'cases/nuevo',component:FutCreateComponent},
  {path:'cases/:id',component:FutDetailComponent},
  {path:'seguimiento',component:PendingComponent,data:{title:'Alertas'}},
  {path:'reportes',component:PendingComponent,data:{title:'Reportes'}},
  {path:'usuarios',component:PendingComponent,canActivate:[adminGuard],data:{title:'Usuarios'}}
 ]},
 {path:'**',redirectTo:'home'}
];
