"""Pruebas HTTP contra el backend local y SQL Server real.

Requiere servidor iniciado, JAVA_HOME, TEST_SQL_SERVER y sqlcmd disponible.
Usar una base de desarrollo sin administradores. Solo borra las cuentas ficticias
identificadas por el prefijo aleatorio de esta ejecución. No imprime contraseñas.
"""
import http.cookiejar
import json
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import urllib.error
import urllib.parse
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
LOCAL_CONFIG = ROOT / '.local'
if not (LOCAL_CONFIG / 'application-local.properties').exists():
    LOCAL_CONFIG = ROOT.parent / '.local'
CONFIG_ARGUMENT = '--spring.config.additional-location=' + LOCAL_CONFIG.as_uri() + '/'
BASE = os.environ.get("TEST_BASE_URL", "http://127.0.0.1:8080")
PREFIX = "test_" + secrets.token_hex(10)
ADMIN = PREFIX + "_admin@example.invalid"
PASSWORD = secrets.token_urlsafe(24)
SQLCMD = os.environ.get("SQLCMD_PATH") or shutil.which("sqlcmd")
SERVER = os.environ["TEST_SQL_SERVER"]


def sql(query):
    result = subprocess.run([SQLCMD, "-S", SERVER, "-E", "-C", "-l", "5", "-b",
                             "-d", "SistemaBullying", "-h", "-1", "-W", "-Q",
                             "SET NOCOUNT ON; SET ANSI_NULLS ON; SET QUOTED_IDENTIFIER ON; "
                             "SET ANSI_PADDING ON; SET ANSI_WARNINGS ON; SET CONCAT_NULL_YIELDS_NULL ON; "
                             "SET ARITHABORT ON; SET NUMERIC_ROUNDABORT OFF; " + query],
                            capture_output=True, text=True, errors="replace")
    if result.returncode:
        raise RuntimeError("Falló la consulta SQL de prueba: " + result.stderr[:500])
    return result.stdout.strip()


class Client:
    def __init__(self):
        self.cookies = http.cookiejar.CookieJar()
        self.http = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
        self.token = None

    def call(self, method, path, body=None, form=False, csrf=True):
        headers = {}
        data = None
        if body is not None:
            data = (urllib.parse.urlencode(body) if form else json.dumps(body)).encode()
            headers["Content-Type"] = "application/x-www-form-urlencoded" if form else "application/json"
        if csrf and self.token:
            headers["X-CSRF-TOKEN"] = self.token
        request = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
        try:
            response = self.http.open(request, timeout=15)
        except urllib.error.HTTPError as error:
            response = error
        with response:
            content = response.read().decode()
            return response.status, json.loads(content) if content else None, response.headers

    def csrf(self):
        status, body, _ = self.call("GET", "/api/auth/csrf")
        assert status == 200
        self.token = body["token"]

    def login(self, email, password=PASSWORD):
        self.csrf()
        return self.call("POST", "/api/auth/login", {"email": email, "password": password}, form=True)


def expect(result, status, name):
    assert result[0] == status, f"{name}: esperado {status}, recibido {result[0]}"
    print("OK:", name)
    return result[1]


def run():
    assert sql("SELECT COUNT(*) FROM dbo.usuarios WHERE activo=1 AND rol_codigo='ADMINISTRADOR'") == "0", \
        "Estas pruebas requieren una base de desarrollo sin administradores activos."
    java = str(Path(os.environ["JAVA_HOME"]) / "bin" / "java.exe")
    environment = os.environ.copy()
    environment.update(BOOTSTRAP_ADMIN="true", ADMIN_EMAIL=ADMIN, ADMIN_PASSWORD=PASSWORD,
                       ADMIN_NOMBRES="Prueba", ADMIN_APELLIDOS="Temporal")
    boot = subprocess.run([java, "-jar", "target/sistema-bullying-backend-0.0.1-SNAPSHOT.jar",
                           "--spring.profiles.active=local", CONFIG_ARGUMENT],
                          cwd=ROOT, env=environment, capture_output=True, text=True, errors="replace", timeout=45)
    if boot.returncode:
        (LOCAL_CONFIG / "bootstrap-test.log").write_text(boot.stdout + boot.stderr, encoding="utf-8")
        raise RuntimeError("Falló la creación del administrador ficticio; revisar .local/bootstrap-test.log")

    anonymous = Client()
    expect(anonymous.call("GET", "/api/auth/me"), 401, "consulta anónima rechazada")
    expect(anonymous.call("POST", "/api/auth/login", {"email": ADMIN, "password": PASSWORD}, form=True),
           403, "login sin CSRF rechazado")
    admin = Client()
    admin.csrf()
    previous = {c.name: c.value for c in admin.cookies}.get("JSESSIONID")
    login = admin.call("POST", "/api/auth/login", {"email": " " + ADMIN.upper() + " ", "password": PASSWORD}, form=True)
    expect(login, 200, "login y normalización de correo")
    current = {c.name: c.value for c in admin.cookies}.get("JSESSIONID")
    assert previous and current and previous != current, "La sesión no se renovó al autenticar."
    assert "HttpOnly" in login[2].get("Set-Cookie", "")
    profile = expect(admin.call("GET", "/api/auth/me"), 200, "perfil autenticado")
    assert profile["rol"] == "ADMINISTRADOR" and "password" not in json.dumps(profile).lower()
    admin.csrf()
    expect(admin.call("PATCH", f'/api/users/{profile["id"]}/status', {"activo": False}),
           409, "administrador no puede desactivarse a sí mismo")

    invalid = Client()
    failure = expect(invalid.login(ADMIN, "incorrecta"), 401, "contraseña incorrecta rechazada")
    assert expect(invalid.login(PREFIX + "_ausente@example.invalid"), 401,
                  "usuario inexistente rechazado") == failure
    expect(invalid.login(ADMIN, "á" * 40), 401, "contraseña excesiva rechazada sin error interno")

    tutor_email = PREFIX + "_docente@example.invalid"
    payload = dict(nombres="Docente", apellidos="Temporal", correo=tutor_email, password=PASSWORD, rol="DOCENTE_TUTOR")
    expect(admin.call("POST", "/api/users", payload, csrf=False), 403, "alta sin CSRF rechazada")
    tutor_profile = expect(admin.call("POST", "/api/users", payload), 201, "alta de usuario persistida")
    assert "password" not in json.dumps(tutor_profile).lower()
    expect(admin.call("POST", "/api/users", payload), 409, "correo duplicado rechazado")
    expect(admin.call("POST", "/api/users", {**payload, "rol": "SUPERADMIN"}), 400, "rol inválido rechazado")
    expect(admin.call("POST", "/api/users", {**payload, "password": "corta"}), 400, "contraseña corta rechazada")
    expect(admin.call("POST", "/api/users", {**payload, "password": "á" * 40}), 400, "límite BCrypt medido en bytes")
    stored = sql(f"SELECT password_hash FROM dbo.usuarios WHERE correo=N'{tutor_email}'")
    assert stored.startswith("$2") and stored != PASSWORD
    print("OK: contraseña guardada como hash BCrypt")

    tutor = Client()
    expect(tutor.login(tutor_email), 200, "docente inicia sesión")
    expect(tutor.call("GET", "/api/users"), 403, "docente no puede listar usuarios")
    tutor.csrf()
    expect(tutor.call("POST", "/api/users", payload), 403, "docente no puede crear usuarios")
    expect(admin.call("PATCH", f'/api/users/{tutor_profile["id"]}/status', {"activo": False}),
           200, "administrador desactiva al docente")
    expect(tutor.call("GET", "/api/auth/me"), 401, "sesión existente revocada al desactivar")
    expect(tutor.login(tutor_email), 401, "cuenta inactiva no puede iniciar sesión")
    expect(admin.call("PATCH", f'/api/users/{tutor_profile["id"]}/status', {"activo": True}),
           200, "reactivación de cuenta")
    expect(tutor.login(tutor_email), 200, "cuenta reactivada puede iniciar sesión")
    tutor.csrf()
    expect(tutor.call("POST", "/api/auth/logout"), 204, "cierre de sesión")
    expect(tutor.call("GET", "/api/auth/me"), 401, "sesión cerrada no reutilizable")
    listing = expect(admin.call("GET", "/api/users?limit=10"), 200, "listado de usuarios por administrador")
    assert all("password" not in json.dumps(row).lower() for row in listing)
    expect(admin.call("GET", "/api/users?limit=1000"), 400, "paginación limitada")

    limiter = Client()
    limiter.csrf()
    statuses = [limiter.call("POST", "/api/auth/login", {"email": ADMIN, "password": "incorrecta"}, form=True)[0]
                for _ in range(16)]
    assert 429 in statuses and all(s in (401, 429) for s in statuses)
    print("OK: límite de intentos de login")


if __name__ == "__main__":
    try:
        run()
        print("PRUEBAS HTTP Y SQL CORRECTAS")
    finally:
        # Prefijo aleatorio generado por esta ejecución; no toca usuarios del proyecto.
        sql(f"DELETE FROM dbo.usuarios WHERE LEFT(correo,{len(PREFIX)})=N'{PREFIX}' AND correo LIKE N'%@example.invalid';")
        print("Cuentas ficticias de esta ejecución eliminadas.")
