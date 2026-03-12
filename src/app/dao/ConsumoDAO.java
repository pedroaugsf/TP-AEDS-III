package app.dao;

import app.Arquivo;
import app.model.Consumo;

import java.lang.reflect.Constructor;

public class ConsumoDAO {
    private final Arquivo<Consumo> arq;

    public ConsumoDAO() throws Exception {
        Constructor<Consumo> c = Consumo.class.getConstructor();
        arq = new Arquivo<>("consumo", c);
    }

    public int create(Consumo c) throws Exception { return arq.create(c); }
    public Consumo read(int id) throws Exception { return arq.read(id); }
    public boolean update(Consumo c) throws Exception { return arq.update(c); }
    public boolean delete(int id) throws Exception { return arq.delete(id); }
    public void close() throws Exception { arq.close(); }
}
