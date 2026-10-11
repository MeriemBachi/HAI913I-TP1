package hai913i.tp1.report;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.metrics.ClassCoupling;
import hai913i.tp1.metrics.MetricsReport;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

/**
 * Rapport HTML autonome (aucune ressource externe) : métriques du TP, couplage,
 * graphe SVG coloré par paquetage, clic pour l'analyse d'impact.
 */
public final class HtmlReport {

    private static final String[] PALETTE = {
            "#7dd3fc", "#f9a8d4", "#86efac", "#fcd34d", "#c4b5fd", "#fca5a5", "#67e8f9"
    };

    private HtmlReport() {
    }

    public static String render(ProjectFacts facts, MetricsReport metrics, CallGraph graph) {
        ClassCoupling coupling = ClassCoupling.of(graph);
        StringBuilder html = new StringBuilder();
        html.append("""
                <!DOCTYPE html>
                <html lang="fr">
                <head>
                <meta charset="UTF-8">
                <title>HAI913I — analyse statique</title>
                <style>
                :root { --bg:#0f1419; --card:#1a2332; --ink:#e8eef7; --muted:#9aa8bc; --accent:#7dd3fc; }
                body { margin:0; font-family:Georgia, 'Times New Roman', serif; background:var(--bg); color:var(--ink); }
                header { padding:28px 36px 16px; border-bottom:1px solid #2a3a50; }
                header h1 { margin:0; font-size:28px; letter-spacing:.02em; }
                header p { margin:8px 0 0; color:var(--muted); }
                main { padding:24px 36px 48px; display:grid; gap:22px; }
                section { background:var(--card); border-radius:14px; padding:18px 22px; }
                h2 { margin:0 0 12px; font-size:18px; color:var(--accent); }
                table { border-collapse:collapse; width:100%; font-size:14px; }
                th, td { text-align:left; padding:6px 8px; border-bottom:1px solid #2a3a50; }
                th { color:var(--muted); font-weight:normal; }
                .kpis { display:flex; flex-wrap:wrap; gap:12px; }
                .kpi { background:#121a26; border-radius:10px; padding:10px 14px; min-width:110px; }
                .kpi b { display:block; font-size:22px; }
                .kpi span { color:var(--muted); font-size:12px; }
                svg { width:100%; height:auto; background:#121a26; border-radius:10px; }
                .hint { color:var(--muted); font-size:13px; margin:8px 0 12px; }
                .edge { stroke:#5b6b80; stroke-width:1.2; fill:none; marker-end:url(#arrow); }
                .edge.on { stroke:#fcd34d; stroke-width:2.4; }
                .node { cursor:pointer; }
                .node circle { stroke:#0f1419; stroke-width:2; }
                .node.on circle { stroke:#fcd34d; stroke-width:3; }
                .node text { font-family:Calibri, sans-serif; font-size:11px; fill:#e8eef7; }
                .legend span { display:inline-block; width:10px; height:10px; border-radius:50%; margin-right:6px; }
                .legend div { display:inline-flex; align-items:center; margin:0 14px 6px 0; color:var(--muted); font-size:13px; }
                </style>
                </head>
                <body>
                """);
        html.append("<header><h1>Analyse statique — HAI913I TP1</h1><p>")
                .append(esc(facts.sourceRoot().toString()))
                .append(" · ").append(facts.compilationUnitCount()).append(" unites · ")
                .append(facts.errorCount()).append(" erreur(s)</p></header><main>");

        html.append("<section><h2>Points de controle</h2><div class=\"kpis\">");
        kpi(html, String.valueOf(metrics.classCount()), "classes (Q1)");
        kpi(html, String.valueOf(metrics.applicationLoc()), "LOC (Q2)");
        kpi(html, String.valueOf(metrics.methodCount()), "methodes (Q3)");
        kpi(html, String.valueOf(metrics.packageCount()), "paquetages (Q4)");
        kpi(html, MetricsReport.formatAverage(metrics.methodsPerClass()), "meth./classe");
        kpi(html, String.valueOf(graph.nodes().size()), "noeuds graphe");
        kpi(html, String.valueOf(graph.edges().size()), "arcs");
        kpi(html, String.valueOf(graph.internalSites()), "sites internes");
        html.append("</div></section>");

        html.append("<section><h2>Metrques du sujet</h2><pre style=\"white-space:pre-wrap;font-family:Consolas,monospace;font-size:13px;\">")
                .append(esc(metrics.asText()))
                .append("</pre></section>");

        html.append("<section><h2>Graphe d'appel</h2>");
        html.append("<p class=\"hint\">Cliquez un nœud : ses appelantes et appelees s'allument (analyse d'impact). Couleur = paquetage.</p>");
        html.append(legend(facts));
        html.append(svg(graph, facts));
        html.append("</section>");

        html.append("<section><h2>Couplage inter-classes (variante TP2)</h2>");
        html.append("<p class=\"hint\">A couple B s'il existe un appel d'une methode de A vers une methode de B, A ≠ B. Poids = somme des sites.</p>");
        html.append("<table><tr><th>Source</th><th>Cible</th><th>Poids</th></tr>");
        for (ClassCoupling.Link link : coupling.links()) {
            html.append("<tr><td>").append(esc(link.fromType())).append("</td><td>")
                    .append(esc(link.toType())).append("</td><td>").append(link.weight()).append("</td></tr>");
        }
        html.append("</table><table style=\"margin-top:16px\"><tr><th>Type</th><th>Fan-out</th><th>Fan-in</th><th>Force</th></tr>");
        for (ClassCoupling.TypeScore score : coupling.scores()) {
            html.append("<tr><td>").append(esc(score.type())).append("</td><td>").append(score.fanOut())
                    .append("</td><td>").append(score.fanIn()).append("</td><td>").append(score.strength())
                    .append("</td></tr>");
        }
        html.append("</table></section>");

        html.append("""
                </main>
                <script>
                const nodes = [...document.querySelectorAll('.node')];
                const edges = [...document.querySelectorAll('.edge')];
                nodes.forEach(n => n.addEventListener('click', () => {
                  const id = n.dataset.id;
                  nodes.forEach(x => x.classList.toggle('on', x.dataset.id === id
                      || edges.some(e => (e.dataset.from === id && e.dataset.to === x.dataset.id)
                                      || (e.dataset.to === id && e.dataset.from === x.dataset.id))));
                  edges.forEach(e => e.classList.toggle('on', e.dataset.from === id || e.dataset.to === id));
                }));
                </script>
                </body></html>
                """);
        return html.toString();
    }

    private static void kpi(StringBuilder html, String value, String label) {
        html.append("<div class=\"kpi\"><b>").append(esc(value)).append("</b><span>")
                .append(esc(label)).append("</span></div>");
    }

    private static String legend(ProjectFacts facts) {
        Map<String, String> colors = packageColors(facts);
        StringBuilder out = new StringBuilder("<div class=\"legend\">");
        for (Map.Entry<String, String> e : colors.entrySet()) {
            out.append("<div><span style=\"background:").append(e.getValue()).append("\"></span>")
                    .append(esc(e.getKey())).append("</div>");
        }
        return out.append("</div>").toString();
    }

    private static Map<String, String> packageColors(ProjectFacts facts) {
        Map<String, String> colors = new LinkedHashMap<>();
        int i = 0;
        for (TypeFact type : facts.types()) {
            colors.putIfAbsent(type.packageName(), PALETTE[i++ % PALETTE.length]);
        }
        return colors;
    }

    private static String svg(CallGraph graph, ProjectFacts facts) {
        List<String> visible = graph.nodes().stream()
                .filter(n -> graph.edges().stream().anyMatch(e -> e.from().equals(n) || e.to().equals(n)))
                .toList();
        if (visible.isEmpty()) {
            return "<p>Aucun arc a dessiner.</p>";
        }
        Map<String, String> colors = packageColors(facts);
        Map<String, String> typeToPkg = new LinkedHashMap<>();
        facts.types().forEach(t -> typeToPkg.put(t.qualifiedName(), t.packageName()));

        int width = 1100;
        int height = 720;
        double cx = width / 2.0;
        double cy = height / 2.0;
        double radius = 290;
        Map<String, double[]> pos = new LinkedHashMap<>();
        for (int i = 0; i < visible.size(); i++) {
            double angle = (2 * Math.PI * i / visible.size()) - Math.PI / 2;
            pos.put(visible.get(i), new double[] { cx + radius * Math.cos(angle), cy + radius * Math.sin(angle) });
        }

        StringBuilder svg = new StringBuilder();
        svg.append("<svg viewBox=\"0 0 ").append(width).append(' ').append(height).append("\" xmlns=\"http://www.w3.org/2000/svg\">");
        svg.append("<defs><marker id=\"arrow\" viewBox=\"0 0 10 10\" refX=\"10\" refY=\"5\" markerWidth=\"7\" markerHeight=\"7\" orient=\"auto-start-reverse\">")
                .append("<path d=\"M 0 0 L 10 5 L 0 10 z\" fill=\"#5b6b80\"/></marker></defs>");
        for (CallGraph.Edge edge : graph.edges()) {
            double[] a = pos.get(edge.from());
            double[] b = pos.get(edge.to());
            if (a == null || b == null) {
                continue;
            }
            svg.append("<line class=\"edge\" data-from=\"").append(esc(edge.from())).append("\" data-to=\"")
                    .append(esc(edge.to())).append("\" x1=\"").append(fmt(a[0])).append("\" y1=\"").append(fmt(a[1]))
                    .append("\" x2=\"").append(fmt(b[0])).append("\" y2=\"").append(fmt(b[1])).append("\"/>");
        }
        for (String node : visible) {
            double[] p = pos.get(node);
            String pkg = typeToPkg.getOrDefault(CallGraph.ownerType(node), "(défaut)");
            String color = colors.getOrDefault(pkg, "#7dd3fc");
            svg.append("<g class=\"node\" data-id=\"").append(esc(node)).append("\">");
            svg.append("<circle cx=\"").append(fmt(p[0])).append("\" cy=\"").append(fmt(p[1]))
                    .append("\" r=\"11\" fill=\"").append(color).append("\"/>");
            svg.append("<text x=\"").append(fmt(p[0] + 14)).append("\" y=\"").append(fmt(p[1] + 4)).append("\">")
                    .append(esc(CallGraph.shortLabel(node))).append("</text></g>");
        }
        svg.append("</svg>");
        return svg.toString();
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private static String esc(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
