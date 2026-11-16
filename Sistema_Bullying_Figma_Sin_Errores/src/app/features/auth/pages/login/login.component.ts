import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Api, message } from '../../../../core/api';
import { emailError, hasFieldErrors, passwordError } from '../../../../core/form-validation';
@Component({selector:'app-login',standalone:true,imports:[CommonModule,FormsModule],templateUrl:'./login.component.html',styleUrl:'./login.component.css'})
export class LoginComponent {
  private api=inject(Api); private router=inject(Router);
  email='';password='';remember=false;busy=false;error='';help=false;
  attempted = false;
  get fields() { return { email: emailError(this.email), password: passwordError(this.password) }; }
  constructor(){try{this.email=localStorage.getItem('bullying_email')||'';this.remember=!!this.email;localStorage.removeItem('bullying_session');localStorage.removeItem('bullying_user');}catch{}}
  async login(){
    if(this.busy)return;
    this.attempted = true;
    this.error = '';
    if (hasFieldErrors(this.fields)) return;
    this.busy = true;
    try {
      await this.api.login(this.email.trim(),this.password);
      try{if(this.remember)localStorage.setItem('bullying_email',this.email.trim());else localStorage.removeItem('bullying_email');}catch{}
      this.password='';await this.router.navigate(['/home']);
    } catch(e){this.error=message(e);}finally{this.busy=false;}
  }
}
