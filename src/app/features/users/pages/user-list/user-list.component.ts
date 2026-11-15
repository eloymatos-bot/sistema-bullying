import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppShellComponent } from '../../../../shared/components/app-shell/app-shell.component';
import { Api, Directory, User, message, roleName } from '../../../../core/api';
@Component({selector:'app-user-list',standalone:true,imports:[CommonModule,FormsModule,AppShellComponent],templateUrl:'./user-list.component.html',styleUrl:'./user-list.component.css'})
export class UserListComponent {
 api=inject(Api);data:Directory|null=null;search='';role='';active='';loading=true;error='';success='';offset=0;limit=20;busy=false;dialog=false;editing:User|null=null;statusTarget:User|null=null;formError='';
 names='';surnames='';email='';password='';selectedRole='DOCENTE_TUTOR';roleName=roleName;roles=['ADMINISTRADOR','CONVIVENCIA','DOCENTE_TUTOR'];private request=0;
 constructor(){void this.load();}
 async load(offset=0){const request=++this.request;this.loading=true;this.error='';this.offset=offset;try{const params:Record<string,string|number>={q:this.search,rol:this.role,offset,limit:this.limit};if(this.active)params['activo']=this.active;const data=await this.api.get<Directory>('/users/directory',params);if(request===this.request)this.data=data;}catch(e){if(request===this.request)this.error=message(e);}finally{if(request===this.request)this.loading=false;}}
 open(user:User|null=null){this.editing=user;this.names=user?.nombres||'';this.surnames=user?.apellidos||'';this.email=user?.correo||'';this.selectedRole=user?.rol||'DOCENTE_TUTOR';this.password='';this.formError='';this.dialog=true;}
 close(){if(!this.busy){this.dialog=false;this.password='';}}
 async save(){if(this.busy)return;this.busy=true;this.formError='';this.success='';try{const body={nombres:this.names,apellidos:this.surnames,correo:this.email,rol:this.selectedRole};if(this.editing){const u=await this.api.write<User>('/users/'+this.editing.id,body,'PUT');if(u.id===this.api.user()?.id)this.api.user.set(u);}else{await this.api.write<User>('/users',{...body,password:this.password});}this.password='';this.dialog=false;this.success=this.editing?'Usuario actualizado.':'Usuario creado correctamente.';await this.load(this.offset);}catch(e){this.formError=message(e);}finally{this.busy=false;}}
 async changeStatus(){if(this.busy||!this.statusTarget)return;this.busy=true;this.error='';const u=this.statusTarget;try{await this.api.write('/users/'+u.id+'/status',{activo:!u.activo},'PATCH');this.statusTarget=null;this.success=u.activo?'Cuenta desactivada.':'Cuenta reactivada.';await this.load(this.offset);}catch(e){this.error=message(e);this.statusTarget=null;}finally{this.busy=false;}}
 initials(u:User){return (u.nombres[0]||'')+(u.apellidos[0]||'');}
 get selfEdit(){return this.editing?.id===this.api.user()?.id;}
 get end(){return Math.min(this.offset+(this.data?.resultados.items.length||0),this.data?.resultados.total||0);}
}
