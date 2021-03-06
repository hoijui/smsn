package net.fortytwo.smsn.server.actions;

import net.fortytwo.smsn.brain.io.BrainWriter;
import net.fortytwo.smsn.brain.io.Format;
import net.fortytwo.smsn.server.ActionContext;
import net.fortytwo.smsn.server.errors.BadRequestException;
import net.fortytwo.smsn.server.errors.RequestProcessingException;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A service for exporting an Extend-o-Brain graph to the file system
 */
public class WriteGraph extends IOAction {

    // note: may be null
    private String rootId;

    private String getRootId() {
        return rootId;
    }

    public void setRootId(String rootId) {
        this.rootId = rootId;
    }

    @Override
    protected void performTransaction(final ActionContext params) throws RequestProcessingException, BadRequestException {

        BrainWriter.Context context = new BrainWriter.Context();
        context.setTopicGraph(params.getBrain().getTopicGraph());
        context.setKnowledgeBase(params.getBrain().getKnowledgeBase());
        context.setRootId(getRootId());
        context.setFilter(getFilter());
        context.setFormat(getFormat());
        BrainWriter writer = Format.getWriter(getFormat());

        try {
            if (getFormat().getType().equals(Format.Type.FileBased)) {
                try (OutputStream destStream = new FileOutputStream(getFile())) {
                    context.setDestStream(destStream);
                    writer.doExport(context);
                }
            } else {
                context.setDestDirectory(getFile());
                writer.doExport(context);
            }
        } catch (IOException e) {
            throw new RequestProcessingException(e);
        }
    }

    @Override
    protected boolean doesRead() {
        // doesn't read, in that no data is returned by the service (data is only written to the file system)
        return false;
    }

    @Override
    protected boolean doesWrite() {
        return false;
    }
}
