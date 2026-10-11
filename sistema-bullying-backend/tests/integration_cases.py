"""Pruebas de casos en base de desarrollo; elimina solo sus fixtures aleatorios."""
from integration_auth import *
from datetime import date, timedelta
from copy import deepcopy


def run_cases():
    assert sql("SELECT COUNT(*) FROM dbo.usuarios WHERE activo=1 AND rol_codigo='ADMINISTRADOR'") == "0"
    environment = os.environ.copy()
    environment.update(BOOTSTRAP_ADMIN="true", ADMIN_EMAIL=ADMIN, ADMIN_PASSWORD=PASSWORD,
                       ADMIN_NOMBRES="Prueba", ADMIN_APELLIDOS="Temporal")
    result = subprocess.run([str(Path(os.environ['JAVA_HOME']) / 'bin/java.exe'), '-jar',
        'target/sistema-bullying-backend-0.0.1-SNAPSHOT.jar', '--spring.profiles.active=local',
        '--spring.config.additional-location=file:./.local/'], cwd=ROOT, env=environment,
        capture_output=True, text=True, errors='replace', timeout=45)
    assert result.returncode == 0, 'No se pudo crear el administrador de prueba'
    admin = Client()
    expect(admin.login(ADMIN), 200, 'login administrador')
    admin.csrf()
    aid = expect(admin.call('GET', '/api/auth/me'), 200, 'perfil administrador')['id']
    users = []
    for suffix, role in [('uno','DOCENTE_TUTOR'), ('dos','DOCENTE_TUTOR'), ('conv','CONVIVENCIA')]:
        email = PREFIX + '_' + suffix + '@example.invalid'
        profile = expect(admin.call('POST', '/api/users', dict(nombres='Prueba', apellidos=suffix,
            correo=email, password=PASSWORD, rol=role)), 201, 'alta ' + suffix)
        client = Client()
        expect(client.login(email), 200, 'login ' + suffix)
        client.csrf()
        users.append((client, profile['id']))
    tutor, tid = users[0]
    other, oid = users[1]
    conv, _ = users[2]
    base = dict(fechaIncidente=date.today().isoformat(), lugar=PREFIX + ' patio 50%_[',
        tipoCodigo='AGRESION_VERBAL', descripcion='Descripción ficticia de prueba',
        observaciones='Solo prueba automática', responsableId=tid,
        estudiantes=[dict(nuevoEstudiante=dict(nombres=PREFIX, apellidos='Ficticio', grado=2,
            seccion='A'), participacion='AFECTADO')])
    expect(Client().call('GET','/api/cases'),401,'casos requieren sesión')
    expect(tutor.call('POST','/api/cases',base,csrf=False),403,'alta requiere CSRF')
    catalogs=expect(tutor.call('GET','/api/cases/catalogs'),200,'catálogos disponibles')
    assert len(catalogs['tipos']) == 6 and len(catalogs['estados']) == 4
    candidates=expect(tutor.call('GET','/api/cases/responsibles'),200,'responsables disponibles')
    assert any(p['id']==tid for p in candidates) and all('correo' not in p for p in candidates)
    first=expect(tutor.call('POST','/api/cases',base),201,'crear caso con estudiante nuevo')
    cid=first['caso']['id']; sid=first['estudiantes'][0]['estudiante']['id']
    assert first['caso']['estadoCodigo']=='REPORTADO' and first['caso']['registradoPor']==tid
    assert sql(f'SELECT COUNT(*) FROM dbo.casos WHERE id={cid}')=='1'
    expect(tutor.call('GET',f'/api/cases/{cid}'),200,'docente consulta caso propio')
    expect(other.call('GET',f'/api/cases/{cid}'),404,'otro docente no ve detalle')
    expect(conv.call('GET',f'/api/cases/{cid}'),200,'convivencia consulta cualquier caso')
    expect(admin.call('GET',f'/api/cases/{cid}'),200,'administrador consulta cualquier caso')
    second=deepcopy(base)
    second['responsableId']=oid
    second['estudiantes']=[dict(estudianteId=sid, participacion='AFECTADO')]
    created=expect(tutor.call('POST','/api/cases',second),201,'reutilizar estudiante y asignar otro docente')
    expect(other.call('GET',f"/api/cases/{created['caso']['id']}"),200,'responsable puede consultar caso asignado')
    unrelated=deepcopy(base); unrelated['responsableId']=aid
    hidden=expect(admin.call('POST','/api/cases',unrelated),201,'caso de otro ámbito')
    hidden_sid=hidden['estudiantes'][0]['estudiante']['id']
    prefix_query=urllib.parse.urlencode({'q':PREFIX})
    rows=expect(tutor.call('GET','/api/cases?'+prefix_query),200,'listado filtrado por docente')
    assert rows['total']==2 and hidden['caso']['id'] not in [r['id'] for r in rows['items']]
    rows=expect(admin.call('GET','/api/cases?'+prefix_query+'&limit=1&offset=1'),200,'paginación con total')
    assert rows['total']==3 and len(rows['items'])==1
    rows=expect(tutor.call('GET','/api/students?'+prefix_query),200,'estudiantes limitados al ámbito')
    assert {s['id'] for s in rows['items']}=={sid}
    for q, total in [('50%_[',3), ("' OR 1=1 --",0)]:
        rows=expect(admin.call('GET','/api/cases?'+urllib.parse.urlencode({'q':q})),200,'búsqueda literal y parametrizada')
        assert rows['total']==total
    rows=expect(admin.call('GET','/api/cases?'+prefix_query+'&estado=CERRADO'),200,'filtro de estado')
    assert rows['total']==0
    rows=expect(admin.call('GET','/api/cases?'+prefix_query+'&tipo=AGRESION_VERBAL&desde='+base['fechaIncidente']+'&hasta='+base['fechaIncidente']),200,'filtros de tipo y fechas')
    assert rows['total']==3
    for query in ['limit=101','offset=-1','estado=INVENTADO','tipo=OTRO','desde=2026-10-10&hasta=2026-01-01']:
        expect(tutor.call('GET','/api/cases?'+query),400,'rechaza filtro '+query)
    before=sql('SELECT (SELECT COUNT(*) FROM dbo.casos),(SELECT COUNT(*) FROM dbo.estudiantes),(SELECT COUNT(*) FROM dbo.caso_estudiantes)')
    variants=[dict(fechaIncidente=(date.today()+timedelta(days=1)).isoformat()),dict(lugar=' '),
        dict(tipoCodigo='OTRO'),dict(responsableId=9223372036854775807),dict(estudiantes=[]),
        dict(descripcion='x'*4001),dict(estudiantes=[dict(estudianteId=hidden_sid,participacion='AFECTADO')]),
        dict(estudiantes=[dict(estudianteId=sid,participacion='TESTIGO')]),
        dict(estudiantes=[dict(estudianteId=sid,participacion='AFECTADO')]*2),
        dict(estudiantes=base['estudiantes']+[dict(estudianteId=9223372036854775807,participacion='TESTIGO')])]
    for i, changes in enumerate(variants):
        expect(tutor.call('POST','/api/cases',{**base,**changes}),400,'validación de alta '+str(i+1))
    after=sql('SELECT (SELECT COUNT(*) FROM dbo.casos),(SELECT COUNT(*) FROM dbo.estudiantes),(SELECT COUNT(*) FROM dbo.caso_estudiantes)')
    assert before==after, 'Una solicitud inválida dejó datos parciales'
    print('OK: solicitudes inválidas no dejan casos, estudiantes ni relaciones parciales')
    expect(tutor.call('GET','/api/cases/9223372036854775807'),404,'caso inexistente')
    expect(tutor.call('DELETE',f'/api/cases/{cid}'),405,'borrado no implementado')


if __name__=='__main__':
    try:
        run_cases()
        print('PRUEBAS DE CASOS HTTP Y SQL CORRECTAS')
    finally:
        # Solo registros ligados a cuentas/estudiantes del prefijo aleatorio de esta ejecución.
        sql(f"""BEGIN TRANSACTION;
        DELETE ce FROM dbo.caso_estudiantes ce JOIN dbo.casos c ON c.id=ce.caso_id
          JOIN dbo.usuarios u ON u.id=c.registrado_por WHERE LEFT(u.correo,{len(PREFIX)})=N'{PREFIX}';
        DELETE c FROM dbo.casos c JOIN dbo.usuarios u ON u.id=c.registrado_por
          WHERE LEFT(u.correo,{len(PREFIX)})=N'{PREFIX}';
        DELETE FROM dbo.estudiantes WHERE nombres=N'{PREFIX}';
        DELETE FROM dbo.usuarios WHERE LEFT(correo,{len(PREFIX)})=N'{PREFIX}' AND correo LIKE N'%@example.invalid';
        COMMIT;""")
        print('Datos ficticios de esta ejecución eliminados.')
