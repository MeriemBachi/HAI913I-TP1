package hai913i.tp1;

import java.nio.file.Path;
import java.util.List;

import hai913i.tp1.extract.FactExtractor;
import hai913i.tp1.model.ProjectFacts;
import hai913i.tp1.parse.JdtParser;
import hai913i.tp1.parse.ProjectSources;

final class Fixtures {

    private Fixtures() {
    }

    static Path validation() {
        return Path.of("resources/validation");
    }

    static Path robustness(String sample) {
        return Path.of("resources/robustness").resolve(sample);
    }

    static ProjectFacts facts(Path project) throws Exception {
        ProjectSources sources = ProjectSources.of(project);
        return FactExtractor.extract(sources, JdtParser.parse(sources, List.of()));
    }
}
