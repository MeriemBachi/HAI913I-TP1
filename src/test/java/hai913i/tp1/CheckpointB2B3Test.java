package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import hai913i.tp1.callgraph.CallGraph;
import hai913i.tp1.metrics.MetricsReport;
import hai913i.tp1.metrics.TopPercent;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;

class CheckpointB2B3Test {

    private static ProjectFacts facts;
    private static MetricsReport metrics;
    private static CallGraph graph;

    @BeforeAll
    static void load() throws Exception {
        facts = Fixtures.facts(Fixtures.validation());
        metrics = new MetricsReport(facts, 5);
        graph = CallGraph.build(facts, false, false);
    }

    @Test
    void b2_basicMetrics() {
        assertEquals(15, metrics.classCount());
        assertEquals(493, metrics.applicationLoc());
        assertEquals(59, metrics.methodCount());
        assertEquals(4, metrics.packageCount());
        assertEquals("3,93", MetricsReport.formatAverage(metrics.methodsPerClass()));
        assertEquals(246, metrics.bodyLocSum());
        assertEquals(53, metrics.methodsWithBody());
        assertEquals("4,64", MetricsReport.formatAverage(metrics.locPerMethod()));
        assertEquals("1,40", MetricsReport.formatAverage(metrics.fieldsPerClass()));
    }

    @Test
    void b2_topPercentAndX() {
        assertEquals(2, TopPercent.k(15));
        assertEquals(3, metrics.topMethods().size());
        assertTrue(names(metrics.topMethods()).contains("library.model.Item"));
        assertTrue(names(metrics.topMethods()).contains("library.model.Member"));
        assertTrue(names(metrics.topMethods()).contains("library.service.LoanService"));

        assertEquals(2, metrics.topFields().size());
        assertTrue(names(metrics.topFields()).contains("library.model.Item"));
        assertTrue(names(metrics.topFields()).contains("library.service.LoanService"));
        assertEquals(names(metrics.topFields()), names(metrics.intersection()));

        assertEquals(3, metrics.moreThanXMethods().size());
        MetricsReport x4 = new MetricsReport(facts, 4);
        assertEquals(6, x4.moreThanXMethods().size());
    }

    @Test
    void b2_question12and13() {
        TypeFact item = facts.type("library.model.Item").orElseThrow();
        assertEquals(List.of("checkOut"), metrics.topMethodsOf(item).stream().map(m -> m.name()).toList());
        assertEquals(7, metrics.topMethodsOf(item).get(0).bodyLoc());

        TypeFact service = facts.type("library.service.LoanService").orElseThrow();
        assertEquals(List.of("borrow"), metrics.topMethodsOf(service).stream().map(m -> m.name()).toList());
        assertEquals(14, metrics.topMethodsOf(service).get(0).bodyLoc());

        TypeFact member = facts.type("library.model.Member").orElseThrow();
        assertEquals(6, metrics.topMethodsOf(member).size());
        assertTrue(metrics.topMethodsOf(member).stream().allMatch(m -> m.bodyLoc() == 3));

        TypeFact loanable = facts.type("library.model.Loanable").orElseThrow();
        assertTrue(metrics.topMethodsOf(loanable).isEmpty());

        assertEquals(5, metrics.maxParameters());
        assertEquals(List.of("library.util.Statistics#report(java.lang.String,int,int,double,int)"),
                metrics.methodsWithMaxParameters().stream().map(m -> m.id()).toList());
    }

    @Test
    void b3_graphShape() {
        assertEquals(59, graph.nodes().size());
        assertEquals(44, graph.edges().size());
        assertEquals(46, graph.internalSites());
        assertEquals(48, graph.externalCalls());
        assertEquals(0, graph.unresolvedCalls());
    }

    @Test
    void b3_specificEdges() {
        assertTrue(graph.edges().stream().anyMatch(e ->
                e.from().equals("library.service.Catalog#add(library.model.Item,library.model.Item)")
                        && e.to().equals("library.service.Catalog#add(library.model.Item)")
                        && e.weight() == 2));
        assertTrue(graph.edges().stream().anyMatch(e ->
                e.from().equals("library.util.TextUtils#repeat(java.lang.String,int)")
                        && e.to().equals("library.util.TextUtils#repeat(java.lang.String,int)")));

        String checkOut = "library.model.Loanable#checkOut(library.model.Member)";
        assertEquals(List.of("library.service.LoanService#borrow(library.model.Member,java.lang.String,int)"),
                graph.callers(checkOut).stream().map(CallGraph.Edge::from).toList());

        String itemCheckOut = "library.model.Item#checkOut(library.model.Member)";
        assertTrue(graph.callers(itemCheckOut).isEmpty());
    }

    @Test
    void b3_constructorsOption() {
        CallGraph withCtor = CallGraph.build(facts, true, false);
        assertEquals(52, withCtor.edges().size());
        assertEquals(56, withCtor.internalSites());
    }

    @Test
    void c3_chaAddsOverrideEdges() {
        CallGraph cha = CallGraph.build(facts, false, true);
        String itemCheckOut = "library.model.Item#checkOut(library.model.Member)";
        assertTrue(cha.callers(itemCheckOut).stream().anyMatch(e ->
                e.from().equals("library.service.LoanService#borrow(library.model.Member,java.lang.String,int)")));
    }

    @Test
    void topPercentDoesNotKeepZeros() {
        record Item(String name, int value) {
        }
        var kept = TopPercent.retain(
                List.of(new Item("a", 0), new Item("b", 0), new Item("c", 0)),
                Item::value);
        assertTrue(kept.isEmpty());
        assertEquals(2, TopPercent.k(15));
    }

    private static List<String> names(List<TypeFact> types) {
        return types.stream().map(TypeFact::qualifiedName).toList();
    }
}
