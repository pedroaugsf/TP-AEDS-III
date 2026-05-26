package app.dao;

import app.Arquivo;
import app.model.Favorito;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

/**
 * FavoritoDAO — persistência da tabela intermediária N:N
 * (Usuario ↔ Alimento) "Alimentos Favoritos".
 *
 * <h3>Estrutura</h3>
 * <ul>
 *   <li><b>{@code favorito.db}</b> — arquivo binário com cabeçalho + lápide +
 *       lista de removidos (best-fit), via {@link Arquivo}.</li>
 *   <li><b>Índice primário</b> (id -&gt; endereço) — Hash Extensível,
 *       provido por {@link Arquivo}.</li>
 *   <li><b>Índice 1 da chave composta</b> ({@code usuarioId -&gt; favoritoId})
 *       em {@link HashExtensivel}. Permite listar todos os alimentos
 *       favoritados por um usuário em O(1) médio.</li>
 *   <li><b>Índice 2 da chave composta</b> ({@code alimentoId -&gt; favoritoId})
 *       em {@link HashExtensivel}. Permite listar todos os usuários que
 *       favoritaram um alimento em O(1) médio (acesso reverso).</li>
 * </ul>
 *
 * <h3>Chave primária composta (usuarioId, alimentoId)</h3>
 * A unicidade do par é garantida em {@link #create(Favorito)}: antes de
 * inserir, consulta-se o índice por usuário e percorre-se a lista (curta)
 * para checar se já existe favorito com o mesmo alimento. Se existir,
 * a operação é rejeitada com {@link IllegalStateException}.
 *
 * <h3>Por que Hash Extensível para os dois índices?</h3>
 * O padrão de acesso típico de uma tabela associativa é <em>equality
 * lookup</em> ("dado este id, traga as associações"). Hash dá O(1) médio,
 * enquanto uma B+ daria O(log n) sem ganho — só faria sentido se
 * precisássemos de varredura ordenada por essas chaves. Para listagem
 * ordenada por nome de alimento, usamos a B+ no AlimentoDAO.
 */
public class FavoritoDAO {

    private static final int SLOTS_BUCKET_REL = 16;

    private final Arquivo<Favorito> arq;
    private final HashExtensivel<ParIDID> idxPorUsuario;   // usuarioId  -> favoritoId
    private final HashExtensivel<ParIDID> idxPorAlimento;  // alimentoId -> favoritoId

    public FavoritoDAO() throws Exception {
        Constructor<Favorito> cf = Favorito.class.getConstructor();
        arq = new Arquivo<>("favorito", cf);

        Constructor<ParIDID> cp = ParIDID.class.getConstructor();
        idxPorUsuario  = new HashExtensivel<>("favorito_por_usuario",  SLOTS_BUCKET_REL, cp);
        idxPorAlimento = new HashExtensivel<>("favorito_por_alimento", SLOTS_BUCKET_REL, cp);

        reconstruirIndicesSeNecessario();
    }

    /** Reconstrói os índices secundários a partir do arquivo de dados, se necessário. */
    private void reconstruirIndicesSeNecessario() throws Exception {
        List<Favorito> todos = arq.listar();
        if (todos.isEmpty()) return;
        // verifica se o primeiro já está indexado
        Favorito primeiro = todos.get(0);
        boolean jaIndexado = false;
        for (ParIDID p : idxPorUsuario.read(primeiro.getUsuarioId())) {
            if (p.getChave2() == primeiro.getId()) { jaIndexado = true; break; }
        }
        if (jaIndexado) return;
        for (Favorito f : todos) {
            idxPorUsuario.create(new ParIDID(f.getUsuarioId(), f.getId()));
            idxPorAlimento.create(new ParIDID(f.getAlimentoId(), f.getId()));
        }
    }

    // ===================== CRUD =====================

    /**
     * Insere o favorito validando unicidade da chave composta (usuarioId, alimentoId).
     * @throws IllegalStateException se o par já existir.
     */
    public int create(Favorito f) throws Exception {
        if (existeAssociacao(f.getUsuarioId(), f.getAlimentoId()))
            throw new IllegalStateException("Favorito (usuario=" + f.getUsuarioId()
                    + ", alimento=" + f.getAlimentoId() + ") já existe");

        int id = arq.create(f);
        idxPorUsuario.create(new ParIDID(f.getUsuarioId(), id));
        idxPorAlimento.create(new ParIDID(f.getAlimentoId(), id));
        return id;
    }

    public Favorito read(int id) throws Exception { return arq.read(id); }
    public boolean exists(int id) throws Exception { return arq.exists(id); }

    /** Atualiza apenas atributos não-chave (nota, observacao, dataInclusao). */
    public boolean update(Favorito novo) throws Exception {
        Favorito atual = arq.read(novo.getId());
        if (atual == null) return false;
        // preserva a chave composta (não permite reatribuir)
        novo.setUsuarioId(atual.getUsuarioId());
        novo.setAlimentoId(atual.getAlimentoId());
        return arq.update(novo);
    }

    public boolean delete(int id) throws Exception {
        Favorito f = arq.read(id);
        if (f == null) return false;
        boolean ok = arq.delete(id);
        if (ok) {
            idxPorUsuario.delete(new ParIDID(f.getUsuarioId(), id));
            idxPorAlimento.delete(new ParIDID(f.getAlimentoId(), id));
        }
        return ok;
    }

    public List<Favorito> listar() throws Exception { return arq.listar(); }

    // ===================== Consultas N:N =====================

    /** Lista os favoritos de um usuário (lado "alimentos favoritados pelo usuário"). */
    public List<Favorito> listarPorUsuario(int usuarioId) throws Exception {
        List<ParIDID> pares = idxPorUsuario.read(usuarioId);
        List<Favorito> r = new ArrayList<>(pares.size());
        for (ParIDID p : pares) {
            Favorito f = arq.read(p.getChave2());
            if (f != null) r.add(f);
        }
        return r;
    }

    /** Lista os favoritos por alimento (lado "usuários que favoritaram"). */
    public List<Favorito> listarPorAlimento(int alimentoId) throws Exception {
        List<ParIDID> pares = idxPorAlimento.read(alimentoId);
        List<Favorito> r = new ArrayList<>(pares.size());
        for (ParIDID p : pares) {
            Favorito f = arq.read(p.getChave2());
            if (f != null) r.add(f);
        }
        return r;
    }

    /** Existe favorito (usuarioId, alimentoId)? Usado para garantir unicidade da PK composta. */
    public boolean existeAssociacao(int usuarioId, int alimentoId) throws Exception {
        for (ParIDID p : idxPorUsuario.read(usuarioId)) {
            Favorito f = arq.read(p.getChave2());
            if (f != null && f.getAlimentoId() == alimentoId) return true;
        }
        return false;
    }

    /**
     * Integridade referencial: ao remover um <b>usuário</b>, todos os
     * favoritos vinculados são removidos em cascata.
     */
    public int removerPorUsuario(int usuarioId) throws Exception {
        int n = 0;
        for (Favorito f : listarPorUsuario(usuarioId)) {
            if (delete(f.getId())) n++;
        }
        return n;
    }

    /**
     * Integridade referencial: ao remover um <b>alimento</b>, todos os
     * favoritos vinculados são removidos em cascata.
     */
    public int removerPorAlimento(int alimentoId) throws Exception {
        int n = 0;
        for (Favorito f : listarPorAlimento(alimentoId)) {
            if (delete(f.getId())) n++;
        }
        return n;
    }

    public void close() throws Exception {
        arq.close();
        idxPorUsuario.close();
        idxPorAlimento.close();
    }

    public String descricaoIndices() {
        return "primario=" + arq.descricaoIndice()
                + " | porUsuario=" + idxPorUsuario.toString()
                + " | porAlimento=" + idxPorAlimento.toString();
    }
}
