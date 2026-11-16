import { Component, inject, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Api, Responsible, limaToday, message } from '../../../../core/api';
import { hasFieldErrors, isCalendarDate, requiredText } from '../../../../core/form-validation';

interface Agreement {
 id:number; descripcion:string; responsable:string; fechaLimite:string; creadoPor:string;
 creadoEn:string; cumplidoEn:string|null; cumplidoPor:string|null; version:string;
}
interface Directory {puedeEditar:boolean;items:Agreement[]}

@Component({selector:'app-agreements',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./agreements.component.html',styles:[`
 .card-panel{margin-top:16px}.agreement-list{display:grid;gap:12px;padding:0;list-style:none}
 li{border:1px solid #dce4e2;border-radius:10px;padding:12px;overflow-wrap:anywhere}
 form{display:grid;gap:10px;margin-top:18px}label{display:grid;gap:5px}
 textarea,select,input{width:100%;box-sizing:border-box}.result{color:#236041}
`]})
export class AgreementsComponent implements OnInit,OnChanges {
 @Input({required:true}) caseId!:number;
 @Input() editable=false;
 private api=inject(Api); private request=0;
 data:Directory|null=null; people:Responsible[]=[];
 description=''; responsibleId:number|null=null; date=limaToday();
 get today(){return limaToday();}
 busy=false; loading=false; error=''; success='';
 attempted=false;
 get fields(){
   return {
     description: requiredText(this.description,'la descripción del acuerdo',2000),
     responsible: this.people.some(person=>person.id===this.responsibleId) ? '' : 'Selecciona un responsable activo.',
     due: !isCalendarDate(this.date)||this.date<this.today ? 'La fecha límite debe ser hoy o posterior.' : ''
   };
 }
 ngOnInit(){void this.load();}
 ngOnChanges(changes:SimpleChanges){
   if(changes['caseId']&&!changes['caseId'].firstChange)void this.load();
 }
 async load(){
   const request=++this.request;
   this.loading=true;this.error='';
   try{
     const data=await this.api.get<Directory>('/cases/'+this.caseId+'/agreements');
     const people=data.puedeEditar?await this.api.allResponsibles():[];
     if(request===this.request){this.data=data;this.people=people;}
   }catch(e){if(request===this.request)this.error=message(e);}
   finally{if(request===this.request)this.loading=false;}
 }
 async save(){
   if(this.busy||!this.editable||!this.data?.puedeEditar)return;
   this.attempted=true;this.error='';this.success='';
   if(hasFieldErrors(this.fields))return;
   this.busy=true;
   try{
     const result=await this.api.write<Agreement>('/cases/'+this.caseId+'/agreements',
       {descripcion:this.description.trim(),responsableId:this.responsibleId,fechaLimite:this.date});
     this.data.items=[result,...this.data.items];this.description='';this.attempted=false;this.success='Acuerdo registrado.';
   }catch(e){this.error=message(e);}finally{this.busy=false;}
 }
 async complete(row:Agreement){
   if(this.busy||!this.editable||!this.data?.puedeEditar||row.cumplidoEn)return;
   this.busy=true;this.error='';this.success='';
   try{
     const result=await this.api.write<Agreement>('/cases/'+this.caseId+'/agreements/'+row.id+'/complete',{version:row.version},'PATCH');
     this.data.items=this.data.items.map(item=>item.id===row.id?result:item);this.success='Acuerdo cumplido.';
   }catch(e){this.error=message(e);}finally{this.busy=false;}
 }
}
