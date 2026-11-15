"""API de estudiantes y SQL reales; conserva datos ajenos a sus fixtures."""
import os
import subprocess
import tempfile
import zipfile
from datetime import datetime
from pathlib import Path
from zoneinfo import ZoneInfo
from integration_auth import sql, Client, PREFIX, ADMIN, PASSWORD, ROOT, expect


def password_hash():
    with tempfile.TemporaryDirectory() as folder, zipfile.ZipFile(ROOT/'target/sistema-bullying-backend-0.0.1-SNAPSHOT.jar') as z:
        work=Path(folder); jars=[]
        for entry in z.namelist():
            if entry.startswith('BOOT-INF/lib/') and Path(entry).name.startswith(('spring-security-crypto-','commons-logging-')) and entry.endswith('.jar'):
                dest=work/Path(entry).name;dest.write_bytes(z.read(entry));jars.append(str(dest))
        source=work/'FixtureHash.java'
        source.write_text('import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; class FixtureHash { public static void main(String[] a) throws Exception {String p=new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();System.out.print(new BCryptPasswordEncoder(12).encode(p));}}',encoding='utf-8')
        r=subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/java.exe'),'--class-path',os.pathsep.join(jars),str(source)],input=PASSWORD,text=True,capture_output=True,check=True)
        hashed=r.stdout.strip();assert len(hashed)==60 and hashed.startswith('$2');return hashed


def run():
    baseline=sql('SELECT COUNT(*) FROM dbo.casos')
    sql(f"INSERT dbo.usuarios(nombres,apellidos,correo,password_hash,rol_codigo) VALUES(N'Prueba',N'{PREFIX}',N'{ADMIN}','{password_hash()}','ADMINISTRADOR')")
    anon=Client();expect(anon.call('GET','/api/students/directory'),401,'directorio requiere sesion')
    admin=Client();expect(admin.login(ADMIN),200,'login ficticio');admin.csrf()
    aid=expect(admin.call('GET','/api/auth/me'),200,'perfil ficticio')['id']
    payload=dict(nombres=PREFIX,apellidos='Ficticio',grado=2,seccion=' a ')
    expect(admin.call('POST','/api/students',payload,csrf=False),403,'alta exige CSRF')
    before=sql('SELECT COUNT(*) FROM dbo.estudiantes')
    for invalid in [dict(payload,grado=0),dict(payload,grado=6),dict(payload,grado=2.5),dict(payload,nombres=' '),dict(payload,seccion='x'*11),dict(payload,apellidos='x\n')]:
        expect(admin.call('POST','/api/students',invalid),400,'alta invalida sin persistencia')
    assert sql('SELECT COUNT(*) FROM dbo.estudiantes')==before
    status,student,headers=admin.call('POST','/api/students',payload)
    assert status==201 and headers['Location'].endswith('/'+str(student['id']))
    sid=student['id'];assert student['seccion']=='A' and len(student['version'])==16
    assert expect(admin.call('GET',f'/api/students/{sid}'),200,'alta persiste al consultar')==student
    for suffix,role in [('conv','CONVIVENCIA'),('teacher','DOCENTE_TUTOR')]:
        u=expect(admin.call('POST','/api/users',dict(nombres='Prueba',apellidos=PREFIX,correo=PREFIX+'_'+suffix+'@example.invalid',password=PASSWORD,rol=role)),201,'crear rol '+suffix)
        c=Client();expect(c.login(u['correo']),200,'login '+suffix);c.csrf()
        if suffix=='conv': conv=c
        else: teacher=c;tid=u['id']
    expect(teacher.call('POST','/api/students',payload),403,'docente no administra directorio')
    expect(teacher.call('GET',f'/api/students/{sid}'),404,'detalle ajeno oculto')
    assert expect(teacher.call('GET','/api/students/directory?q='+PREFIX),200,'directorio docente limitado')['total']==0
    today=datetime.now(ZoneInfo('America/Lima')).date().isoformat()
    case=expect(admin.call('POST','/api/cases',dict(fechaIncidente=today,lugar=PREFIX,tipoCodigo='AGRESION_VERBAL',descripcion='Caso ficticio de integridad.',responsableId=tid,estudiantes=[dict(estudianteId=sid,participacion='AFECTADO')])),201,'vincular alumno a caso')
    cid=case['caso']['id']
    expect(teacher.call('GET',f'/api/students/{sid}'),200,'docente asignado consulta alumno')
    assert expect(teacher.call('GET','/api/students/directory?q='+PREFIX),200,'directorio asignado')['total']==1
    edit=dict(payload,grado=3,seccion=' b ',version=student['version'])
    expect(teacher.call('PUT',f'/api/students/{sid}',edit),403,'docente no edita alumno asignado')
    expect(admin.call('PUT',f'/api/students/{sid}',edit,csrf=False),403,'edicion exige CSRF')
    updated=expect(conv.call('PUT',f'/api/students/{sid}',edit),200,'convivencia edita alumno')
    assert updated['version']!=student['version'] and updated['seccion']=='B' and updated['id']==sid
    expect(admin.call('PUT',f'/api/students/{sid}',dict(edit,nombres='Obsoleto')),409,'version antigua no sobrescribe')
    expect(admin.call('PUT',f'/api/students/{sid}',dict(edit,version='incorrecta')),400,'version invalida rechazada')
    expect(admin.call('PUT',f'/api/students/{sid}',dict(edit,grado=6,version=updated['version'])),400,'edicion invalida rechazada')
    assert expect(admin.call('GET',f'/api/students/{sid}'),200,'sin cambios parciales')==updated
    page=expect(admin.call('GET','/api/students/directory?q='+PREFIX+'&grado=3&seccion=b&limit=1'),200,'filtros combinados y paginacion')
    assert page['total']==1 and page['items'][0]['id']==sid
    assert expect(admin.call('GET','/api/students/directory?q='+PREFIX+'&grado=2'),200,'filtro excluye grado anterior')['total']==0
    assert expect(admin.call('GET','/api/students/directory?q=%25'+PREFIX),200,'busqueda literal de comodines')['total']==0
    for query in ['limit=0','offset=-1','grado=6','seccion='+'x'*11,'q='+'x'*101]:
        expect(admin.call('GET','/api/students/directory?'+query),400,'filtro invalido rechazado')
    detail=expect(admin.call('GET',f'/api/cases/{cid}'),200,'caso conserva participacion')
    assert detail['estudiantes'][0]['estudiante']['id']==sid and detail['estudiantes'][0]['estudiante']['grado']==3
    assert sql(f'SELECT COUNT(*) FROM dbo.caso_estudiantes WHERE caso_id={cid} AND estudiante_id={sid}')=='1'
    assert expect(admin.call('GET','/api/students?q='+PREFIX),200,'selector anterior compatible')['total']==1
    # Alumnos homonimos no se fusionan ni se rechazan por una falsa clave de nombre.
    second=expect(conv.call('POST','/api/students',payload),201,'convivencia crea homonimo independiente')
    assert second['id']!=sid
    assert expect(teacher.call('GET','/api/students/directory?q='+PREFIX),200,'homonimo ajeno no filtrado al docente')['total']==1
    sql(f'UPDATE dbo.estudiantes SET activo=0 WHERE id={sid}')
    expect(teacher.call('GET',f'/api/students/{sid}'),404,'alumno inactivo oculto al docente')
    assert not expect(admin.call('GET',f'/api/students/{sid}'),200,'administrador conserva consulta inactivo')['activo']
    assert int(sql('SELECT COUNT(*) FROM dbo.casos'))==int(baseline)+1
    print('API ESTUDIANTES: persistencia, permisos, version, filtros e integridad correctos')


if __name__=='__main__':
    try: run()
    finally:
        sql(f"""SET XACT_ABORT ON; BEGIN TRANSACTION;
        DELETE s FROM dbo.seguimientos s JOIN dbo.casos c ON c.id=s.caso_id JOIN dbo.usuarios u ON u.id=c.registrado_por WHERE LEFT(u.correo,{len(PREFIX)})=N'{PREFIX}';
        DELETE ce FROM dbo.caso_estudiantes ce JOIN dbo.casos c ON c.id=ce.caso_id JOIN dbo.usuarios u ON u.id=c.registrado_por WHERE LEFT(u.correo,{len(PREFIX)})=N'{PREFIX}';
        DELETE c FROM dbo.casos c JOIN dbo.usuarios u ON u.id=c.registrado_por WHERE LEFT(u.correo,{len(PREFIX)})=N'{PREFIX}';
        DELETE FROM dbo.estudiantes WHERE nombres=N'{PREFIX}' AND NOT EXISTS(SELECT 1 FROM dbo.caso_estudiantes ce WHERE ce.estudiante_id=estudiantes.id);
        DELETE FROM dbo.usuarios WHERE LEFT(correo,{len(PREFIX)})=N'{PREFIX}' AND correo LIKE N'%@example.invalid';
        COMMIT;""")
        print('Fixtures propios retirados; datos anteriores conservados.')
