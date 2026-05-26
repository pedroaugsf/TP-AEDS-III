package app.dao;

import app.Arquivo;
import app.model.Alimento;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * AlimentoDAO — além do índice primário (Hash Extensível id -&gt; endereço),
 * mantém uma <b>Árvore B+</b> (nome -&gt; id) usada para a consulta
 * <em>ordenada</em> {@link #listarOrdenadoPorNome()} sem ordenação em memória.
 *
 * Justificativa do uso da B+: a estrutura é eficiente para varredura ordenada
 * e por faixa — as folhas estão encadeadas em ordem de chave, então o método
 * percorre apenas a lista de folhas e retorna os ids já ordenados.
 */
public class AlimentoDAO {

    private static final int ORDEM_BMAIS = 8;

    private final Arquivo<Alimento> arq;
    private final ArvoreBMais<ParNomeID> idxNome;

    public AlimentoDAO() throws Exception {
        Constructor<Alimento> c = Alimento.class.getConstructor();
        arq = new Arquivo<>("alimento", c);

        Constructor<ParNomeID> cp = ParNomeID.class.getConstructor();
        idxNome = new ArvoreBMais<>("alimento", ORDEM_BMAIS, cp);

        reconstruirIndiceNomeSeNecessario();
    }

    private void reconstruirIndiceNomeSeNecessario() throws Exception {
        List<Alimento> todos = arq.listar();
        if (todos.isEmpty()) return;
        List<ParNomeID> existentes = idxNome.listarTodos();
        if (existentes.size() >= todos.size()) return;
        for (Alimento a : todos) {
            ParNomeID novo = new ParNomeID(a.getNome(), a.getId());
            boolean existe = false;
            for (ParNomeID p : existentes) {
                if (p.compareTo(novo) == 0) { existe = true; break; }
            }
            if (!existe) idxNome.inserir(novo);
        }
    }

    public int create(Alimento a) throws Exception {
        int id = arq.create(a);
        idxNome.inserir(new ParNomeID(a.getNome(), id));
        return id;
    }

    public Alimento read(int id) throws Exception { return arq.read(id); }
    public boolean exists(int id) throws Exception { return arq.exists(id); }

    public boolean update(Alimento a) throws Exception {
        Alimento antigo = arq.read(a.getId());
        if (antigo == null) return false;
        boolean ok = arq.update(a);
        if (!ok) return false;
        // se o nome mudou, atualiza a B+: remove a chave antiga e insere a nova
        if (!equalsSafe(antigo.getNome(), a.getNome())) {
            idxNome.remover(new ParNomeID(antigo.getNome(), a.getId()));
            idxNome.inserir(new ParNomeID(a.getNome(), a.getId()));
        }
        return true;
    }

    public boolean delete(int id) throws Exception {
        Alimento a = arq.read(id);
        if (a == null) return false;
        boolean ok = arq.delete(id);
        if (ok) idxNome.remover(new ParNomeID(a.getNome(), id));
        return ok;
    }

    /** Listagem em ordem de inserção (varredura sequencial do arquivo). */
    public List<Alimento> listar() throws Exception { return arq.listar(); }

    /**
     * <b>Consulta ordenada via Árvore B+</b>: percorre as folhas em ordem
     * crescente de nome (já encadeadas) e materializa cada Alimento a partir
     * do índice primário em hash — sem ordenação em memória.
     */
    public List<Alimento> listarOrdenadoPorNome() throws Exception {
        List<ParNomeID> pares = idxNome.listarTodos();
        List<Alimento> r = new ArrayList<>(pares.size());
        for (ParNomeID p : pares) {
            Alimento a = arq.read(p.getId());
            if (a != null) r.add(a);
        }
        return r;
    }

    public void close() throws Exception {
        arq.close();
        idxNome.close();
    }

    public String descricaoIndice() { return arq.descricaoIndice(); }
    public String descricaoIndiceNome() { return idxNome.toString(); }

    private static boolean equalsSafe(String a, String b) {
        return (a == null ? "" : a).equals(b == null ? "" : b);
    }
}
