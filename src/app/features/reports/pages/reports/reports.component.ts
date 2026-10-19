import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppShellComponent } from '../../../../shared/components/app-shell/app-shell.component';
import { Api, Report, message, badge, limaToday } from '../../../../core/api';
@Component({selector:'app-reports',standalone:true,imports:[CommonModule,FormsModule,AppShellComponent],templateUrl:'./reports.component.html',styleUrl:'./reports.component.css'})
export class ReportsComponent {
 private api=inject(Api);year=Number(limaToday().slice(0,4));data:Report|null=null;loading=true;error='';private request=0;cls=badge;
 constructor(){void this.load();}
 async load(){const request=++this.request;this.loading=true;this.error='';try{const data=await this.api.get<Report>('/reports',{year:this.year});if(request===this.request)this.data=data;}catch(e){if(request===this.request){this.error=message(e);this.data=null;}}finally{if(request===this.request)this.loading=false;}}
 get max(){return Math.max(1,...(this.data?.tipos.map(t=>t.total)||[]));}
 percent(n:number){return this.data?.total?n/this.data.total*100:0;}
 month(n:number){return new Intl.DateTimeFormat('es-PE',{month:'long',timeZone:'UTC'}).format(new Date(Date.UTC(2026,n-1,1)));}
 get months(){return Array.from({length:12},(_,i)=>this.data?.meses.find(m=>m.mes===i+1)||{mes:i+1,registrados:0,cerrados:0,promedioDias:null});}
 get difference(){return this.data?.totalAnterior?((this.data.total-this.data.totalAnterior)/this.data.totalAnterior*100):null;}
 print(){if(this.data&&!this.loading)window.print();}
}
