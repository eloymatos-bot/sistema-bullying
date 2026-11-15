const {chromium}=require('playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
async function run(){
 const browser=await chromium.launch({channel:'msedge',headless:true});
 const context=await browser.newContext({viewport:{width:1440,height:1024}});
 const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
 const base=process.env.TEST_FRONTEND_URL||'http://127.0.0.1:4200',prefix=process.env.TEST_PREFIX;
 const out=path.resolve('.local/remaining');fs.mkdirSync(out,{recursive:true});
 async function login(email){await page.goto(base+'/login');await page.getByLabel('Correo institucional').fill(email);await page.getByLabel('Contraseña',{exact:true}).fill(process.env.TEST_PASSWORD);await page.getByRole('button',{name:'Ingresar',exact:true}).click();await page.waitForURL('**/home');await page.locator('.stats').waitFor();}
 async function shot(name){await page.screenshot({path:path.join(out,name+'.png'),fullPage:true});}
 try{
  await login(process.env.TEST_EMAIL);
  await page.goto(base+'/usuarios');await page.locator('.users').waitFor();
  await page.getByRole('button',{name:'Nuevo usuario'}).click();let dialog=page.getByRole('dialog');
  const email=prefix+'_ui@example.invalid';
  await dialog.locator('input[name=names]').fill('Usuario');await dialog.locator('input[name=surnames]').fill('Prueba');await dialog.locator('input[name=email]').fill(email);await dialog.locator('input[name=password]').fill(process.env.TEST_PASSWORD);await dialog.getByRole('button',{name:'Guardar usuario'}).click();await page.locator('.success').waitFor();
  await page.getByLabel('Buscar usuarios').fill(email);await page.getByRole('button',{name:'Buscar',exact:true}).click();await page.locator('.users').waitFor();assert.equal(await page.locator('tbody tr').count(),1);console.log('OK: alta y búsqueda de usuario desde Angular');
  await page.getByRole('button',{name:'Editar',exact:true}).click();dialog=page.getByRole('dialog');await dialog.locator('input[name=surnames]').fill('Actualizado');await dialog.getByRole('button',{name:'Guardar usuario'}).click();await page.getByRole('dialog').waitFor({state:'hidden'});await page.getByText('Usuario Actualizado',{exact:true}).waitFor();console.log('OK: edición persistida en SQL');
  await page.getByRole('button',{name:'Desactivar',exact:true}).click();await page.getByRole('dialog').getByRole('button',{name:'Confirmar'}).click();await page.getByRole('button',{name:'Reactivar',exact:true}).waitFor();
  await page.getByRole('button',{name:'Reactivar',exact:true}).click();await page.getByRole('dialog').getByRole('button',{name:'Confirmar'}).click();await page.getByRole('button',{name:'Desactivar',exact:true}).waitFor();console.log('OK: desactivar y reactivar usuario');await shot('01-usuarios');
  await page.goto(base+'/cases/'+process.env.TEST_CASE_ID);await page.locator('.detail-grid').waitFor();
  await page.locator('textarea[name=action]').fill('Acción guardada desde el navegador para prueba.');
  await page.getByLabel('Estado después de la acción',{exact:true}).selectOption('EN_EVALUACION');
  const today=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Lima',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
  const tomorrow=new Date(today+'T12:00:00Z');tomorrow.setUTCDate(tomorrow.getUTCDate()+1);const next=tomorrow.toISOString().slice(0,10);
  await page.locator('input[name=nextDate]').fill(next);
  await page.getByRole('button',{name:'Guardar seguimiento'}).click();await page.getByText('Seguimiento guardado correctamente.',{exact:true}).waitFor();
  await page.reload();await page.locator('.history-entry').filter({hasText:'Acción guardada desde el navegador'}).waitFor();console.log('OK: seguimiento y estado sobreviven recarga');await shot('02-seguimiento');
  // Un cambio en otra sesión vuelve obsoleta la versión del formulario.
  const csrf=await (await context.request.get(base+'/api/auth/csrf')).json();
  const timeline=await (await context.request.get(base+'/api/cases/'+process.env.TEST_CASE_ID+'/followups')).json();
  const changed=await context.request.post(base+'/api/cases/'+process.env.TEST_CASE_ID+'/followups',{headers:{[csrf.headerName]:csrf.token},data:{version:timeline.version,accion:'Acción simultánea ficticia.',fechaAccion:today,estado:'EN_EVALUACION',proximaFecha:next}});assert.equal(changed.status(),201);
  await page.locator('textarea[name=action]').fill('Borrador que debe conservarse');await page.getByRole('button',{name:'Guardar seguimiento'}).click();await page.getByRole('button',{name:'Recargar estado y conservar texto'}).waitFor();
  await page.getByRole('button',{name:'Recargar estado y conservar texto'}).click();await page.locator('.detail-grid').waitFor();assert.equal(await page.locator('textarea[name=action]').inputValue(),'Borrador que debe conservarse');console.log('OK: conflicto concurrente conserva borrador');
  await page.goto(base+'/seguimiento');await page.locator('.alerts').waitFor();await page.getByLabel('Buscar alertas').fill(prefix);await page.getByRole('button',{name:'Buscar',exact:true}).click();await page.locator('.alerts').waitFor();assert.equal(await page.locator('article.alert').count(),2);await shot('03-alertas');
  await page.getByLabel('Tipo de alerta').selectOption('VENCIDA');await page.getByRole('button',{name:'Buscar',exact:true}).click();await page.locator('.empty').waitFor();console.log('OK: filtros y resultados vacíos de alertas');
  await page.goto(base+'/reportes');await page.locator('.reports-grid').waitFor();assert.equal(await page.locator('.bar-group').count(),6);assert.equal(await page.locator('.monthly tbody tr').count(),12);await shot('04-reportes');
  await page.pdf({path:path.join(out,'reporte-prueba.pdf'),format:'A4',printBackground:true,margin:{top:'10mm',bottom:'10mm',left:'8mm',right:'8mm'}});assert(fs.statSync(path.join(out,'reporte-prueba.pdf')).size>3000);console.log('OK: reporte imprimible con datos y PDF generado');
  for(const route of ['/seguimiento','/reportes','/usuarios']){await page.setViewportSize({width:390,height:844});await page.goto(base+route);await page.getByRole('heading',{level:1}).waitFor();await page.waitForLoadState('networkidle');assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),route+' desborda');await shot('movil-'+route.slice(1));}console.log('OK: pantallas móviles sin desbordamiento');
  await page.getByRole('button',{name:'Cerrar sesión',exact:true}).click();await page.waitForURL('**/login');await page.setViewportSize({width:1440,height:1024});
  await login(process.env.TEST_TEACHER);await page.goto(base+'/usuarios');await page.waitForURL('**/home');console.log('OK: docente no accede a usuarios');
  await page.goto(base+'/cases/'+process.env.TEST_ASSIGNED_ID);await page.locator('.detail-grid').waitFor();assert(await page.getByRole('button',{name:'Guardar seguimiento'}).isVisible());
  await page.goto(base+'/cases/'+process.env.TEST_CASE_ID);await page.getByRole('alert').waitFor();assert.equal(await page.locator('.history').count(),0);console.log('OK: permisos de seguimiento por caso');
  assert.deepEqual(errors,[]);console.log('PRUEBAS DE NAVEGADOR CORRECTAS');
 }finally{await browser.close();}
}
run().catch(e=>{console.error(e);process.exitCode=1;});
