package net.fortytwo.myotherbrain.notes.util;

import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.KeyIndexableGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.tg.TinkerGraph;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLReader;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;
import net.fortytwo.myotherbrain.MOBGraph;
import net.fortytwo.myotherbrain.MyOtherBrain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class ListGraphMigrator {
    public void migrate(final KeyIndexableGraph source,
                        final KeyIndexableGraph target) {
        for (Vertex vs : source.getVertices()) {
            Vertex vt = target.addVertex(vs.getId());

            for (String key : vs.getPropertyKeys()) {
                vt.setProperty(key, vs.getProperty(key));
            }
        }

        for (Vertex vs : source.getVertices()) {
            Vertex vt = target.getVertex(vs.getId());

            List<Vertex> list = new LinkedList<Vertex>();
            for (Edge e : vs.getEdges(Direction.OUT)) {
                list.add(e.getVertex(Direction.IN));
            }

            if (list.size() > 0) {
                Collections.sort(list, new VertexTimestampComparator());
                Vertex last = null;

                for (int i = 0; i < list.size(); i++) {
                    Vertex cur = target.addVertex(null);

                    if (i > 0) {
                        target.addEdge(null, last, cur, MyOtherBrain.REST);
                    } else {
                        target.addEdge(null, vt, cur, MyOtherBrain.NOTES);
                    }

                    Vertex v2s = list.get(i);
                    Vertex v2t = target.getVertex(v2s.getId());

                    target.addEdge(null, cur, v2t, MyOtherBrain.FIRST);
                    last = cur;
                }
            }
        }
    }

    private class VertexTimestampComparator implements Comparator<Vertex> {
        public int compare(final Vertex v1,
                           final Vertex v2) {
            return ((Long) v1.getProperty(MyOtherBrain.CREATED)).compareTo((Long) v2.getProperty(MyOtherBrain.CREATED));
        }
    }

    public static void main(final String[] args) throws Exception {
        File dirs = new File("/tmp/tinkernotes-migration/source");
        File dirt = new File("/tmp/tinkernotes-migration/target");
        if (dirs.exists()) {
            dirs.delete();
        }
        if (dirt.exists()) {
            dirt.delete();
        }

        //String dirIn = dirs.getAbsolutePath();
        //System.out.println("loading original data into database at " + dirIn);
        //KeyIndexableGraph graphIn = new Neo4jGraph(dirIn);
        KeyIndexableGraph graphIn = new TinkerGraph();
        MOBGraph source = MOBGraph.getInstance(graphIn);

        String fileIn = "/Volumes/encrypted/mob-data/tinkernotes/tinkernotes.xml";
        System.out.println("reading graph from " + fileIn);
        GraphMLReader r = new GraphMLReader(source.getGraph());
        InputStream in = new FileInputStream(fileIn);
        try {
            r.inputGraph(in);
        } finally {
            in.close();
        }

        //String dirOut = dirt.getAbsolutePath();
        //System.out.println("migrating graph into database at " + dirOut);
        //KeyIndexableGraph graphOut = new Neo4jGraph(dirOut);
        KeyIndexableGraph graphOut = new TinkerGraph();
        MOBGraph target = MOBGraph.getInstance(graphOut);

        new ListGraphMigrator().migrate(source.getGraph(), target.getGraph());
        source.getGraph().shutdown();

        String fileOut = "/tmp/tinkernotes-migrated.xml";
        System.out.println("writing graph to " + fileOut);
        GraphMLWriter w = new GraphMLWriter(target.getGraph());
        w.setNormalize(true);
        OutputStream out = new FileOutputStream(fileOut);
        try {
            w.outputGraph(out);
        } finally {
            out.close();
        }

        target.getGraph().shutdown();
    }
}
