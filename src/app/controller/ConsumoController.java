package app.controller;

import app.dao.AlimentoDAO;
import app.dao.ConsumoDAO;
import app.dao.RefeicaoDAO;
import app.model.Alimento;
import app.model.Consumo;
import app.model.Refeicao;

/**
 * Regras principais:
 * - FK: refeicaoId deve existir
 * - FK: alimentoId deve existir
 * - quantidadeGramas > 0
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
        if (gramas <= 0) throw new IllegalArgumentException("Quantidade deve ser > 0");

        Refeicao r = refeicaoDAO.read(refeicaoId);
        if (r == null) throw new IllegalArgumentException("Refeição inexistente (FK refeicaoId)");

        Alimento a = alimentoDAO.read(alimentoId);
        if (a == null) throw new IllegalArgumentException("Alimento inexistente (FK alimentoId)");

        return consumoDAO.create(new Consumo(refeicaoId, alimentoId, gramas));
    }

    public Consumo buscar(int id) throws Exception { return consumoDAO.read(id); }
    public boolean atualizar(Consumo c) throws Exception { return consumoDAO.update(c); }
    public boolean remover(int id) throws Exception { return consumoDAO.delete(id); }
}
