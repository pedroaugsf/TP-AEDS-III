package app.dao;

import app.Arquivo;
import app.model.Consumo;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * ConsumoDAO usa o {@link Arquivo} (com índice primário hash) e mantém um
 * <b>índice 1:N</b> em Hash Extensível ({@link HashExtensivel} de {@link ParIDID})
 * mapeando refeicaoId -&gt; consumoId.
 *
 * Permite, em O(1) médio, listar todos os consumos de uma refeição sem varrer
 * linearmente o arquivo de consumos.
 */
public class ConsumoDAO {

    private static final int SLOTS_BUCKET_REL = 16;

    private final Arquivo<Consumo> arq;
    private final HashExtensivel<ParIDID> indiceRefeicaoConsumo;

    public ConsumoDAO() throws Exception {
        Constructor<Consumo> c = Consumo.class.getConstructor();
        arq = new Arquivo<>("consumo", c);

        Constructor<ParIDID> ctor = ParIDID.class.getConstructor();
        indiceRefeicaoConsumo = new HashExtensivel<>(
                "consumo_por_refeicao", SLOTS_BUCKET_REL, ctor);

        reconstruirIndice1NSeNecessario();
    }

    private void reconstruirIndice1NSeNecessario() throws Exception {
        List<Consumo> todos = arq.listar();
        if (todos.isEmpty()) return;
        Consumo primeiro = todos.get(0);
        List<ParIDID> existentes = indiceRefeicaoConsumo.read(primeiro.getRefeicaoId());
        for (ParIDID p : existentes) {
            if (p.getChave2() == primeiro.getId()) return; // já indexado
        }
        for (Consumo c : todos) {
            indiceRefeicaoConsumo.create(new ParIDID(c.getRefeicaoId(), c.getId()));
        }
    }

    public int create(Consumo c) throws Exception {
        int id = arq.create(c);
        indiceRefeicaoConsumo.create(new ParIDID(c.getRefeicaoId(), id));
        return id;
    }

    public Consumo read(int id) throws Exception { return arq.read(id); }
    public boolean exists(int id) throws Exception { return arq.exists(id); }

    public boolean update(Consumo cNovo) throws Exception {
        Consumo antigo = arq.read(cNovo.getId());
        if (antigo == null) return false;
        boolean ok = arq.update(cNovo);
        if (!ok) return false;
        if (antigo.getRefeicaoId() != cNovo.getRefeicaoId()) {
            indiceRefeicaoConsumo.delete(new ParIDID(antigo.getRefeicaoId(), antigo.getId()));
            indiceRefeicaoConsumo.create(new ParIDID(cNovo.getRefeicaoId(), cNovo.getId()));
        }
        return true;
    }

    public boolean delete(int id) throws Exception {
        Consumo c = arq.read(id);
        if (c == null) return false;
        boolean ok = arq.delete(id);
        if (ok) indiceRefeicaoConsumo.delete(new ParIDID(c.getRefeicaoId(), id));
        return ok;
    }

    public List<Consumo> listar() throws Exception { return arq.listar(); }

    /** Lista os consumos de uma refeição via índice Hash Extensível 1:N. */
    public List<Consumo> listarPorRefeicao(int refeicaoId) throws Exception {
        List<ParIDID> pares = indiceRefeicaoConsumo.read(refeicaoId);
        List<Consumo> result = new ArrayList<>(pares.size());
        for (ParIDID p : pares) {
            Consumo c = arq.read(p.getChave2());
            if (c != null) result.add(c);
        }
        return result;
    }

    /** Remove em cascata todos os consumos de uma refeição (usado quando a refeição é deletada). */
    public int removerPorRefeicao(int refeicaoId) throws Exception {
        List<Consumo> consumos = listarPorRefeicao(refeicaoId);
        int n = 0;
        for (Consumo c : consumos) {
            if (delete(c.getId())) n++;
        }
        return n;
    }

    public void close() throws Exception {
        arq.close();
        indiceRefeicaoConsumo.close();
    }

    public String descricaoIndices() {
        return "primario=" + arq.descricaoIndice() + " | 1:N=" + indiceRefeicaoConsumo.toString();
    }
}
