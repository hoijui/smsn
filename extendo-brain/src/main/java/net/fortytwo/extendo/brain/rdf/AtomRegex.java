package net.fortytwo.extendo.brain.rdf;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class AtomRegex {

    public enum Modifier {
        ZeroOrOne, ZeroOrMore, One, OneOrMore
    }

    private final List<El> elements;

    public AtomRegex(List<El> elements) {
        this.elements = elements;
    }

    public List<El> getElements() {
        return elements;
    }

    public static class El {
        private final AtomClass.FieldHandler fieldHandler;
        private final Modifier modifier;
        private final Set<Class<? extends AtomClass>> alternatives;

        public El(final AtomClass.FieldHandler fieldHandler,
                  final Modifier modifier,
                  final Class<? extends AtomClass>... alternatives) {
            this.fieldHandler = fieldHandler;
            this.modifier = modifier;
            this.alternatives = new HashSet<Class<? extends AtomClass>>();
            Collections.addAll(this.alternatives, alternatives);
        }

        public AtomClass.FieldHandler getFieldHandler() {
            return fieldHandler;
        }

        public Modifier getModifier() {
            return modifier;
        }

        public Set<Class<? extends AtomClass>> getAlternatives() {
            return alternatives;
        }
    }
}