"""Prueba local en una base de desarrollo vacía; limpia exclusivamente sus datos."""
import os
from pathlib import Path
import subprocess
import sys

ROOT=Path(__file__).resolve().parents[1]
BACKEND=ROOT.parent/'sistema-bullying-backend'
sys.path.insert(0,str(BACKEND/'tests'))
from integration_auth import sql, PREFIX, ADMIN, PASSWORD

def run():
    assert sql('SELECT COUNT(*) FROM dbo.usuarios')=='0', 'Usar una base de desarrollo sin usuarios.'
    environment=os.environ.copy()
    environment.update(BOOTSTRAP_ADMIN='true',ADMIN_EMAIL=ADMIN,ADMIN_PASSWORD=PASSWORD,
        ADMIN_NOMBRES='Prueba',ADMIN_APELLIDOS='Temporal')
    boot=subprocess.run([str(Path(os.environ['JAVA_HOME'])/'bin/java.exe'),'-jar',
        'target/sistema-bullying-backend-0.0.1-SNAPSHOT.jar','--spring.profiles.active=local',
        '--spring.config.additional-location=file:./.local/'],cwd=BACKEND,env=environment,
        capture_output=True,text=True,errors='replace',timeout=60)
    assert boot.returncode==0,'No se pudo crear la cuenta ficticia'
    environment=os.environ.copy()
    environment.update(TEST_EMAIL=ADMIN,TEST_PASSWORD=PASSWORD,TEST_PREFIX=PREFIX)
    result=subprocess.run(['node','tests/e2e.cjs'],cwd=ROOT,env=environment,timeout=180)
    if result.returncode:raise RuntimeError('Fallaron las pruebas de navegador')
    assert sql(f"SELECT COUNT(*) FROM dbo.casos c JOIN dbo.usuarios u ON u.id=c.registrado_por WHERE u.correo=N'{ADMIN}'")=='1'
    print('OK: persistencia confirmada directamente en SQL Server')

try:
    run()
finally:
    sql(f"""SET XACT_ABORT ON; BEGIN TRANSACTION;
    DELETE ce FROM dbo.caso_estudiantes ce JOIN dbo.casos c ON c.id=ce.caso_id
      JOIN dbo.usuarios u ON u.id=c.registrado_por WHERE u.correo=N'{ADMIN}';
    DELETE c FROM dbo.casos c JOIN dbo.usuarios u ON u.id=c.registrado_por WHERE u.correo=N'{ADMIN}';
    DELETE FROM dbo.estudiantes WHERE nombres=N'{PREFIX}';
    DELETE FROM dbo.usuarios WHERE LEFT(correo,{len(PREFIX)})=N'{PREFIX}' AND correo LIKE N'%@example.invalid'; COMMIT;""")
    print('Datos ficticios eliminados; capturas locales en .local/e2e.')
