package app.controller;

import app.dao.RefeicaoDAO;
import app.dao.UsuarioDAO;
import app.model.Refeicao;

import java.time.LocalDate;

public class RefeicaoController {
    private final RefeicaoDAO refeicaoDAO;
    private final UsuarioDAO usuarioDAO; // pode ser null se você não quiser validar FK

    public RefeicaoController(RefeicaoDAO refeicaoDAO) {
        this(refeicaoDAO, null);
    }

    public RefeicaoController(RefeicaoDAO refeicaoDAO, UsuarioDAO usuarioDAO) {
        this.refeicaoDAO = refeicaoDAO;
        this.usuarioDAO = usuarioDAO;
    }

    public int criar(int usuarioId, LocalDate data, String tipo, String observacao) throws Exception {
        if (data == null) throw new IllegalArgumentException("Data é obrigatória");
        if (tipo == null || tipo.trim().isEmpty()) throw new IllegalArgumentException("Tipo é obrigatório");

        if (usuarioDAO != null) {
            if (usuarioDAO.read(usuarioId) == null) {
                throw new IllegalArgumentException("Usuário inexistente (FK usuarioId)");
            }
        }

        return refeicaoDAO.create(new Refeicao(usuarioId, data, tipo.trim(), observacao));
    }

    public Refeicao buscar(int id) throws Exception { return refeicaoDAO.read(id); }
    public boolean atualizar(Refeicao r) throws Exception { return refeicaoDAO.update(r); }
    public boolean remover(int id) throws Exception { return refeicaoDAO.delete(id); }
}
