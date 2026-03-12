package app.controller;

import app.dao.UsuarioDAO;
import app.model.Usuario;

import java.time.LocalDate;
import java.util.List;

public class UsuarioController {
    private final UsuarioDAO usuarioDAO;

    public UsuarioController(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    public int criar(String nome, String email, LocalDate dataNascimento, List<String> telefones) throws Exception {
        if (nome == null || nome.trim().isEmpty()) throw new IllegalArgumentException("Nome é obrigatório");
        if (email == null || email.trim().isEmpty()) throw new IllegalArgumentException("Email é obrigatório");
        if (dataNascimento == null) throw new IllegalArgumentException("Data de nascimento é obrigatória");
        return usuarioDAO.create(new Usuario(nome.trim(), email.trim(), dataNascimento, telefones));
    }

    public Usuario buscar(int id) throws Exception { return usuarioDAO.read(id); }
    public boolean atualizar(Usuario u) throws Exception { return usuarioDAO.update(u); }
    public boolean remover(int id) throws Exception { return usuarioDAO.delete(id); }
}
