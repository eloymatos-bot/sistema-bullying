import { Injectable, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

export interface User { id:number; nombres:string; apellidos:string; correo:string; rol:string; activo:boolean }
export interface Catalog { codigo:string; nombre:string }
export interface Student { id:number; nombres:string; apellidos:string; grado:number; seccion:string }
export interface Summary { id:number; codigo:string; fechaIncidente:string; lugar:string; tipoCodigo:string; tipo:string; estadoCodigo:string; estado:string; responsableId:number; responsable:string; registradoPor:number; creadoEn:string }
export interface Detail { caso:Summary; descripcion:string; observaciones:string|null; estudiantes:{estudiante:Student; participacion:string}[] }
export interface Page<T> { items:T[]; total:number; offset:number; limit:number }
export interface Catalogs { tipos:Catalog[]; estados:Catalog[]; participaciones:string[] }
export interface Responsible { id:number; nombres:string; apellidos:string; rol:string }
export interface PersonInput { estudianteId?:number; nuevoEstudiante?:{nombres:string;apellidos:string;grado:number;seccion:string}; participacion:string }
export interface CreateCase { fechaIncidente:string; lugar:string; tipoCodigo:string; descripcion:string; observaciones:string|null; responsableId:number; estudiantes:PersonInput[] }

export function message(error:unknown):string {
  if (error instanceof HttpErrorResponse) {
    if (error.status===0 || error.status===502 || error.status===504) return 'No se pudo conectar con el servidor. Comprueba que esté iniciado e inténtalo nuevamente.';
    if (error.status===401) return 'La sesión no es válida o las credenciales son incorrectas. Inicia sesión nuevamente.';
    if (error.status===403) return 'No tienes permiso o la sesión de seguridad venció. Vuelve a iniciar sesión.';
    if (error.status===404) return 'El caso no existe o no tienes acceso a él.';
    if (error.status===429) return 'Demasiados intentos. Espera un minuto e inténtalo nuevamente.';
    return error.error?.error || 'No se pudo completar la operación. Inténtalo nuevamente.';
  }
  return error instanceof Error ? error.message : 'No se pudo completar la operación.';
}
export function badge(status:string) { return status==='REPORTADO'?'badge-red':status==='EN_EVALUACION'?'badge-gold':status==='EN_SEGUIMIENTO'?'badge-green':'badge-dark'; }
export function roleName(role:string) { return ({ADMINISTRADOR:'Administrador',CONVIVENCIA:'Convivencia escolar',DOCENTE_TUTOR:'Docente / tutor'} as Record<string,string>)[role] || role; }

@Injectable({providedIn:'root'})
export class Api {
  private http=inject(HttpClient);
  private router=inject(Router);
  user=signal<User|null>(null);
  private async csrf() { return firstValueFrom(this.http.get<{token:string;headerName:string}>('/api/auth/csrf')); }
  async get<T>(path:string, params:Record<string,string|number>={}) {
    try { return await firstValueFrom(this.http.get<T>('/api'+path,{params})); }
    catch(e) { if(e instanceof HttpErrorResponse && e.status===401){this.user.set(null); void this.router.navigate(['/login']);} throw e; }
  }
  async write<T>(path:string, body:unknown) {
    const token=await this.csrf();
    try { return await firstValueFrom(this.http.post<T>('/api'+path,body,{headers:{[token.headerName]:token.token}})); }
    catch(e) { if(e instanceof HttpErrorResponse && e.status===401){this.user.set(null); void this.router.navigate(['/login']);} throw e; }
  }
  async login(email:string,password:string) {
    const token=await this.csrf();
    await firstValueFrom(this.http.post('/api/auth/login',new HttpParams().set('email',email).set('password',password),{headers:{[token.headerName]:token.token}}));
    const user=await this.get<User>('/auth/me'); this.user.set(user); return user;
  }
  async session() { const user=await this.get<User>('/auth/me'); this.user.set(user); return user; }
  async logout() { await this.write('/auth/logout',{}); this.user.set(null); await this.router.navigate(['/login']); }
  async allResponsibles() {
    const result:Responsible[]=[];
    for(let offset=0;;offset+=100){const rows=await this.get<Responsible[]>('/cases/responsibles',{offset,limit:100});result.push(...rows);if(rows.length<100)return result;}
  }
}
export const sessionGuard:CanActivateFn=async()=>{
  const api=inject(Api), router=inject(Router);
  try { await api.session();return true; } catch { return router.createUrlTree(['/login']); }
};
export const adminGuard:CanActivateFn=()=>inject(Api).user()?.rol==='ADMINISTRADOR' || inject(Router).createUrlTree(['/home']);
