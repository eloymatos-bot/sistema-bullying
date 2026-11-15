package pe.vallegrande.bullying.users;

public record Account(long id, String nombres, String apellidos, String correo,
                      String passwordHash, String rol, boolean activo) {
    public UserView view() { return new UserView(id, nombres, apellidos, correo, rol, activo); }

    public record UserView(long id, String nombres, String apellidos, String correo,
                           String rol, boolean activo) {}
}
