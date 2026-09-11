package org.webjcvi.report;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.webjcvi.dna.DnaSequence;

/**
 * Structured, extensible DNA/code report. Section content is free to evolve
 * iteration by iteration; callers should treat {@link #sections()} as the
 * stable extension point rather than assuming a fixed schema.
 */
public final class DnaReport {

    private final Instant generatedAt;
    private final DnaSequence sequence;
    private final Map<String, String> sections;
    private final String markdown;

    public DnaReport(
            Instant generatedAt,
            DnaSequence sequence,
            Map<String, String> sections,
            String markdown) {
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt");
        this.sequence = Objects.requireNonNull(sequence, "sequence");
        this.sections = Collections.unmodifiableMap(new LinkedHashMap<>(sections));
        this.markdown = Objects.requireNonNull(markdown, "markdown");
    }

    public Instant generatedAt() {
        return generatedAt;
    }

    public DnaSequence sequence() {
        return sequence;
    }

    public Map<String, String> sections() {
        return sections;
    }

    public String markdown() {
        return markdown;
    }

    public List<String> sectionNames() {
        return List.copyOf(sections.keySet());
    }
}
