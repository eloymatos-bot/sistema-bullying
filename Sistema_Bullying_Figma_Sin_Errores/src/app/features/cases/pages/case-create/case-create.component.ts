import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AppShellComponent } from '../../../../shared/components/app-shell/app-shell.component';
import { Api, Catalogs, Detail, Page, PersonInput, Responsible, Student, message } from '../../../../core/api';
@Component({selector:'app-case-create',standalone:true,imports:[CommonModule,FormsModule,AppShellComponent],templateUrl:'./case-create.component.html',styleUrl:'./case-create.component.css'})
export class FutCreateComponent {
 private api=inject(Api);private router=inject(Router);
 date='';place='';type='';responsible=0;description='';observations='';error='';busy=false;loading=true;ready=false;
 catalogs:Catalogs={tipos:[],estados:[],participaciones:[]};responsibles:Responsible[]=[];
 people:{input:PersonInput;label:string}[]=[];names='';surnames='';grade=1;section='';participation='AFECTADO';mode='new';studentSearch='';found:Student[]=[];studentId=0;searching=false;
 today=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
 constructor(){void this.init();}
 async init(){this.loading=true;this.error='';try{[this.catalogs,this.responsibles]=await Promise.all([this.api.get<Catalogs>('/cases/catalogs'),this.api.allResponsibles()]);this.responsible=this.api.user()?.id||0;this.ready=true;}catch(e){this.error=message(e);}finally{this.loading=false;}}
 async find(){if(this.searching)return;this.searching=true;this.error='';try{const page=await this.api.get<Page<Student>>('/students',{q:this.studentSearch,limit:100});this.found=page.items;this.studentId=0;if(page.total>100)this.error='Se muestran 100 coincidencias; escribe un nombre más específico.';}catch(e){this.error=message(e);}finally{this.searching=false;}}
 add(){
  this.error='';if(this.people.length>=50){this.error='Máximo 50 participaciones por caso.';return;}
  if(this.mode==='existing'){
   const s=this.found.find(s=>s.id===Number(this.studentId));if(!s){this.error='Selecciona un estudiante de la búsqueda.';return;}
   if(this.people.some(p=>p.input.estudianteId===s.id&&p.input.participacion===this.participation)){this.error='Esa participación ya fue agregada.';return;}
   this.people.push({input:{estudianteId:s.id,participacion:this.participation},label:s.nombres+' '+s.apellidos});
  }else{
   if(!this.names.trim()||!this.surnames.trim()||!this.section.trim()){this.error='Completa nombres, apellidos, grado y sección del estudiante.';return;}
   if(this.names.trim().length>100||this.surnames.trim().length>100||this.section.trim().length>10||this.grade<1||this.grade>5){this.error='Revisa los datos del estudiante.';return;}
   this.people.push({input:{nuevoEstudiante:{nombres:this.names.trim(),apellidos:this.surnames.trim(),grado:Number(this.grade),seccion:this.section.trim()},participacion:this.participation},label:this.names.trim()+' '+this.surnames.trim()});
   this.names='';this.surnames='';this.section='';
  }
 }
 label(p:string){return p==='AFECTADO'?'Afectado':p==='TESTIGO'?'Testigo':'Presunto agresor';}
 cancel(){if(!this.busy)void this.router.navigate(['/cases']);}
 async save(){
  if(this.busy||!this.ready)return;this.error='';
  if(!this.people.some(p=>p.input.participacion==='AFECTADO')){this.error='Agrega al menos un estudiante afectado antes de registrar.';return;}
  if(this.names.trim()||this.surnames.trim()||this.section.trim()){this.error='Hay datos de un estudiante sin agregar. Pulsa «Agregar estudiante» o limpia esos campos.';return;}
  this.busy=true;
  try{const detail=await this.api.write<Detail>('/cases',{fechaIncidente:this.date,lugar:this.place,tipoCodigo:this.type,descripcion:this.description,observaciones:this.observations||null,responsableId:Number(this.responsible),estudiantes:this.people.map(p=>p.input)});await this.router.navigate(['/cases',detail.caso.id],{state:{created:true}});}catch(e){this.error=message(e);}finally{this.busy=false;}
 }
}
