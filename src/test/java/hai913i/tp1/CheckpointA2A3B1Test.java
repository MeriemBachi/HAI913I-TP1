package hai913i.tp1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import hai913i.tp1.model.CallFact;
import hai913i.tp1.model.FieldFact;
import hai913i.tp1.model.MethodFact;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.model.TypeFact;
import hai913i.tp1.model.TypeKind;
import hai913i.tp1.model.Visibility;

class CheckpointA2A3B1Test {

    private static ProjectFacts facts;

    @BeforeAll
    static void load() throws Exception {
        facts = Fixtures.facts(Fixtures.validation());
    }

    @Test
    void a2_counts() {
        assertEquals(15, facts.types().size());
        long classes = facts.types().stream().filter(t -> t.kind() == TypeKind.CLASS).count();
        long interfaces = facts.types().stream().filter(t -> t.kind() == TypeKind.INTERFACE).count();
        long enums = facts.types().stream().filter(t -> t.kind() == TypeKind.ENUM).count();
        assertEquals(12, classes);
        assertEquals(2, interfaces);
        assertEquals(1, enums);
        assertEquals(59, facts.methods().size());
        assertEquals(9, facts.constructorCount());
        assertEquals(21, facts.types().stream().mapToInt(TypeFact::fieldCount).sum());
    }

    @Test
    void a2_loanServiceAndNestedLoan() {
        TypeFact service = facts.type("library.service.LoanService").orElseThrow();
        TypeFact loan = facts.type("library.service.LoanService.Loan").orElseThrow();
        assertEquals(6, service.methodCount());
        assertEquals(5, service.fieldCount());
        assertEquals(4, loan.methodCount());
        assertEquals(3, loan.fieldCount());
    }

    @Test
    void a2_loanExceptionSuperclasses() {
        TypeFact type = facts.type("library.service.LoanException").orElseThrow();
        assertEquals(List.of("java.lang.Exception", "java.lang.Throwable"), type.superclasses());
    }

    @Test
    void a2_itemFieldVisibility() {
        TypeFact item = facts.type("library.model.Item").orElseThrow();
        assertEquals(Visibility.PUBLIC, field(item, "MAX_LOAN_DAYS").visibility());
        assertEquals(Visibility.PROTECTED, field(item, "id").visibility());
        assertEquals(Visibility.PROTECTED, field(item, "title").visibility());
        assertEquals(Visibility.PRIVATE, field(item, "borrower").visibility());
        assertEquals(Visibility.PACKAGE, field(item, "category").visibility());
    }

    @Test
    void a3_callCounts() {
        List<CallFact> calls = facts.invocationCalls();
        assertEquals(94, calls.size());
        long methodInv = calls.stream().filter(c -> c.kind().name().equals("INVOCATION")).count();
        long superInv = calls.stream().filter(c -> c.kind().name().equals("SUPER")).count();
        assertEquals(93, methodInv);
        assertEquals(1, superInv);
        assertEquals(46, calls.stream().filter(CallFact::internal).count());
        assertEquals(48, calls.stream().filter(c -> c.resolved() && !c.internal()).count());
        assertEquals(0, calls.stream().filter(c -> !c.resolved()).count());
    }

    @Test
    void a3_loanableReceiverNotItem() {
        MethodFact borrow = facts.method(
                "library.service.LoanService#borrow(library.model.Member,java.lang.String,int)").orElseThrow();
        CallFact checkOut = borrow.calls().stream()
                .filter(c -> c.sourceLine() == 36 && c.targetName().equals("checkOut"))
                .findFirst()
                .orElseThrow();
        assertEquals("library.model.Loanable", checkOut.staticReceiverType());
        assertEquals("library.model.Loanable#checkOut(library.model.Member)", checkOut.targetId().orElseThrow());
    }

    @Test
    void a3_inheritedGetTitle() {
        MethodFact main = facts.method("library.app.LibraryApp#main(java.lang.String[])").orElseThrow();
        CallFact getTitle = main.calls().stream()
                .filter(c -> c.sourceLine() == 32 && c.targetName().equals("getTitle"))
                .findFirst()
                .orElseThrow();
        assertEquals("library.model.Book", getTitle.staticReceiverType());
        assertEquals("library.model.Item#getTitle()", getTitle.targetId().orElseThrow());
    }

    @Test
    void a3_superDescribeAndLambda() {
        MethodFact describe = facts.method("library.model.Book#describe()").orElseThrow();
        CallFact superCall = describe.calls().stream()
                .filter(c -> c.sourceLine() == 32)
                .findFirst()
                .orElseThrow();
        assertEquals("library.model.Item", superCall.staticReceiverType());

        MethodFact available = facts.method("library.service.Catalog#available()").orElseThrow();
        CallFact isAvailable = available.calls().stream()
                .filter(c -> c.sourceLine() == 37 && c.targetName().equals("isAvailable"))
                .findFirst()
                .orElseThrow();
        assertEquals("library.model.Item", isAvailable.staticReceiverType());
    }

    @Test
    void a3_missingDepPartialResolution() throws Exception {
        ProjectFacts missing = Fixtures.facts(Fixtures.robustness("missing-dep"));
        List<CallFact> calls = missing.invocationCalls();
        CallFact capitalize = calls.stream().filter(c -> c.targetName().equals("capitalize")).findFirst().orElseThrow();
        CallFact decorate = calls.stream().filter(c -> c.targetName().equals("decorate")).findFirst().orElseThrow();
        assertTrue(!capitalize.resolved(), "StringUtils.capitalize doit etre non resolu");
        assertTrue(decorate.resolved(), "formatter.decorate doit etre resolu");
    }

    @Test
    void b1_deterministic() throws Exception {
        ProjectFacts again = Fixtures.facts(Fixtures.validation());
        assertEquals(facts.types(), again.types());
        assertEquals(facts.invocationCalls(), again.invocationCalls());
    }

    private static FieldFact field(TypeFact type, String name) {
        return type.fields().stream().filter(f -> f.name().equals(name)).findFirst().orElseThrow();
    }
}
