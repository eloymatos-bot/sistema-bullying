import { Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AppShellComponent } from './app-shell/app-shell.component';
@Component({standalone:true,imports:[AppShellComponent,RouterLink],template:`<app-shell [title]="title"><div class="page"><section class="card-panel"><h2>Esta pantalla estará disponible próximamente</h2><p>Por ahora puedes registrar y consultar casos.</p><a class="btn-primary" routerLink="/cases">Ir a casos</a></section></div></app-shell>`})
export class PendingComponent {title=inject(ActivatedRoute).snapshot.data['title'];}
