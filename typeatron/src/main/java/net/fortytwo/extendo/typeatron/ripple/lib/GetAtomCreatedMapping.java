package net.fortytwo.extendo.typeatron.ripple.lib;

import net.fortytwo.extendo.brain.Filter;
import net.fortytwo.extendo.brain.Note;
import net.fortytwo.extendo.typeatron.ripple.ExtendoBrainClient;
import net.fortytwo.flow.Sink;
import net.fortytwo.ripple.RippleException;
import net.fortytwo.ripple.model.ModelConnection;
import net.fortytwo.ripple.model.RippleList;

import java.util.logging.Logger;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class GetAtomCreatedMapping extends AtomMapping {

    private static final Logger logger = Logger.getLogger(GetAtomCreatedMapping.class.getName());

    public GetAtomCreatedMapping(final ExtendoBrainClient client,
                                 final Filter filter) {
        super(client, filter);
    }

    public String[] getIdentifiers() {
        return new String[]{
                ExtendoLibrary.NS_2014_12 + "get-atom-created"
        };
    }

    public Parameter[] getParameters() {
        return new Parameter[]{new Parameter("atom", "the reference atom", true)};
    }

    public String getComment() {
        return "gets the @created property of an atom";
    }

    public void apply(RippleList stack,
                      final Sink<RippleList> solutions,
                      final ModelConnection mc) throws RippleException {

        Object first = stack.getFirst();
        stack = stack.getRest();

        Note n = toNote(first, true);

        if (null == n) {
            logger.warning("can't get @created of non-atom: " + first);
        } else {
            Long value = n.getCreated();
            if (null != value) {
                // put both the @created property and the (synced) atom back on the stack
                solutions.put(stack.push(n).push(value));
            }
        }
    }
}