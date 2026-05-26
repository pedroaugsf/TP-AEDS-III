package app.controller;

import app.dao.AlimentoDAO;
import app.dao.FavoritoDAO;
import app.dao.UsuarioDAO;
import app.model.Favorito;

import java.time.LocalDate;
import java.util.List;

/**
 * Regras do relacionamento N:N "Alimentos Favoritos" (Usuario ↔ Alimento):
 *  - usuarioId e alimentoId são <b>obrigatórios</b> e devem existir (FKs);
 *  - o par (usuarioId, alimentoId) é <b>único</b> (chave primária composta);
 *  - nota é opcional; se informada, deve estar em 1..5;
 *  - dataInclusao default = hoje.
 *
 * Integridade referencial: o controller é também responsável por orquestrar
 * remoções em cascata quando um Usuario ou Alimento referenciado for apagado
 * (chamado pelos respectivos controllers).
 */
public class FavoritoController {

    private final FavoritoDAO favoritoDAO;
    private final UsuarioDAO usuarioDAO;
    private final AlimentoDAO alimentoDAO;

    public FavoritoController(FavoritoDAO favoritoDAO, UsuarioDAO usuarioDAO, AlimentoDAO alimentoDAO) {
        this.favoritoDAO = favoritoDAO;
        this.usuarioDAO = usuarioDAO;
        this.alimentoDAO = alimentoDAO;
    }

    public int criar(int usuarioId, int alimentoId, LocalDate data, int nota, String observacao) throws Exception {
        if (!usuarioDAO.exists(usuarioId))
            throw new IllegalArgumentException("Usuário id=" + usuarioId + " inexistente (FK)");
        if (!alimentoDAO.exists(alimentoId))
            throw new IllegalArgumentException("Alimento id=" + alimentoId + " inexistente (FK)");
        if (nota != 0 && (nota < 1 || nota > 5))
            throw new IllegalArgumentException("Nota deve estar entre 1 e 5");
        if (data == null) data = LocalDate.now();
        try {
            return favoritoDAO.create(new Favorito(usuarioId, alimentoId, data, (byte) nota,
                    observacao == null ? "" : observacao));
        } catch (IllegalStateException dup) {
            // converte para IllegalArgumentException para o servidor responder 400
            throw new IllegalArgumentException(dup.getMessage());
        }
    }

    public Favorito buscar(int id) throws Exception { return favoritoDAO.read(id); }

    public List<Favorito> listar() throws Exception { return favoritoDAO.listar(); }

    public List<Favorito> listarPorUsuario(int usuarioId) throws Exception {
        if (!usuarioDAO.exists(usuarioId))
            throw new IllegalArgumentException("Usuário id=" + usuarioId + " inexistente");
        return favoritoDAO.listarPorUsuario(usuarioId);
    }

    public List<Favorito> listarPorAlimento(int alimentoId) throws Exception {
        if (!alimentoDAO.exists(alimentoId))
            throw new IllegalArgumentException("Alimento id=" + alimentoId + " inexistente");
        return favoritoDAO.listarPorAlimento(alimentoId);
    }

    public boolean atualizar(Favorito f) throws Exception {
        if (f == null || f.getId() <= 0) throw new IllegalArgumentException("Favorito inválido");
        if (!favoritoDAO.exists(f.getId()))
            throw new IllegalArgumentException("Favorito id=" + f.getId() + " não existe");
        if (f.getNota() != 0 && (f.getNota() < 1 || f.getNota() > 5))
            throw new IllegalArgumentException("Nota deve estar entre 1 e 5");
        return favoritoDAO.update(f);
    }

    public boolean remover(int id) throws Exception {
        if (!favoritoDAO.exists(id))
            throw new IllegalArgumentException("Favorito id=" + id + " não existe");
        return favoritoDAO.delete(id);
    }
}
