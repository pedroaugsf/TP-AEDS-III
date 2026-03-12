package app;

import app.controller.AlimentoController;
import app.controller.ConsumoController;
import app.controller.RefeicaoController;
import app.controller.UsuarioController;
import app.dao.AlimentoDAO;
import app.dao.ConsumoDAO;
import app.dao.RefeicaoDAO;
import app.dao.UsuarioDAO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Demo guiado via console:
 * 1) Cria 1 usuário
 * 2) Cria 2 alimentos
 * 3) Cria 1 refeição (FK usuarioId)
 * 4) Cria 2 consumos (N:N)
 * 5) Faz READ e imprime
 *
 * Se preferir CRUD completo por menu, execute: new ConsoleApp().run();
 */
public class Main {

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        UsuarioDAO usuarioDAO = new UsuarioDAO();
        AlimentoDAO alimentoDAO = new AlimentoDAO();
        RefeicaoDAO refeicaoDAO = new RefeicaoDAO();
        ConsumoDAO consumoDAO = new ConsumoDAO();

        UsuarioController usuarioCtrl = new UsuarioController(usuarioDAO);
        AlimentoController alimentoCtrl = new AlimentoController(alimentoDAO);
        RefeicaoController refeicaoCtrl = new RefeicaoController(refeicaoDAO, usuarioDAO);
        ConsumoController consumoCtrl = new ConsumoController(consumoDAO, refeicaoDAO, alimentoDAO);

        try {
            System.out.println("=== NutriTrack - Demo guiado (CRUD via console) ===\n");

            int usuarioId = criarUsuario(sc, usuarioCtrl);

            int alimento1Id = criarAlimento(sc, alimentoCtrl, 1);
            int alimento2Id = criarAlimento(sc, alimentoCtrl, 2);

            int refeicaoId = criarRefeicao(sc, refeicaoCtrl, usuarioId);

            int consumo1Id = criarConsumo(sc, consumoCtrl, refeicaoId, alimento1Id, 1);
            int consumo2Id = criarConsumo(sc, consumoCtrl, refeicaoId, alimento2Id, 2);

            System.out.println("\n=== RESULTADOS (READ) ===");
            System.out.println("Usuário: " + usuarioCtrl.buscar(usuarioId));
            System.out.println("Alimento #1: " + alimentoCtrl.buscar(alimento1Id));
            System.out.println("Alimento #2: " + alimentoCtrl.buscar(alimento2Id));
            System.out.println("Refeição: " + refeicaoCtrl.buscar(refeicaoId));
            System.out.println("Consumo #1: " + consumoCtrl.buscar(consumo1Id));
            System.out.println("Consumo #2: " + consumoCtrl.buscar(consumo2Id));

            System.out.println("\nDados gravados em ./dados/ (arquivos binários com cabeçalho + lápide).");

        } finally {
            usuarioDAO.close();
            alimentoDAO.close();
            refeicaoDAO.close();
            consumoDAO.close();
        }
    }

    private static int criarUsuario(Scanner sc, UsuarioController ctrl) throws Exception {
        System.out.println("=== CADASTRO USUÁRIO ===");
        String nome = readStr(sc, "Nome: ");
        String email = readStr(sc, "Email: ");
        LocalDate nasc = LocalDate.parse(readStr(sc, "Data nascimento (YYYY-MM-DD): "));
        String telsRaw = readStrAllowEmpty(sc, "Telefones (separe por vírgula): ");
        List<String> tels = parseList(telsRaw);

        int id = ctrl.criar(nome, email, nasc, tels);
        System.out.println("✅ Usuário criado com ID: " + id + "\n");
        return id;
    }

    private static int criarAlimento(Scanner sc, AlimentoController ctrl, int n) throws Exception {
        System.out.println("=== CADASTRO ALIMENTO " + n + " ===");
        String nome = readStr(sc, "Nome: ");
        String marca = readStrAllowEmpty(sc, "Marca (opcional): ");

        float kcal = readFloat(sc, "Kcal por 100g: ");
        float p = readFloat(sc, "Proteína por 100g: ");
        float c = readFloat(sc, "Carbo por 100g: ");
        float g = readFloat(sc, "Gordura por 100g: ");

        String tagsRaw = readStrAllowEmpty(sc, "Tags (separe por vírgula): ");
        List<String> tags = parseList(tagsRaw);

        int id = ctrl.criar(nome, marca, kcal, p, c, g, tags);
        System.out.println("✅ Alimento criado com ID: " + id + "\n");
        return id;
    }

    private static int criarRefeicao(Scanner sc, RefeicaoController ctrl, int usuarioId) throws Exception {
        System.out.println("=== CADASTRO REFEIÇÃO ===");
        System.out.println("UsuarioId (FK) = " + usuarioId);
        String dataStr = readStr(sc, "Data (YYYY-MM-DD): ");
        LocalDate data = LocalDate.parse(dataStr);

        String tipo = readStr(sc, "Tipo (Café/Almoço/Jantar/Lanche): ");
        String obs = readStrAllowEmpty(sc, "Observação (opcional): ");

        int id = ctrl.criar(usuarioId, data, tipo, obs);
        System.out.println("✅ Refeição criada com ID: " + id + "\n");
        return id;
    }

    private static int criarConsumo(Scanner sc, ConsumoController ctrl, int refeicaoId, int alimentoId, int n) throws Exception {
        System.out.println("=== ADICIONAR ITEM " + n + " NA REFEIÇÃO (Consumo) ===");
        System.out.println("RefeicaoId (FK) = " + refeicaoId);
        System.out.println("AlimentoId (FK) = " + alimentoId);
        float gramas = readFloat(sc, "Quantidade (g): ");

        int id = ctrl.adicionarItem(refeicaoId, alimentoId, gramas);
        System.out.println("✅ Consumo criado com ID: " + id + "\n");
        return id;
    }

    private static List<String> parseList(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String p : raw.split(",")) {
            String t = p.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static float readFloat(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim().replace(",", ".");
            try {
                return Float.parseFloat(s);
            } catch (Exception e) {
                System.out.println("Digite um número real válido.");
            }
        }
    }

    private static String readStr(Scanner sc, String prompt) {
        while (true) {
            System.out.print(prompt);
            String s = sc.nextLine().trim();
            if (!s.isEmpty()) return s;
            System.out.println("Campo obrigatório.");
        }
    }

    private static String readStrAllowEmpty(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine().trim();
    }
}
