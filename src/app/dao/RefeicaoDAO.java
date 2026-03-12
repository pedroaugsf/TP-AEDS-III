package app.dao;

import app.Arquivo;
import app.model.Refeicao;

import java.lang.reflect.Constructor;

public class RefeicaoDAO {
    private final Arquivo<Refeicao> arq;

    public RefeicaoDAO() throws Exception {
        Constructor<Refeicao> c = Refeicao.class.getConstructor();
        arq = new Arquivo<>("refeicao", c);
    }

    public int create(Refeicao r) throws Exception { return arq.create(r); }
    public Refeicao read(int id) throws Exception { return arq.read(id); }
    public boolean update(Refeicao r) throws Exception { return arq.update(r); }
    public boolean delete(int id) throws Exception { return arq.delete(id); }
    public void close() throws Exception { arq.close(); }
}
