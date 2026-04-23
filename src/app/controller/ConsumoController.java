package app.controller;

import app.dao.AlimentoDAO;
import app.dao.ConsumoDAO;
import app.dao.RefeicaoDAO;
import app.model.Consumo;

import java.util.List;

/**
 * Regras:
 *  - FK refeicaoId deve existir
 *  - FK alimentoId deve existir
 *  - quantidadeGramas > 0
 *
 * O ConsumoDAO mantém um índice 1:N (refeicaoId -> consumoId) em Hash Extensível.
 */
public class ConsumoController {

    private final ConsumoDAO consumoDAO;
    private final RefeicaoDAO refeicaoDAO;
    private final AlimentoDAO alimentoDAO;

    public ConsumoController(ConsumoDAO consumoDAO, RefeicaoDAO refeicaoDAO, AlimentoDAO alimentoDAO) {
        this.consumoDAO = consumoDAO;
        this.refeicaoDAO = refeicaoDAO;
        this.alimentoDAO = alimentoDAO;
    }

    public int adicionarItem(int refeicaoId, int alimentoId, float gramas) throws Exception {
        if (gramas <= 0) throw new IllegalArgumentException("Quantidade (g) deve ser > 0");
        if (!refeicaoDAO.exists(refeicaoId))
            throw new IllegalArgumentException("Refeição id=" + refeicaoId + " inexistente (FK)");
        if (!alimentoDAO.exists(alimentoId))
            throw new IllegalArgumentException("Alimento id=" + alimentoId + " inexistente (FK)");
        return consumoDAO.create(new Consumo(refeicaoId, alimentoId, gramas));
    }

    public Consumo buscar(int id) throws Exception { return consumoDAO.read(id); }

    public List<Consumo> listar() throws Exception { return consumoDAO.listar(); }

    public List<Consumo> listarPorRefeicao(int refeicaoId) throws Exception {
        return consumoDAO.listarPorRefeicao(refeicaoId);
    }

    public boolean atualizar(Consumo c) throws Exception {
        if (c == null || c.getId() <= 0) throw new IllegalArgumentException("Consumo inválido");
        if (!consumoDAO.exists(c.getId())) throw new IllegalArgumentException("Consumo id=" + c.getId() + " não existe");
        if (!refeicaoDAO.exists(c.getRefeicaoId()))
            throw new IllegalArgumentException("Refeição id=" + c.getRefeicaoId() + " inexistente (FK)");
        if (!alimentoDAO.exists(c.getAlimentoId()))
            throw new IllegalArgumentException("Alimento id=" + c.getAlimentoId() + " inexistente (FK)");
        if (c.getQuantidadeGramas() <= 0) throw new IllegalArgumentException("Quantidade (g) deve ser > 0");
        return consumoDAO.update(c);
    }

    public boolean remover(int id) throws Exception {
        if (!consumoDAO.exists(id)) throw new IllegalArgumentException("Consumo id=" + id + " não existe");
        return consumoDAO.delete(id);
    }
}
