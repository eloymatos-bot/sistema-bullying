import { Component, Input, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { Api, message, roleName } from '../../../core/api';
@Component({selector:'app-shell',standalone:true,imports:[CommonModule,RouterLink,RouterLinkActive],templateUrl:'./app-shell.component.html',styleUrl:'./app-shell.component.css'})
export class AppShellComponent {
 @Input() title='Panel general';@Input() eyebrow='CONVIVENCIA ESCOLAR';@Input() creating=false;@Input() back=false;@Input() customActions=false;
 api=inject(Api);roleName=roleName;error='';busy=false;
 get initials(){const u=this.api.user();return u?(u.nombres[0]||'')+(u.apellidos[0]||''):'';}
 async logout(){if(this.busy)return;this.busy=true;this.error='';try{await this.api.logout();}catch(e){this.error=message(e);}finally{this.busy=false;}}
}
