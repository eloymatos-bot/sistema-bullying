import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AppShellComponent } from '../../../../shared/components/app-shell/app-shell.component';
import { AgreementsComponent } from './agreements.component';
import { Api, Detail, Timeline, badge, message, stateName, limaToday } from '../../../../core/api';
import { hasFieldErrors, isCalendarDate, requiredText } from '../../../../core/form-validation';
@Component({selector:'app-case-detail',standalone:true,imports:[CommonModule,FormsModule,AppShellComponent,AgreementsComponent],templateUrl:'./case-detail.component.html',styleUrl:'./case-detail.component.css'})
export class FutDetailComponent {
 private api=inject(Api);private route=inject(ActivatedRoute);detail:Detail|null=null;timeline:Timeline|null=null;loading=true;error='';saveError='';success='';busy=false;conflict=false;action='';date=limaToday();nextDate='';state='';cls=badge;stateName=stateName;
 created=typeof history!=='undefined'&&!!history.state?.created;
 attempted=false;
 get today(){return limaToday();}
 get fields(){
  const incident=this.detail?.caso.fechaIncidente||'';
  const today=this.today;
  return {
   action: requiredText(this.action,'la descripción de la acción',4000),
   date: !isCalendarDate(this.date)||this.date<incident||this.date>today ? 'Elige una fecha entre el incidente y hoy.' : '',
   state: this.states.includes(this.state) ? '' : 'Selecciona el estado actual o el siguiente.',
   nextDate: this.state!=='CERRADO'&&this.nextDate&&(!isCalendarDate(this.nextDate)||this.nextDate<today||this.nextDate<incident)
    ? 'El próximo seguimiento debe ser hoy o una fecha posterior.' : ''
  };
 }
 private request=0;
 constructor(){this.route.paramMap.subscribe(params=>void this.load(params.get('id')||''));}
 async load(id:string,keepDraft=false){const request=++this.request;this.loading=true;this.error='';if(!keepDraft){this.detail=null;this.timeline=null;this.attempted=false;this.saveError='';this.success='';}
  try{if(!/^[1-9]\d*$/.test(id))throw new Error('El identificador del caso no es válido.');const [detail,timeline]=await Promise.all([this.api.get<Detail>('/cases/'+id),this.api.get<Timeline>('/cases/'+id+'/followups')]);if(request===this.request){this.detail=detail;this.timeline=timeline;this.state=timeline.estado;this.nextDate=timeline.proximaFecha||'';this.conflict=false;if(!keepDraft){this.action='';this.date=limaToday();}}}catch(e){if(request===this.request)this.error=message(e);}finally{if(request===this.request)this.loading=false;}}
 get states(){const all=['REPORTADO','EN_EVALUACION','EN_SEGUIMIENTO','CERRADO'];const i=all.indexOf(this.timeline?.estado||'');return all.slice(i,i+2);}
 changeState(){if(this.state==='CERRADO')this.nextDate='';}
 async save(){
  if(this.busy||!this.detail||!this.timeline||!this.timeline.puedeEditar||this.conflict)return;
  this.attempted=true;this.saveError='';this.success='';
  if(hasFieldErrors(this.fields))return;
  this.busy=true;const id=this.detail.caso.id;
  try{const timeline=await this.api.write<Timeline>('/cases/'+id+'/followups',{version:this.timeline.version,accion:this.action.trim(),fechaAccion:this.date,estado:this.state,proximaFecha:this.state==='CERRADO'?null:this.nextDate||null});this.timeline=timeline;this.detail.caso.estadoCodigo=timeline.estado;this.detail.caso.estado=stateName(timeline.estado);this.action='';this.nextDate=timeline.proximaFecha||'';this.attempted=false;this.success='Seguimiento guardado correctamente.';}
  catch(e){this.saveError=message(e);this.conflict=e instanceof HttpErrorResponse&&e.status===409;}finally{this.busy=false;}
 }
 refresh(){if(this.detail)void this.load(String(this.detail.caso.id),true);}
 label(p:string){return p==='AFECTADO'?'Afectado':p==='TESTIGO'?'Testigo':'Presunto agresor';}
 get title(){return this.detail?.estudiantes.filter(p=>p.participacion==='AFECTADO').map(p=>p.estudiante.nombres+' '+p.estudiante.apellidos).join(', ')||'Detalle del caso';}
 get overdue(){return !!this.timeline?.proximaFecha&&this.timeline.proximaFecha<this.today;}
}
