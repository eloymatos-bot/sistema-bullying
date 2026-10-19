// Ejecutar mediante run_e2e.py; usa cuentas ficticias y SQL Server local.
const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
async function main(){
 const browser=await chromium.launch({channel:'msedge',headless:true});
 const context=await browser.newContext({viewport:{width:1440,height:1024}});
 const page=await context.newPage();const errors=[];
 page.on('pageerror',e=>errors.push(e.message));
 const base=process.env.TEST_FRONTEND_URL||'http://127.0.0.1:4200';
 const out=path.resolve('.local/e2e');fs.mkdirSync(out,{recursive:true});
 async function check(name,test){await test();console.log('OK: '+name);}
 async function login(email,password){await page.getByLabel('Correo institucional').fill(email);await page.getByLabel('Contraseña',{exact:true}).fill(password);await page.getByRole('button',{name:'Ingresar',exact:true}).click();}
 try{
  await page.goto(base+'/cases');await page.waitForURL('**/login');
  await check('ruta protegida redirige al login',async()=>assert(page.url().endsWith('/login')));
  await page.screenshot({path:path.join(out,'01-login.png'),fullPage:true});
  await check('recursos Figma locales cargados',async()=>{
   await page.locator('.network img').first().waitFor();
   assert(await page.locator('.network img').evaluateAll(imgs=>imgs.length===11&&imgs.every(i=>i.complete&&i.naturalWidth>0)));
  });
  await login(process.env.TEST_EMAIL,'incorrecta');await page.getByRole('alert').waitFor();
  await check('contraseña incorrecta no permite acceso',async()=>assert(page.url().endsWith('/login')));
  await login(process.env.TEST_EMAIL,process.env.TEST_PASSWORD);await page.waitForURL('**/home');
  await page.locator('.stats').waitFor();
  await check('login real y resumen sin datos inventados',async()=>assert.equal(await page.locator('.stat b').first().innerText(),'0'));
  const token=await (await context.request.get(base+'/api/auth/csrf')).json();
  const teacherEmail=process.env.TEST_PREFIX+'_docente@example.invalid';
  const teacherResponse=await context.request.post(base+'/api/users',{headers:{[token.headerName]:token.token},data:{nombres:'Docente',apellidos:'Temporal',correo:teacherEmail,password:process.env.TEST_PASSWORD,rol:'DOCENTE_TUTOR'}});
  assert.equal(teacherResponse.status(),201);
  await page.goto(base+'/cases/nuevo');await page.getByRole('heading',{name:'Datos del incidente'}).waitFor();
  await page.getByLabel('Fecha del incidente').fill('2026-10-09');
  await page.getByLabel('Lugar',{exact:true}).fill(process.env.TEST_PREFIX+' Patio');
  await page.getByLabel('Tipo de bullying').selectOption('AGRESION_VERBAL');
  await page.getByLabel('Descripción de la situación').fill('Prueba ficticia de integración desde Angular.');
  await page.getByRole('button',{name:'Registrar caso',exact:true}).click();
  await check('exige al menos un afectado',async()=>assert.match(await page.getByRole('alert').innerText(),/afectado/));
  await page.getByLabel('Nombres',{exact:true}).fill(process.env.TEST_PREFIX);
  await page.getByLabel('Apellidos',{exact:true}).fill('Ficticio');
  await page.getByLabel('Grado',{exact:true}).selectOption({label:'2.° de secundaria'});
  await page.getByLabel('Sección',{exact:true}).fill('B');
  await page.getByRole('button',{name:'Agregar estudiante'}).click();
  await page.screenshot({path:path.join(out,'02-formulario.png'),fullPage:true});
  await page.getByRole('button',{name:'Registrar caso',exact:true}).click();
  await page.waitForURL(/\/cases\/\d+$/);
  await page.locator('.detail-grid').waitFor();
  const detailUrl=page.url();
  await check('caso guardado y detalle correcto',async()=>assert.match(await page.locator('.description').first().innerText(),/Prueba ficticia/));
  await page.reload();await page.locator('.detail-grid').waitFor();
  await check('caso y sesión sobreviven a recarga',async()=>assert.match(await page.locator('.people').innerText(),new RegExp(process.env.TEST_PREFIX)));
  await page.screenshot({path:path.join(out,'03-detalle.png'),fullPage:true});
  await page.getByRole('link',{name:'Volver a casos'}).click();await page.locator('.table-panel').waitFor();
  await page.getByLabel('Buscar casos').fill(process.env.TEST_PREFIX);await page.getByRole('button',{name:'Buscar',exact:true}).click();
  await page.locator('.table-panel').waitFor();
  await check('búsqueda encuentra registro persistido',async()=>assert.equal(await page.locator('tbody tr').count(),1));
  await page.screenshot({path:path.join(out,'04-listado.png'),fullPage:true});
  await page.getByLabel('Estado',{exact:true}).selectOption('CERRADO');await page.getByRole('button',{name:'Buscar',exact:true}).click();await page.locator('.empty').waitFor();
  await check('filtro y estado vacío',async()=>assert.match(await page.locator('.empty').innerText(),/No hay casos/));
  await page.goto(base+'/home');await page.locator('.stats').waitFor();
  await check('resumen actualizado desde SQL',async()=>assert.equal(await page.locator('.stat b').first().innerText(),'1'));
  await page.screenshot({path:path.join(out,'05-panel.png'),fullPage:true});
  await page.goto(base+'/cases/9223372036854775807');await page.getByRole('alert').waitFor();
  await check('caso inexistente no muestra ejemplo estático',async()=>assert.equal(await page.locator('.detail-grid').count(),0));
  await page.goto(detailUrl);await page.locator('.detail-grid').waitFor();
  await check('seguimiento real disponible sin historial ficticio',async()=>{
   assert.equal(await page.getByRole('button',{name:'Guardar seguimiento'}).count(),1);
   assert.equal(await page.locator('.history-entry').count(),1);
  });
  await page.setViewportSize({width:390,height:844});
  await check('detalle móvil sin desbordamiento horizontal',async()=>assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth)));
  await page.screenshot({path:path.join(out,'06-movil.png'),fullPage:true});
  await page.getByRole('button',{name:'Cerrar sesión'}).click();await page.waitForURL('**/login');
  await page.goto(detailUrl);await page.waitForURL('**/login');
  await check('logout revoca acceso',async()=>assert(page.url().endsWith('/login')));
  await page.setViewportSize({width:1440,height:1024});
  await login(teacherEmail,process.env.TEST_PASSWORD);await page.waitForURL('**/home');await page.locator('.stats').waitFor();
  await check('docente no ve casos de otro responsable',async()=>assert.equal(await page.locator('.stat b').first().innerText(),'0'));
  await check('docente no ve administración de usuarios',async()=>assert.equal(await page.getByRole('link',{name:'Usuarios',exact:false}).count(),0));
  await page.goto(base+'/usuarios');await page.waitForURL('**/home');
  await page.goto(detailUrl);await page.getByRole('alert').waitFor();
  await check('detalle ajeno bloqueado desde Angular y servidor',async()=>assert.equal(await page.locator('.detail-grid').count(),0));
  await page.getByRole('button',{name:'Cerrar sesión'}).click();await page.waitForURL('**/login');
  await check('sin errores JavaScript',async()=>assert.deepEqual(errors,[]));
  console.log('PRUEBAS ANGULAR + SPRING BOOT + SQL SERVER CORRECTAS');
 }finally{await browser.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
