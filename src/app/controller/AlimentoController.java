package app.controller;

import app.dao.AlimentoDAO;
import app.dao.FavoritoDAO;
import app.model.Alimento;

import java.util.List;

/**
 * Regras de Alimento + integração com a B+ (consulta ordenada por nome)
 * e com a tabela N:N (cascade nos favoritos quando um alimento é removido).
 */
public class AlimentoController {
    private final AlimentoDAO dao;
    private final FavoritoDAO favoritoDAO;

    public AlimentoController(AlimentoDAO dao, FavoritoDAO favoritoDAO) {
        this.dao = dao;
        this.favoritoDAO = favoritoDAO;
    }

    /** Sobrecarga para compatibilidade com chamadores antigos (sem FavoritoDAO). */
    public AlimentoController(AlimentoDAO dao) { this(dao, null); }

    public int criar(String nome, String marca, float kcal, float p, float c, float g, List<String> tags) throws Exception {
        if (nome == null || nome.trim().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        if (kcal < 0 || p < 0 || c < 0 || g < 0) throw new IllegalArgumentException("Macros não podem ser negativos");
        return dao.create(new Alimento(nome.trim(), marca == null ? "" : marca.trim(), kcal, p, c, g, tags));
    }

    public Alimento buscar(int id) throws Exception { return dao.read(id); }

    public List<Alimento> listar() throws Exception { return dao.listar(); }

    /** Listagem em ordem alfabética via Árvore B+ (sem ordenação em memória). */
    public List<Alimento> listarOrdenado() throws Exception { return dao.listarOrdenadoPorNome(); }

    public boolean atualizar(Alimento a) throws Exception {
        if (a == null || a.getId() <= 0) throw new IllegalArgumentException("Alimento inválido");
        if (!dao.exists(a.getId())) throw new IllegalArgumentException("Alimento id=" + a.getId() + " não existe");
        if (a.getNome() == null || a.getNome().trim().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        if (a.getKcalPor100g() < 0 || a.getProteinaPor100g() < 0 || a.getCarboPor100g() < 0 || a.getGorduraPor100g() < 0)
            throw new IllegalArgumentException("Macros não podem ser negativos");
        return dao.update(a);
    }

    /** Remoção com cascade: apaga os favoritos referenciando este alimento (integridade N:N). */
    public boolean remover(int id) throws Exception {
        if (!dao.exists(id)) throw new IllegalArgumentException("Alimento id=" + id + " não existe");
        if (favoritoDAO != null) favoritoDAO.removerPorAlimento(id);
        return dao.delete(id);
    }
}
