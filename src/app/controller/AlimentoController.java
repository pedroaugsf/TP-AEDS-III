package app.controller;

import app.dao.AlimentoDAO;
import app.model.Alimento;

import java.util.List;

public class AlimentoController {
    private final AlimentoDAO alimentoDAO;

    public AlimentoController(AlimentoDAO alimentoDAO) {
        this.alimentoDAO = alimentoDAO;
    }

    public int criar(String nome, String marca, float kcal, float p, float c, float g, List<String> tags) throws Exception {
        if (nome == null || nome.trim().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        if (kcal < 0 || p < 0 || c < 0 || g < 0) throw new IllegalArgumentException("Macros não podem ser negativos");
        return alimentoDAO.create(new Alimento(nome.trim(), marca, kcal, p, c, g, tags));
    }

    public Alimento buscar(int id) throws Exception {
        return alimentoDAO.read(id);
    }

    public boolean atualizar(Alimento a) throws Exception {
        if (a == null || a.getId() <= 0) throw new IllegalArgumentException("Alimento inválido");
        return alimentoDAO.update(a);
    }

    public boolean remover(int id) throws Exception {
        return alimentoDAO.delete(id);
    }
}
