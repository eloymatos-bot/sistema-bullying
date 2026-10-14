import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { AppShellComponent } from '../../../../shared/components/app-shell/app-shell.component';
import { Api, Detail, badge, message } from '../../../../core/api';
@Component({selector:'app-case-detail',standalone:true,imports:[CommonModule,AppShellComponent],templateUrl:'./case-detail.component.html',styleUrl:'./case-detail.component.css'})
export class FutDetailComponent {
 private api=inject(Api);private route=inject(ActivatedRoute);detail:Detail|null=null;loading=true;error='';cls=badge;created=typeof history!=='undefined'&&!!history.state?.created;
 constructor(){this.route.paramMap.subscribe(params=>void this.load(params.get('id')||''));}
 private request=0;
 async load(id:string){const request=++this.request;this.loading=true;this.error='';this.detail=null;try{if(!/^[1-9]\d*$/.test(id))throw new Error('El identificador del caso no es válido.');const detail=await this.api.get<Detail>('/cases/'+id);if(request===this.request)this.detail=detail;}catch(e){if(request===this.request)this.error=message(e);}finally{if(request===this.request)this.loading=false;}}
 label(p:string){return p==='AFECTADO'?'Afectado':p==='TESTIGO'?'Testigo':'Presunto agresor';}
 get title(){return this.detail?.estudiantes.filter(p=>p.participacion==='AFECTADO').map(p=>p.estudiante.nombres+' '+p.estudiante.apellidos).join(', ')||'Detalle del caso';}
}
