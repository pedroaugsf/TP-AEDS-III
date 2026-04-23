package app;

import app.controller.AlimentoController;
import app.controller.ConsumoController;
import app.controller.RefeicaoController;
import app.controller.UsuarioController;
import app.dao.AlimentoDAO;
import app.dao.ConsumoDAO;
import app.dao.RefeicaoDAO;
import app.dao.UsuarioDAO;
import app.model.Alimento;
import app.model.Consumo;
import app.model.Refeicao;
import app.model.Usuario;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Servidor HTTP embutido (com.sun.net.httpserver, sem dependências externas).
 *
 *  - Serve arquivos estáticos da pasta ./web (front-end).
 *  - Expõe API REST JSON em /api/usuario, /api/alimento, /api/refeicao, /api/consumo,
 *    e /api/refeicao/{id}/consumos para o relacionamento 1:N via Hash Extensível.
 *
 * Para executar:  java -cp out app.Servidor
 * Front-end:      http://localhost:8080
 */
public class Servidor {

    private static final int PORTA = 8080;
    private static final Path WEB_DIR = Paths.get("./web").toAbsolutePath().normalize();

    private static UsuarioDAO usuarioDAO;
    private static AlimentoDAO alimentoDAO;
    private static RefeicaoDAO refeicaoDAO;
    private static ConsumoDAO consumoDAO;

    private static UsuarioController usuarioCtrl;
    private static AlimentoController alimentoCtrl;
    private static RefeicaoController refeicaoCtrl;
    private static ConsumoController consumoCtrl;

    public static void main(String[] args) throws Exception {
        usuarioDAO = new UsuarioDAO();
        alimentoDAO = new AlimentoDAO();
        refeicaoDAO = new RefeicaoDAO();
        consumoDAO = new ConsumoDAO();

        usuarioCtrl = new UsuarioController(usuarioDAO);
        alimentoCtrl = new AlimentoController(alimentoDAO);
        refeicaoCtrl = new RefeicaoController(refeicaoDAO, usuarioDAO, consumoDAO);
        consumoCtrl = new ConsumoController(consumoDAO, refeicaoDAO, alimentoDAO);

        HttpServer server = HttpServer.create(new InetSocketAddress(PORTA), 0);

        server.createContext("/api/usuario", Servidor::handleUsuario);
        server.createContext("/api/alimento", Servidor::handleAlimento);
        server.createContext("/api/refeicao", Servidor::handleRefeicao);
        server.createContext("/api/consumo", Servidor::handleConsumo);
        server.createContext("/", Servidor::handleStatic);

        server.setExecutor(null);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                usuarioDAO.close();
                alimentoDAO.close();
                refeicaoDAO.close();
                consumoDAO.close();
                System.out.println("\n[Servidor] arquivos fechados.");
            } catch (Exception ignored) {}
        }));

        server.start();
        System.out.println("[Servidor] NutriTrack rodando em http://localhost:" + PORTA);
        System.out.println("[Servidor] Diretório web/: " + WEB_DIR);
        System.out.println("[Servidor] Ctrl+C para encerrar.");
    }

    // ====================================================================
    //   ROTAS
    // ====================================================================

    private static void handleUsuario(HttpExchange ex) throws IOException {
        try {
            String m = ex.getRequestMethod();
            String[] partes = pathSegments(ex, "/api/usuario");

            if (partes.length == 0) {
                if (m.equals("GET")) {
                    sendJson(ex, 200, listToJson(usuarioCtrl.listar(), Servidor::usuarioToJson));
                } else if (m.equals("POST")) {
                    Map<String, Object> body = parseJson(readBody(ex));
                    int id = usuarioCtrl.criar(
                            (String) body.get("nome"),
                            (String) body.get("email"),
                            LocalDate.parse((String) body.get("dataNascimento")),
                            asListString(body.get("telefones")));
                    sendJson(ex, 201, "{\"id\":" + id + "}");
                } else sendError(ex, 405, "Método não permitido");
            } else if (partes.length == 1) {
                int id = Integer.parseInt(partes[0]);
                if (m.equals("GET")) {
                    Usuario u = usuarioCtrl.buscar(id);
                    if (u == null) { sendError(ex, 404, "Usuário não encontrado"); return; }
                    sendJson(ex, 200, usuarioToJson(u));
                } else if (m.equals("PUT")) {
                    Usuario u = usuarioCtrl.buscar(id);
                    if (u == null) { sendError(ex, 404, "Usuário não encontrado"); return; }
                    Map<String, Object> body = parseJson(readBody(ex));
                    if (body.containsKey("nome")) u.setNome((String) body.get("nome"));
                    if (body.containsKey("email")) u.setEmail((String) body.get("email"));
                    if (body.containsKey("dataNascimento"))
                        u.setDataNascimento(LocalDate.parse((String) body.get("dataNascimento")));
                    if (body.containsKey("telefones")) u.setTelefones(asListString(body.get("telefones")));
                    usuarioCtrl.atualizar(u);
                    sendJson(ex, 200, usuarioToJson(u));
                } else if (m.equals("DELETE")) {
                    usuarioCtrl.remover(id);
                    sendJson(ex, 200, "{\"ok\":true}");
                } else sendError(ex, 405, "Método não permitido");
            } else sendError(ex, 404, "Rota inválida");
        } catch (IllegalArgumentException iae) {
            sendError(ex, 400, iae.getMessage());
        } catch (Exception e) {
            sendError(ex, 500, e.getMessage());
        }
    }

    private static void handleAlimento(HttpExchange ex) throws IOException {
        try {
            String m = ex.getRequestMethod();
            String[] partes = pathSegments(ex, "/api/alimento");

            if (partes.length == 0) {
                if (m.equals("GET")) {
                    sendJson(ex, 200, listToJson(alimentoCtrl.listar(), Servidor::alimentoToJson));
                } else if (m.equals("POST")) {
                    Map<String, Object> body = parseJson(readBody(ex));
                    int id = alimentoCtrl.criar(
                            (String) body.get("nome"),
                            (String) body.getOrDefault("marca", ""),
                            toFloat(body.get("kcalPor100g")),
                            toFloat(body.get("proteinaPor100g")),
                            toFloat(body.get("carboPor100g")),
                            toFloat(body.get("gorduraPor100g")),
                            asListString(body.get("tags")));
                    sendJson(ex, 201, "{\"id\":" + id + "}");
                } else sendError(ex, 405, "Método não permitido");
            } else if (partes.length == 1) {
                int id = Integer.parseInt(partes[0]);
                if (m.equals("GET")) {
                    Alimento a = alimentoCtrl.buscar(id);
                    if (a == null) { sendError(ex, 404, "Alimento não encontrado"); return; }
                    sendJson(ex, 200, alimentoToJson(a));
                } else if (m.equals("PUT")) {
                    Alimento a = alimentoCtrl.buscar(id);
                    if (a == null) { sendError(ex, 404, "Alimento não encontrado"); return; }
                    Map<String, Object> body = parseJson(readBody(ex));
                    if (body.containsKey("nome")) a.setNome((String) body.get("nome"));
                    if (body.containsKey("marca")) a.setMarca((String) body.get("marca"));
                    if (body.containsKey("kcalPor100g")) a.setKcalPor100g(toFloat(body.get("kcalPor100g")));
                    if (body.containsKey("proteinaPor100g")) a.setProteinaPor100g(toFloat(body.get("proteinaPor100g")));
                    if (body.containsKey("carboPor100g")) a.setCarboPor100g(toFloat(body.get("carboPor100g")));
                    if (body.containsKey("gorduraPor100g")) a.setGorduraPor100g(toFloat(body.get("gorduraPor100g")));
                    if (body.containsKey("tags")) a.setTags(asListString(body.get("tags")));
                    alimentoCtrl.atualizar(a);
                    sendJson(ex, 200, alimentoToJson(a));
                } else if (m.equals("DELETE")) {
                    alimentoCtrl.remover(id);
                    sendJson(ex, 200, "{\"ok\":true}");
                } else sendError(ex, 405, "Método não permitido");
            } else sendError(ex, 404, "Rota inválida");
        } catch (IllegalArgumentException iae) {
            sendError(ex, 400, iae.getMessage());
        } catch (Exception e) {
            sendError(ex, 500, e.getMessage());
        }
    }

    private static void handleRefeicao(HttpExchange ex) throws IOException {
        try {
            String m = ex.getRequestMethod();
            String[] partes = pathSegments(ex, "/api/refeicao");

            if (partes.length == 0) {
                if (m.equals("GET")) {
                    sendJson(ex, 200, listToJson(refeicaoCtrl.listar(), Servidor::refeicaoToJson));
                } else if (m.equals("POST")) {
                    Map<String, Object> body = parseJson(readBody(ex));
                    int id = refeicaoCtrl.criar(
                            ((Number) body.get("usuarioId")).intValue(),
                            LocalDate.parse((String) body.get("data")),
                            (String) body.get("tipo"),
                            (String) body.getOrDefault("observacao", ""));
                    sendJson(ex, 201, "{\"id\":" + id + "}");
                } else sendError(ex, 405, "Método não permitido");
            } else if (partes.length == 1) {
                int id = Integer.parseInt(partes[0]);
                if (m.equals("GET")) {
                    Refeicao r = refeicaoCtrl.buscar(id);
                    if (r == null) { sendError(ex, 404, "Refeição não encontrada"); return; }
                    sendJson(ex, 200, refeicaoToJson(r));
                } else if (m.equals("PUT")) {
                    Refeicao r = refeicaoCtrl.buscar(id);
                    if (r == null) { sendError(ex, 404, "Refeição não encontrada"); return; }
                    Map<String, Object> body = parseJson(readBody(ex));
                    if (body.containsKey("usuarioId")) r.setUsuarioId(((Number) body.get("usuarioId")).intValue());
                    if (body.containsKey("data")) r.setData(LocalDate.parse((String) body.get("data")));
                    if (body.containsKey("tipo")) r.setTipo((String) body.get("tipo"));
                    if (body.containsKey("observacao")) r.setObservacao((String) body.get("observacao"));
                    refeicaoCtrl.atualizar(r);
                    sendJson(ex, 200, refeicaoToJson(r));
                } else if (m.equals("DELETE")) {
                    refeicaoCtrl.remover(id);
                    sendJson(ex, 200, "{\"ok\":true}");
                } else sendError(ex, 405, "Método não permitido");
            } else if (partes.length == 2 && partes[1].equals("consumos")) {
                int refId = Integer.parseInt(partes[0]);
                if (m.equals("GET")) {
                    sendJson(ex, 200, listToJson(consumoCtrl.listarPorRefeicao(refId), Servidor::consumoToJson));
                } else sendError(ex, 405, "Método não permitido");
            } else sendError(ex, 404, "Rota inválida");
        } catch (IllegalArgumentException iae) {
            sendError(ex, 400, iae.getMessage());
        } catch (Exception e) {
            sendError(ex, 500, e.getMessage());
        }
    }

    private static void handleConsumo(HttpExchange ex) throws IOException {
        try {
            String m = ex.getRequestMethod();
            String[] partes = pathSegments(ex, "/api/consumo");

            if (partes.length == 0) {
                if (m.equals("GET")) {
                    sendJson(ex, 200, listToJson(consumoCtrl.listar(), Servidor::consumoToJson));
                } else if (m.equals("POST")) {
                    Map<String, Object> body = parseJson(readBody(ex));
                    int id = consumoCtrl.adicionarItem(
                            ((Number) body.get("refeicaoId")).intValue(),
                            ((Number) body.get("alimentoId")).intValue(),
                            toFloat(body.get("quantidadeGramas")));
                    sendJson(ex, 201, "{\"id\":" + id + "}");
                } else sendError(ex, 405, "Método não permitido");
            } else if (partes.length == 1) {
                int id = Integer.parseInt(partes[0]);
                if (m.equals("GET")) {
                    Consumo c = consumoCtrl.buscar(id);
                    if (c == null) { sendError(ex, 404, "Consumo não encontrado"); return; }
                    sendJson(ex, 200, consumoToJson(c));
                } else if (m.equals("PUT")) {
                    Consumo c = consumoCtrl.buscar(id);
                    if (c == null) { sendError(ex, 404, "Consumo não encontrado"); return; }
                    Map<String, Object> body = parseJson(readBody(ex));
                    if (body.containsKey("refeicaoId")) c.setRefeicaoId(((Number) body.get("refeicaoId")).intValue());
                    if (body.containsKey("alimentoId")) c.setAlimentoId(((Number) body.get("alimentoId")).intValue());
                    if (body.containsKey("quantidadeGramas")) c.setQuantidadeGramas(toFloat(body.get("quantidadeGramas")));
                    consumoCtrl.atualizar(c);
                    sendJson(ex, 200, consumoToJson(c));
                } else if (m.equals("DELETE")) {
                    consumoCtrl.remover(id);
                    sendJson(ex, 200, "{\"ok\":true}");
                } else sendError(ex, 405, "Método não permitido");
            } else sendError(ex, 404, "Rota inválida");
        } catch (IllegalArgumentException iae) {
            sendError(ex, 400, iae.getMessage());
        } catch (Exception e) {
            sendError(ex, 500, e.getMessage());
        }
    }

    // ====================================================================
    //   STATIC FILES
    // ====================================================================

    private static void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path.equals("/")) path = "/index.html";
        Path target = WEB_DIR.resolve(path.substring(1)).normalize();
        // proteção contra path traversal
        if (!target.startsWith(WEB_DIR)) { sendError(ex, 403, "forbidden"); return; }
        if (!Files.exists(target) || Files.isDirectory(target)) { sendError(ex, 404, "Arquivo não encontrado: " + path); return; }
        byte[] data = Files.readAllBytes(target);
        String mime = guessMime(target.toString());
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    private static String guessMime(String f) {
        f = f.toLowerCase();
        if (f.endsWith(".html") || f.endsWith(".htm")) return "text/html; charset=utf-8";
        if (f.endsWith(".css")) return "text/css; charset=utf-8";
        if (f.endsWith(".js")) return "application/javascript; charset=utf-8";
        if (f.endsWith(".json")) return "application/json; charset=utf-8";
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    // ====================================================================
    //   JSON helpers
    // ====================================================================

    interface ToJson<T> { String to(T t); }

    private static <T> String listToJson(List<T> xs, ToJson<T> fn) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < xs.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(fn.to(xs.get(i)));
        }
        return sb.append(']').toString();
    }

    private static String usuarioToJson(Usuario u) {
        return "{\"id\":" + u.getId()
                + ",\"nome\":" + jstr(u.getNome())
                + ",\"email\":" + jstr(u.getEmail())
                + ",\"dataNascimento\":" + jstr(u.getDataNascimento() == null ? "" : u.getDataNascimento().toString())
                + ",\"telefones\":" + jarr(u.getTelefones())
                + "}";
    }

    private static String alimentoToJson(Alimento a) {
        return "{\"id\":" + a.getId()
                + ",\"nome\":" + jstr(a.getNome())
                + ",\"marca\":" + jstr(a.getMarca())
                + ",\"kcalPor100g\":" + a.getKcalPor100g()
                + ",\"proteinaPor100g\":" + a.getProteinaPor100g()
                + ",\"carboPor100g\":" + a.getCarboPor100g()
                + ",\"gorduraPor100g\":" + a.getGorduraPor100g()
                + ",\"tags\":" + jarr(a.getTags())
                + "}";
    }

    private static String refeicaoToJson(Refeicao r) {
        return "{\"id\":" + r.getId()
                + ",\"usuarioId\":" + r.getUsuarioId()
                + ",\"data\":" + jstr(r.getData() == null ? "" : r.getData().toString())
                + ",\"tipo\":" + jstr(r.getTipo())
                + ",\"observacao\":" + jstr(r.getObservacao())
                + "}";
    }

    private static String consumoToJson(Consumo c) {
        return "{\"id\":" + c.getId()
                + ",\"refeicaoId\":" + c.getRefeicaoId()
                + ",\"alimentoId\":" + c.getAlimentoId()
                + ",\"quantidadeGramas\":" + c.getQuantidadeGramas()
                + "}";
    }

    private static String jstr(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (ch < 0x20) sb.append(String.format("\\u%04x", (int) ch));
                    else sb.append(ch);
            }
        }
        return sb.append('"').toString();
    }

    private static String jarr(List<String> ls) {
        if (ls == null || ls.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < ls.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(jstr(ls.get(i)));
        }
        return sb.append(']').toString();
    }

    // ===== JSON parser mínimo (objetos planos: strings, números, booleans, arrays de strings) =====

    private static Map<String, Object> parseJson(String s) {
        Parser p = new Parser(s);
        p.skipWs();
        Object o = p.parseValue();
        if (!(o instanceof Map)) throw new IllegalArgumentException("JSON inválido (esperado objeto)");
        @SuppressWarnings("unchecked") Map<String, Object> m = (Map<String, Object>) o;
        return m;
    }

    private static class Parser {
        final String s; int i;
        Parser(String s) { this.s = s == null ? "" : s; }
        void skipWs() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        Object parseValue() {
            skipWs();
            if (i >= s.length()) return null;
            char c = s.charAt(i);
            if (c == '{') return parseObject();
            if (c == '[') return parseArray();
            if (c == '"') return parseString();
            if (c == 't' || c == 'f') return parseBool();
            if (c == 'n') { i += 4; return null; }
            return parseNumber();
        }
        Map<String, Object> parseObject() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // {
            skipWs();
            if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
            while (i < s.length()) {
                skipWs();
                String k = parseString();
                skipWs();
                if (s.charAt(i) != ':') throw new IllegalArgumentException("esperado ':'");
                i++;
                Object v = parseValue();
                m.put(k, v);
                skipWs();
                if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
                if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
                throw new IllegalArgumentException("esperado ',' ou '}'");
            }
            return m;
        }
        List<Object> parseArray() {
            List<Object> a = new ArrayList<>();
            i++; // [
            skipWs();
            if (i < s.length() && s.charAt(i) == ']') { i++; return a; }
            while (i < s.length()) {
                Object v = parseValue();
                a.add(v);
                skipWs();
                if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
                if (i < s.length() && s.charAt(i) == ']') { i++; return a; }
                throw new IllegalArgumentException("esperado ',' ou ']'");
            }
            return a;
        }
        String parseString() {
            if (s.charAt(i) != '"') throw new IllegalArgumentException("esperado string");
            i++;
            StringBuilder sb = new StringBuilder();
            while (i < s.length() && s.charAt(i) != '"') {
                char c = s.charAt(i++);
                if (c == '\\' && i < s.length()) {
                    char n = s.charAt(i++);
                    switch (n) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            String hex = s.substring(i, i + 4); i += 4;
                            sb.append((char) Integer.parseInt(hex, 16));
                            break;
                        default: sb.append(n);
                    }
                } else sb.append(c);
            }
            i++; // "
            return sb.toString();
        }
        Boolean parseBool() {
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            throw new IllegalArgumentException("bool inválido");
        }
        Number parseNumber() {
            int start = i;
            if (s.charAt(i) == '-') i++;
            while (i < s.length() && (Character.isDigit(s.charAt(i)) || s.charAt(i) == '.' || s.charAt(i) == 'e' || s.charAt(i) == 'E' || s.charAt(i) == '+' || s.charAt(i) == '-')) i++;
            String num = s.substring(start, i);
            if (num.contains(".") || num.contains("e") || num.contains("E")) return Double.parseDouble(num);
            return Long.parseLong(num);
        }
    }

    private static List<String> asListString(Object o) {
        List<String> out = new ArrayList<>();
        if (o == null) return out;
        if (o instanceof List<?>) {
            for (Object x : (List<?>) o) out.add(x == null ? "" : x.toString());
        } else if (o instanceof String) {
            for (String p : ((String) o).split(",")) {
                String t = p.trim();
                if (!t.isEmpty()) out.add(t);
            }
        }
        return out;
    }

    private static float toFloat(Object o) {
        if (o == null) return 0f;
        if (o instanceof Number) return ((Number) o).floatValue();
        return Float.parseFloat(o.toString().replace(",", "."));
    }

    // ====================================================================
    //   HTTP helpers
    // ====================================================================

    private static String[] pathSegments(HttpExchange ex, String prefix) {
        String p = ex.getRequestURI().getPath();
        if (p.startsWith(prefix)) p = p.substring(prefix.length());
        if (p.startsWith("/")) p = p.substring(1);
        if (p.endsWith("/")) p = p.substring(0, p.length() - 1);
        if (p.isEmpty()) return new String[0];
        return p.split("/");
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream is = ex.getRequestBody();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = is.read(buf)) > 0) baos.write(buf, 0, n);
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    private static void sendJson(HttpExchange ex, int code, String json) throws IOException {
        byte[] data = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }

    private static void sendError(HttpExchange ex, int code, String msg) throws IOException {
        sendJson(ex, code, "{\"erro\":" + jstr(msg == null ? "erro" : msg) + "}");
    }
}
