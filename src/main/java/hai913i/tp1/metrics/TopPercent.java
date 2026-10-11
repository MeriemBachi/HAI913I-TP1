package hai913i.tp1.metrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * « Les 10 % » du sujet § 4.3 : k = ceil(n/10) = (n+9)/10, égalités retenues,
 * les valeurs nulles jamais retenues.
 */
public final class TopPercent {

    private TopPercent() {
    }

    public static int k(int n) {
        if (n <= 0) {
            return 0;
        }
        return (n + 9) / 10;
    }

    public static <T> List<T> retain(List<T> candidates, ToIntFunction<T> value) {
        int n = candidates.size();
        int keep = k(n);
        if (keep == 0) {
            return List.of();
        }
        List<T> sorted = new ArrayList<>(candidates);
        sorted.sort(Comparator.comparingInt(value).reversed());
        int threshold = value.applyAsInt(sorted.get(keep - 1));
        List<T> result = new ArrayList<>();
        for (T candidate : sorted) {
            int measured = value.applyAsInt(candidate);
            if (measured >= threshold && measured > 0) {
                result.add(candidate);
            }
        }
        return List.copyOf(result);
    }
}
