import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AppShellComponent } from '../../../../shared/components/app-shell/app-shell.component';
import { Api, Alerts, Responsible, message } from '../../../../core/api';
@Component({selector:'app-tracking',standalone:true,imports:[CommonModule,FormsModule,RouterLink,AppShellComponent],templateUrl:'./tracking.component.html',styleUrl:'./tracking.component.css'})
export class TrackingComponent {
 private api=inject(Api);data:Alerts|null=null;responsibles:Responsible[]=[];search='';clase='';responsable='';offset=0;limit=20;loading=true;error='';private request=0;
 constructor(){void this.load();void this.api.allResponsibles().then(r=>this.responsibles=r).catch(()=>{});}
 async load(offset=0){const request=++this.request;this.loading=true;this.error='';this.offset=offset;try{const params:Record<string,string|number>={q:this.search,clase:this.clase,offset,limit:this.limit};if(this.responsable)params['responsableId']=this.responsable;const data=await this.api.get<Alerts>('/alerts',params);if(request===this.request)this.data=data;}catch(e){if(request===this.request)this.error=message(e);}finally{if(request===this.request)this.loading=false;}}
 label(c:string){return ({VENCIDA:'Seguimiento vencido',PROXIMA:'Próximo seguimiento',EVALUACION:'Evaluación pendiente',SIN_FECHA:'Seguimiento sin fecha'} as Record<string,string>)[c]||c;}
 get end(){return Math.min(this.offset+(this.data?.resultados.items.length||0),this.data?.resultados.total||0);}
}
