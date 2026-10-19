"""Prueba de desarrollo: crea fixtures propios y conserva los datos existentes."""
import os, sys, subprocess, tempfile, zipfile
from pathlib import Path
from datetime import date, timedelta

ROOT=Path(__file__).resolve().parents[1]
BACKEND=ROOT.parent/'sistema-bullying-backend'
if not (BACKEND/'pom.xml').exists(): BACKEND=BACKEND/'sistema-bullying-backend'
sys.path.insert(0,str(BACKEND/'tests'))
from integration_auth import sql, Client, PREFIX, ADMIN, PASSWORD, expect

def hash_password():
    jar=BACKEND/'target/sistema-bullying-backend-0.0.1-SNAPSHOT.jar'
    with tempfile.TemporaryDirectory() as folder,zipfile.ZipFile(jar) as z:
        work=Path(folder); jars=[]
        for entry in z.namelist():
            if entry.startswith('BOOT-INF/lib/') and (Path(entry).name.startswith('spring-security-crypto-') or Path(entry).name.startswith('commons-logging-')) and entry.endswith('.jar'):
                dest=work/Path(entry).name;dest.write_bytes(z.read(entry));jars.append(str(dest))
        source=work/'FixtureHash.java'
        source.write_text('import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; class FixtureHash {public static void main(String[] a) throws Exception {String p=new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();System.out.print(new BCryptPasswordEncoder(12).encode(p));}}',encoding='utf-8')
        r=subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/java.exe'),'--class-path',os.pathsep.join(jars),str(source)],input=PASSWORD,text=True,capture_output=True,check=True)
        hashed=r.stdout.strip();assert hashed.startswith('$2') and len(hashed)==60;return hashed

def main():
    hashed=hash_password()
    sql(f"INSERT dbo.usuarios(nombres,apellidos,correo,password_hash,rol_codigo) VALUES(N'Prueba',N'{PREFIX}',N'{ADMIN}','{hashed}','ADMINISTRADOR')")
    admin=Client();expect(admin.login(ADMIN),200,'administrador ficticio inicia sesión');admin.csrf()
    profile=expect(admin.call('GET','/api/auth/me'),200,'perfil');aid=profile['id']
    users=[]
    for suffix,role in [('teacher','DOCENTE_TUTOR'),('other','DOCENTE_TUTOR'),('conv','CONVIVENCIA')]:
        email=PREFIX+'_'+suffix+'@example.invalid'
        u=expect(admin.call('POST','/api/users',dict(nombres='Prueba',apellidos=suffix,correo=email,password=PASSWORD,rol=role)),201,'crear '+suffix)
        c=Client();expect(c.login(email),200,'login '+suffix);c.csrf();users.append((u,c))
    tutor,teacher=users[0];other,outsider=users[1]
    baseline=expect(admin.call('GET',f'/api/reports?year={date.today().year}'),200,'reporte base')
    payload=dict(fechaIncidente=(date.today()-timedelta(days=10)).isoformat(),lugar=PREFIX+' Patio',tipoCodigo='AGRESION_VERBAL',descripcion='Incidente ficticio para pruebas de seguimiento.',responsableId=tutor['id'],estudiantes=[dict(nuevoEstudiante=dict(nombres=PREFIX,apellidos='Ficticio',grado=2,seccion='A'),participacion='AFECTADO')])
    detail=expect(admin.call('POST','/api/cases',payload),201,'caso para seguimiento');cid=detail['caso']['id']
    timeline=expect(teacher.call('GET',f'/api/cases/{cid}/followups'),200,'docente asignado ve seguimiento')
    assert timeline['puedeEditar'] and len(timeline['historial'])==0
    expect(outsider.call('GET',f'/api/cases/{cid}/followups'),404,'historial ajeno bloqueado')
    expect(teacher.call('GET','/api/users/directory'),403,'directorio solo administrador')
    change=dict(version=timeline['version'],accion='Primera atención ficticia.',fechaAccion=date.today().isoformat(),estado='EN_EVALUACION',proximaFecha=(date.today()+timedelta(days=2)).isoformat())
    expect(teacher.call('POST',f'/api/cases/{cid}/followups',change,csrf=False),403,'CSRF requerido para seguimiento')
    expect(teacher.call('POST',f'/api/cases/{cid}/followups',{**change,'estado':'CERRADO'}),400,'no se saltan estados')
    expect(teacher.call('POST',f'/api/cases/{cid}/followups',{**change,'fechaAccion':(date.today()+timedelta(days=1)).isoformat()}),400,'fecha futura rechazada')
    assert sql(f'SELECT COUNT(*) FROM dbo.seguimientos WHERE caso_id={cid}')=='0'
    timeline=expect(teacher.call('POST',f'/api/cases/{cid}/followups',change),201,'guarda acción, estado y próxima fecha')
    assert timeline['estado']=='EN_EVALUACION' and len(timeline['historial'])==1
    expect(teacher.call('POST',f'/api/cases/{cid}/followups',change),409,'versión repetida rechazada')
    assert sql(f'SELECT COUNT(*) FROM dbo.seguimientos WHERE caso_id={cid}')=='1'
    upcoming=expect(teacher.call('GET','/api/alerts?q='+PREFIX),200,'alertas del docente')
    assert any(a['id']==cid and a['clase']=='PROXIMA' for a in upcoming['resultados']['items'])
    # Simula el paso del tiempo en el caso ficticio; no cambia casos ajenos.
    sql(f"UPDATE dbo.casos SET proximo_seguimiento=DATEADD(day,-1,CONVERT(date,GETDATE())) WHERE id={cid} AND registrado_por={aid}")
    overdue=expect(teacher.call('GET','/api/alerts?clase=VENCIDA&q='+PREFIX),200,'vencimiento real')
    assert overdue['resultados']['total']==1
    sql(f"UPDATE dbo.casos SET proximo_seguimiento=NULL WHERE id={cid} AND registrado_por={aid}")
    timeline=expect(teacher.call('GET',f'/api/cases/{cid}/followups'),200,'recarga tras cambio')
    timeline=expect(teacher.call('POST',f'/api/cases/{cid}/followups',{**change,'version':timeline['version'],'estado':'EN_SEGUIMIENTO','proximaFecha':None}),201,'pasa a seguimiento')
    timeline=expect(teacher.call('POST',f'/api/cases/{cid}/followups',{**change,'version':timeline['version'],'estado':'CERRADO','proximaFecha':None}),201,'cierra caso con historial')
    assert not timeline['puedeEditar'] and len(timeline['historial'])==3
    expect(teacher.call('POST',f'/api/cases/{cid}/followups',{**change,'version':timeline['version']}),409,'caso cerrado no se edita')
    assert expect(teacher.call('GET','/api/alerts?q='+PREFIX),200,'cerrado fuera de alertas')['resultados']['total']==0
    # Casos abiertos para probar las pantallas y filtros.
    open_case=expect(admin.call('POST','/api/cases',{**payload,'responsableId':aid}),201,'caso abierto para navegador')
    assigned=expect(admin.call('POST','/api/cases',payload),201,'caso pendiente del docente')
    unassigned=expect(teacher.call('GET',f"/api/cases/{open_case['caso']['id']}/followups"),404,'docente ajeno no consulta')
    report=expect(admin.call('GET',f'/api/reports?year={date.today().year}'),200,'estadísticas actualizadas')
    assert report['total']==baseline['total']+3 and report['cerrados']==baseline['cerrados']+1
    assert sum(t['total'] for t in report['tipos'])==report['total']==sum(s['total'] for s in report['estados'])==sum(m['registrados'] for m in report['meses'])
    expect(admin.call('GET','/api/reports?year=0'),400,'año inválido rechazado')
    expect(teacher.call('GET','/api/alerts?clase=OTRA'),400,'filtro inválido rechazado')
    expect(admin.call('PUT',f'/api/users/{aid}',dict(nombres='Prueba',apellidos=PREFIX,correo=ADMIN,rol='DOCENTE_TUTOR')),409,'conserva rol de propia cuenta')
    edited=expect(admin.call('PUT',f"/api/users/{tutor['id']}",dict(nombres='Docente',apellidos='Editado',correo=tutor['correo'],rol='DOCENTE_TUTOR')),200,'editar datos de usuario')
    assert edited['apellidos']=='Editado'
    expect(admin.call('PATCH',f"/api/users/{tutor['id']}/status",{'activo':False}),200,'desactivar cuenta')
    expect(teacher.call('GET','/api/auth/me'),401,'revoca sesión docente')
    expect(admin.call('PATCH',f"/api/users/{tutor['id']}/status",{'activo':True}),200,'reactivar cuenta')
    directory=expect(admin.call('GET','/api/users/directory?q='+PREFIX),200,'directorio filtrado')
    assert directory['resultados']['total']==4
    assert all('password' not in str(u).lower() for u in directory['resultados']['items'])
    env=os.environ.copy();env.update(TEST_EMAIL=ADMIN,TEST_PASSWORD=PASSWORD,TEST_PREFIX=PREFIX,TEST_CASE_ID=str(open_case['caso']['id']),TEST_TEACHER=tutor['correo'],TEST_ASSIGNED_ID=str(assigned['caso']['id']))
    r=subprocess.run(['node','tests/remaining.cjs'],cwd=ROOT,env=env,timeout=240)
    assert r.returncode==0,'Prueba de navegador falló'
    print('PRUEBAS DE SEGUIMIENTO, ALERTAS, REPORTES Y USUARIOS CORRECTAS')

if __name__=='__main__':
    try:main()
    finally:
        sql(f"""SET XACT_ABORT ON; BEGIN TRANSACTION;
        DELETE s FROM dbo.seguimientos s JOIN dbo.casos c ON c.id=s.caso_id JOIN dbo.usuarios u ON u.id=c.registrado_por WHERE LEFT(u.correo,{len(PREFIX)})=N'{PREFIX}';
        DELETE ce FROM dbo.caso_estudiantes ce JOIN dbo.casos c ON c.id=ce.caso_id JOIN dbo.usuarios u ON u.id=c.registrado_por WHERE LEFT(u.correo,{len(PREFIX)})=N'{PREFIX}';
        DELETE c FROM dbo.casos c JOIN dbo.usuarios u ON u.id=c.registrado_por WHERE LEFT(u.correo,{len(PREFIX)})=N'{PREFIX}';
        DELETE FROM dbo.estudiantes WHERE nombres=N'{PREFIX}';
        DELETE FROM dbo.usuarios WHERE LEFT(correo,{len(PREFIX)})=N'{PREFIX}' AND correo LIKE N'%@example.invalid';
        COMMIT;""")
        print('Fixtures de esta prueba eliminados. Cuentas y casos existentes conservados.')
