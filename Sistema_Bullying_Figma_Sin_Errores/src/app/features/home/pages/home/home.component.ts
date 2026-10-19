import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AppShellComponent } from '../../../../shared/components/app-shell/app-shell.component';
import { Api, Alerts, Page, Summary, badge, message } from '../../../../core/api';
@Component({selector:'app-home',standalone:true,imports:[CommonModule,RouterLink,AppShellComponent],templateUrl:'./home.component.html',styleUrl:'./home.component.css'})
export class HomeComponent {
 private api=inject(Api);cases:Summary[]=[];counts:number[]=[];loading=true;error='';cls=badge;alerts:Alerts|null=null;
 states=['REPORTADO','EN_EVALUACION','EN_SEGUIMIENTO','CERRADO'];labels=['Reportados','En evaluación','En seguimiento','Cerrados'];notes=['Casos pendientes de evaluación','En revisión por el responsable','Con acciones de seguimiento','Casos finalizados'];colors=['red','gold','green','blue'];
 today=new Intl.DateTimeFormat('es-PE',{timeZone:'America/Lima',weekday:'long',day:'numeric',month:'long'}).format(new Date());
 constructor(){void this.load();}
 async load(){this.loading=true;this.error='';try{const [results,alerts]=await Promise.all([Promise.all([this.api.get<Page<Summary>>('/cases',{limit:5}),...this.states.map(estado=>this.api.get<Page<Summary>>('/cases',{estado,limit:1}))]),this.api.get<Alerts>('/alerts',{limit:3})]);this.cases=results[0].items;this.counts=results.slice(1).map(p=>p.total);this.alerts=alerts;}catch(e){this.error=message(e);}finally{this.loading=false;}}
}
