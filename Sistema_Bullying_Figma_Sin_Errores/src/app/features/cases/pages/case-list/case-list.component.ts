import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AppShellComponent } from '../../../../shared/components/app-shell/app-shell.component';
import { Api, Catalogs, Detail, Page, Summary, badge, message } from '../../../../core/api';
@Component({selector:'app-case-list',standalone:true,imports:[CommonModule,FormsModule,RouterLink,AppShellComponent],templateUrl:'./case-list.component.html',styleUrl:'./case-list.component.css'})
export class FutListComponent {
 private api=inject(Api);private route=inject(ActivatedRoute);
 search='';status=this.route.snapshot.queryParamMap.get('estado')||'';type='';desde='';hasta='';offset=0;limit=10;total=0;loading=false;error='';rows:(Summary&{students:string})[]=[];catalogs:Catalogs={tipos:[],estados:[],participaciones:[]};cls=badge;
 private request=0;
 constructor(){void this.init();}
 async init(){try{this.catalogs=await this.api.get<Catalogs>('/cases/catalogs');await this.load();}catch(e){this.error=message(e);}}
 async load(offset=0){
  const request=++this.request;this.loading=true;this.error='';this.offset=offset;this.rows=[];
  try{
   const filters:Record<string,string|number>={q:this.search,estado:this.status,tipo:this.type,offset,limit:this.limit};
   if(this.desde)filters['desde']=this.desde;if(this.hasta)filters['hasta']=this.hasta;
   const page=await this.api.get<Page<Summary>>('/cases',filters);
   const rows=await Promise.all(page.items.map(async row=>{const d=await this.api.get<Detail>('/cases/'+row.id);return {...row,students:d.estudiantes.filter(s=>s.participacion==='AFECTADO').map(s=>s.estudiante.nombres+' '+s.estudiante.apellidos).join(', ')};}));
   if(request===this.request){this.rows=rows;this.total=page.total;}
  }catch(e){if(request===this.request){this.error=message(e);this.total=0;}}finally{if(request===this.request)this.loading=false;}
 }
 clear(){this.search='';this.status='';this.type='';this.desde='';this.hasta='';void this.load();}
 get end(){return Math.min(this.offset+this.rows.length,this.total);}
}
