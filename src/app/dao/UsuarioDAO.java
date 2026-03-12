package app.dao;

import app.Arquivo;
import app.model.Usuario;

import java.lang.reflect.Constructor;

public class UsuarioDAO {
    private final Arquivo<Usuario> arq;

    public UsuarioDAO() throws Exception {
        Constructor<Usuario> c = Usuario.class.getConstructor();
        arq = new Arquivo<>("usuario", c);
    }

    public int create(Usuario u) throws Exception { return arq.create(u); }
    public Usuario read(int id) throws Exception { return arq.read(id); }
    public boolean update(Usuario u) throws Exception { return arq.update(u); }
    public boolean delete(int id) throws Exception { return arq.delete(id); }
    public void close() throws Exception { arq.close(); }
}
