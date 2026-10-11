package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.metrics.ClassCoupling;
import hai913i.tp1.metrics.MetricsReport;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.report.HtmlReport;
import hai913i.tp1.report.JsonReport;

class ExtrasTest {

    @Test
    void couplingConnectsLoanServiceToCatalog() throws Exception {
        ProjectFacts facts = Fixtures.facts(Fixtures.validation());
        ClassCoupling coupling = ClassCoupling.of(CallGraph.build(facts, false, false));
        assertTrue(coupling.links().stream().anyMatch(l ->
                l.fromType().equals("library.service.LoanService")
                        && l.toType().equals("library.service.Catalog")));
        assertTrue(coupling.scores().stream().anyMatch(s ->
                s.type().equals("library.service.LoanService") && s.fanOut() > 0));
    }

    @Test
    void packageFilterKeepsServiceEdges() throws Exception {
        ProjectFacts facts = Fixtures.facts(Fixtures.validation());
        CallGraph full = CallGraph.build(facts, false, false);
        CallGraph filtered = full.restrictToPackage("library.util");
        assertTrue(filtered.edges().size() < full.edges().size());
        assertTrue(filtered.edges().stream().allMatch(e ->
                e.from().startsWith("library.util.") || e.to().startsWith("library.util.")));
    }

    @Test
    void htmlAndJsonAreSelfContained() throws Exception {
        ProjectFacts facts = Fixtures.facts(Fixtures.validation());
        MetricsReport metrics = new MetricsReport(facts, 5);
        CallGraph graph = CallGraph.build(facts, false, false);
        String html = HtmlReport.render(facts, metrics, graph);
        assertTrue(html.contains("<svg"));
        assertTrue(html.contains("Couplage"));
        assertFalse(html.contains("cdn.") || html.contains("unpkg") || html.contains("jsdelivr"));
        String json = JsonReport.render(facts, metrics, graph);
        assertTrue(json.contains("\"classes\": 15"));
        assertTrue(json.contains("classCoupling"));
    }
}
