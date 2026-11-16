import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AppShellComponent } from '../../../../shared/components/app-shell/app-shell.component';
import { Api, Catalogs, Detail, Page, PersonInput, Responsible, Student, limaToday, message } from '../../../../core/api';
import { hasFieldErrors, isCalendarDate, requiredText, studentFields } from '../../../../core/form-validation';
@Component({selector:'app-case-create',standalone:true,imports:[CommonModule,FormsModule,AppShellComponent],templateUrl:'./case-create.component.html',styleUrl:'./case-create.component.css'})
export class FutCreateComponent {
 private api=inject(Api);private router=inject(Router);
 date='';place='';type='';responsible=0;description='';observations='';error='';busy=false;loading=true;ready=false;
 catalogs:Catalogs={tipos:[],estados:[],participaciones:[]};responsibles:Responsible[]=[];
 people:{input:PersonInput;label:string}[]=[];names='';surnames='';grade=1;section='';participation='AFECTADO';mode='new';studentSearch='';found:Student[]=[];studentId=0;searching=false;
 get today(){return limaToday();}
 attempted = false;
 studentAttempted = false;
 peopleError = '';
 get fields() {
  return {
   date: !isCalendarDate(this.date) ? 'Ingresa la fecha del incidente.' : this.date > this.today ? 'El incidente no puede tener una fecha futura.' : '',
   place: requiredText(this.place, 'el lugar', 200),
   type: this.catalogs.tipos.some(type => type.codigo === this.type) ? '' : 'Selecciona un tipo de incidente.',
   responsible: this.responsibles.some(person => person.id === this.responsible) ? '' : 'Selecciona un responsable activo.',
   description: requiredText(this.description, 'la descripción', 4000),
   observations: this.observations.trim().length > 2000 ? 'Las observaciones admiten hasta 2000 caracteres.' : ''
  };
 }
 get studentErrors(){return studentFields(this.names, this.surnames, this.grade, this.section);}
 constructor(){void this.init();}
 async init(){this.loading=true;this.error='';try{[this.catalogs,this.responsibles]=await Promise.all([this.api.get<Catalogs>('/cases/catalogs'),this.api.allResponsibles()]);this.responsible=this.api.user()?.id||0;this.ready=true;}catch(e){this.error=message(e);}finally{this.loading=false;}}
 async find(){if(this.searching)return;this.searching=true;this.error='';try{const page=await this.api.get<Page<Student>>('/students',{q:this.studentSearch,limit:100});this.found=page.items;this.studentId=0;if(page.total>100)this.error='Se muestran 100 coincidencias; escribe un nombre más específico.';}catch(e){this.error=message(e);}finally{this.searching=false;}}
 add(){
  if(this.busy)return;
  this.peopleError='';this.studentAttempted=this.mode==='new';
  if(this.people.length>=50){this.peopleError='Máximo 50 participaciones por caso.';return;}
  if(!this.catalogs.participaciones.includes(this.participation)){this.peopleError='Selecciona una participación válida.';return;}
  if(this.mode==='existing'){
   const s=this.found.find(s=>s.id===this.studentId);if(!s){this.peopleError='Selecciona un estudiante de la búsqueda.';return;}
   if(this.people.some(p=>p.input.estudianteId===s.id&&p.input.participacion===this.participation)){this.peopleError='Esa participación ya fue agregada.';return;}
   this.people.push({input:{estudianteId:s.id,participacion:this.participation},label:s.nombres+' '+s.apellidos});
   this.studentId=0;
  }else{
   if(hasFieldErrors(this.studentErrors))return;
   this.people.push({input:{nuevoEstudiante:{nombres:this.names.trim(),apellidos:this.surnames.trim(),grado:Number(this.grade),seccion:this.section.trim()},participacion:this.participation},label:this.names.trim()+' '+this.surnames.trim()});
   this.clearStudentDraft();
  }
 }
 clearStudentDraft(){this.names='';this.surnames='';this.section='';this.grade=1;this.studentId=0;this.studentAttempted=false;this.peopleError='';}
 label(p:string){return p==='AFECTADO'?'Afectado':p==='TESTIGO'?'Testigo':'Presunto agresor';}
 cancel(){if(!this.busy)void this.router.navigate(['/cases']);}
 async save(){
  if(this.busy||!this.ready)return;
  this.error='';this.peopleError='';this.attempted=true;
  if(this.names.trim()||this.surnames.trim()||this.section.trim()||this.studentId){
   this.peopleError='Hay un estudiante sin agregar. Agrégalo o limpia su selección.';
  }else if(!this.people.some(p=>p.input.participacion==='AFECTADO')){
   this.peopleError='Agrega al menos un estudiante afectado antes de registrar.';
  }
  if(hasFieldErrors(this.fields)||this.peopleError)return;
  this.busy=true;
  try{const detail=await this.api.write<Detail>('/cases',{fechaIncidente:this.date,lugar:this.place.trim(),tipoCodigo:this.type,descripcion:this.description.trim(),observaciones:this.observations.trim()||null,responsableId:this.responsible,estudiantes:this.people.map(p=>p.input)});await this.router.navigate(['/cases',detail.caso.id],{state:{created:true}});}catch(e){this.error=message(e);}finally{this.busy=false;}
 }
}
