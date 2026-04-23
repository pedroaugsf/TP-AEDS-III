package app.dao;

import app.Arquivo;
import app.model.Alimento;

import java.lang.reflect.Constructor;
import java.util.List;

public class AlimentoDAO {
    private final Arquivo<Alimento> arq;

    public AlimentoDAO() throws Exception {
        Constructor<Alimento> c = Alimento.class.getConstructor();
        arq = new Arquivo<>("alimento", c);
    }

    public int create(Alimento a) throws Exception { return arq.create(a); }
    public Alimento read(int id) throws Exception { return arq.read(id); }
    public boolean exists(int id) throws Exception { return arq.exists(id); }
    public boolean update(Alimento a) throws Exception { return arq.update(a); }
    public boolean delete(int id) throws Exception { return arq.delete(id); }
    public List<Alimento> listar() throws Exception { return arq.listar(); }
    public void close() throws Exception { arq.close(); }
    public String descricaoIndice() { return arq.descricaoIndice(); }
}
