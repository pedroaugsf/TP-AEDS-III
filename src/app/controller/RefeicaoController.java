package app.controller;

import app.dao.ConsumoDAO;
import app.dao.RefeicaoDAO;
import app.dao.UsuarioDAO;
import app.model.Refeicao;

import java.time.LocalDate;
import java.util.List;

/**
 * Regras: valida FK usuarioId; remove em cascata os Consumos quando uma Refeição é apagada.
 */
public class RefeicaoController {
    private final RefeicaoDAO refeicaoDAO;
    private final UsuarioDAO usuarioDAO;
    private final ConsumoDAO consumoDAO;

    public RefeicaoController(RefeicaoDAO refeicaoDAO, UsuarioDAO usuarioDAO, ConsumoDAO consumoDAO) {
        this.refeicaoDAO = refeicaoDAO;
        this.usuarioDAO = usuarioDAO;
        this.consumoDAO = consumoDAO;
    }

    public int criar(int usuarioId, LocalDate data, String tipo, String observacao) throws Exception {
        if (data == null) throw new IllegalArgumentException("Data é obrigatória");
        if (tipo == null || tipo.trim().isEmpty()) throw new IllegalArgumentException("Tipo é obrigatório");
        if (usuarioDAO != null && !usuarioDAO.exists(usuarioId))
            throw new IllegalArgumentException("Usuário id=" + usuarioId + " inexistente (FK)");
        return refeicaoDAO.create(new Refeicao(usuarioId, data, tipo.trim(), observacao == null ? "" : observacao));
    }

    public Refeicao buscar(int id) throws Exception { return refeicaoDAO.read(id); }

    public List<Refeicao> listar() throws Exception { return refeicaoDAO.listar(); }

    public boolean atualizar(Refeicao r) throws Exception {
        if (r == null || r.getId() <= 0) throw new IllegalArgumentException("Refeição inválida");
        if (!refeicaoDAO.exists(r.getId())) throw new IllegalArgumentException("Refeição id=" + r.getId() + " não existe");
        if (usuarioDAO != null && !usuarioDAO.exists(r.getUsuarioId()))
            throw new IllegalArgumentException("Usuário id=" + r.getUsuarioId() + " inexistente (FK)");
        if (r.getData() == null) throw new IllegalArgumentException("Data é obrigatória");
        if (r.getTipo() == null || r.getTipo().trim().isEmpty()) throw new IllegalArgumentException("Tipo é obrigatório");
        return refeicaoDAO.update(r);
    }

    /** Remove a refeição e (em cascata) todos os consumos vinculados via índice 1:N. */
    public boolean remover(int id) throws Exception {
        if (!refeicaoDAO.exists(id)) throw new IllegalArgumentException("Refeição id=" + id + " não existe");
        consumoDAO.removerPorRefeicao(id);
        return refeicaoDAO.delete(id);
    }
}
