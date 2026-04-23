package app.controller;

import app.dao.AlimentoDAO;
import app.model.Alimento;

import java.util.List;

public class AlimentoController {
    private final AlimentoDAO dao;

    public AlimentoController(AlimentoDAO dao) { this.dao = dao; }

    public int criar(String nome, String marca, float kcal, float p, float c, float g, List<String> tags) throws Exception {
        if (nome == null || nome.trim().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        if (kcal < 0 || p < 0 || c < 0 || g < 0) throw new IllegalArgumentException("Macros não podem ser negativos");
        return dao.create(new Alimento(nome.trim(), marca == null ? "" : marca.trim(), kcal, p, c, g, tags));
    }

    public Alimento buscar(int id) throws Exception { return dao.read(id); }

    public List<Alimento> listar() throws Exception { return dao.listar(); }

    public boolean atualizar(Alimento a) throws Exception {
        if (a == null || a.getId() <= 0) throw new IllegalArgumentException("Alimento inválido");
        if (!dao.exists(a.getId())) throw new IllegalArgumentException("Alimento id=" + a.getId() + " não existe");
        if (a.getNome() == null || a.getNome().trim().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        if (a.getKcalPor100g() < 0 || a.getProteinaPor100g() < 0 || a.getCarboPor100g() < 0 || a.getGorduraPor100g() < 0)
            throw new IllegalArgumentException("Macros não podem ser negativos");
        return dao.update(a);
    }

    public boolean remover(int id) throws Exception {
        if (!dao.exists(id)) throw new IllegalArgumentException("Alimento id=" + id + " não existe");
        return dao.delete(id);
    }
}
