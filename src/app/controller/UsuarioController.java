package app.controller;

import app.dao.FavoritoDAO;
import app.dao.UsuarioDAO;
import app.model.Usuario;

import java.time.LocalDate;
import java.util.List;

/**
 * Regras de Usuário + cascade nos favoritos quando um usuário é removido.
 */
public class UsuarioController {
    private final UsuarioDAO dao;
    private final FavoritoDAO favoritoDAO;

    public UsuarioController(UsuarioDAO dao, FavoritoDAO favoritoDAO) {
        this.dao = dao;
        this.favoritoDAO = favoritoDAO;
    }

    /** Sobrecarga para compatibilidade (Main/ConsoleApp não usam favoritos). */
    public UsuarioController(UsuarioDAO dao) { this(dao, null); }

    public int criar(String nome, String email, LocalDate dataNasc, List<String> telefones) throws Exception {
        if (nome == null || nome.trim().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("Email é obrigatório");
        if (dataNasc == null) throw new IllegalArgumentException("Data de nascimento é obrigatória");
        return dao.create(new Usuario(nome.trim(), email.trim(), dataNasc, telefones));
    }

    public Usuario buscar(int id) throws Exception { return dao.read(id); }

    public List<Usuario> listar() throws Exception { return dao.listar(); }

    public boolean atualizar(Usuario u) throws Exception {
        if (u == null || u.getId() <= 0) throw new IllegalArgumentException("Usuário inválido");
        if (!dao.exists(u.getId())) throw new IllegalArgumentException("Usuário id=" + u.getId() + " não existe");
        if (u.getNome() == null || u.getNome().trim().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        if (u.getEmail() == null || u.getEmail().trim().isEmpty()) throw new IllegalArgumentException("Email é obrigatório");
        if (u.getDataNascimento() == null) throw new IllegalArgumentException("Data de nascimento é obrigatória");
        return dao.update(u);
    }

    /** Remoção com cascade: apaga favoritos vinculados (integridade N:N). */
    public boolean remover(int id) throws Exception {
        if (!dao.exists(id)) throw new IllegalArgumentException("Usuário id=" + id + " não existe");
        if (favoritoDAO != null) favoritoDAO.removerPorUsuario(id);
        return dao.delete(id);
    }
}
