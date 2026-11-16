import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AppShellComponent } from '../../../../shared/components/app-shell/app-shell.component';
import { Api, Page, StudentRecord, message } from '../../../../core/api';

@Component({selector:'app-student-list',standalone:true,imports:[CommonModule,FormsModule,AppShellComponent],templateUrl:'./student-list.component.html',styleUrl:'./student-list.component.css'})
export class StudentListComponent {
 api=inject(Api);
 data:Page<StudentRecord>|null=null;search='';gradeFilter='';sectionFilter='';offset=0;limit=20;loading=true;error='';success='';
 dialog=false;editing:StudentRecord|null=null;version='';busy=false;formError='';conflict=false;
 names='';surnames='';grade=1;section='';
 grades=[1,2,3,4,5];
 private request=0;
 constructor(){void this.load();}

 /** Solo administración y convivencia pueden crear o editar; el servidor lo vuelve a validar. */
 get canEdit(){const r=this.api.user()?.rol;return r==='ADMINISTRADOR'||r==='CONVIVENCIA';}

 async load(offset=0){
  const request=++this.request;this.loading=true;this.error='';this.offset=offset;
  try{
   const params:Record<string,string|number>={q:this.search.trim(),seccion:this.sectionFilter.trim(),offset,limit:this.limit};
   if(this.gradeFilter)params['grado']=this.gradeFilter;
   const data=await this.api.get<Page<StudentRecord>>('/students/directory',params);
   if(request===this.request)this.data=data;
  }catch(e){if(request===this.request)this.error=message(e);}
  finally{if(request===this.request)this.loading=false;}
 }
 find(){this.success='';void this.load(0);}

 open(student:StudentRecord|null=null){
  if(!this.canEdit)return;
  this.editing=student;this.version=student?.version||'';this.names=student?.nombres||'';this.surnames=student?.apellidos||'';
  this.grade=student?.grado||1;this.section=student?.seccion||'';this.formError='';this.conflict=false;this.dialog=true;
 }
 close(){if(!this.busy){this.dialog=false;this.conflict=false;}}

 async save(){
  if(this.busy)return;this.formError='';this.success='';
  const body={nombres:this.names.trim(),apellidos:this.surnames.trim(),grado:Number(this.grade),seccion:this.section.trim()};
  if(!body.nombres||!body.apellidos||!body.seccion){this.formError='Completa nombres, apellidos, grado y sección.';return;}
  if(body.nombres.length>100||body.apellidos.length>100||body.seccion.length>10||body.grado<1||body.grado>5){this.formError='Revisa los datos del estudiante.';return;}
  this.busy=true;
  try{
   const wasEditing=!!this.editing;
   if(this.editing){
    const saved=await this.api.write<StudentRecord>('/students/'+this.editing.id,{...body,version:this.version},'PUT');
    this.editing=saved;this.version=saved.version; // la versión local se actualiza tras guardar
   }else await this.api.write<StudentRecord>('/students',body);
   this.success=wasEditing?'Estudiante actualizado.':'Estudiante registrado correctamente.';
   this.dialog=false;this.conflict=false;
   await this.load(wasEditing?this.offset:0);
  }catch(e){
   this.conflict=e instanceof HttpErrorResponse&&e.status===409; // se conserva el borrador
   this.formError=message(e);
  }finally{this.busy=false;}
 }

 /** Ante un 409: trae los datos y la versión vigentes del servidor. Reemplaza el borrador solo si la persona lo pide. */
 async reloadCurrent(){
  if(this.busy||!this.editing)return;this.busy=true;this.formError='';
  try{
   const current=await this.api.get<StudentRecord>('/students/'+this.editing.id);
   this.editing=current;this.version=current.version;this.names=current.nombres;this.surnames=current.apellidos;
   this.grade=current.grado;this.section=current.seccion;this.conflict=false;
   await this.load(this.offset);
  }catch(e){this.formError=message(e);}
  finally{this.busy=false;}
 }

 /** Activa o desactiva con la versión vigente; el servidor responde 409 si otra persona lo cambió antes. */
 async toggle(s:StudentRecord){
  if(!this.canEdit||this.busy)return;
  const action=s.activo?'desactivar':'activar';
  const note=s.activo?' Dejará de aparecer al registrar casos nuevos, pero conservará su historial.':'';
  if(!window.confirm(`¿Deseas ${action} a ${s.nombres} ${s.apellidos}?${note}`))return;
  this.busy=true;this.success='';this.error='';
  try{
   await this.api.write<StudentRecord>('/students/'+s.id+'/status',{activo:!s.activo,version:s.version},'PATCH');
   this.success=s.activo?'Estudiante desactivado.':'Estudiante activado.';
  }catch(e){this.error=message(e);}
  finally{this.busy=false;await this.load(this.offset);}
 }

 initials(s:StudentRecord){return (s.nombres[0]||'')+(s.apellidos[0]||'');}
 get end(){return Math.min(this.offset+(this.data?.items.length||0),this.data?.total||0);}
}
