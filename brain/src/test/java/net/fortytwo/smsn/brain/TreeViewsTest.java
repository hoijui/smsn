package net.fortytwo.smsn.brain;

import net.fortytwo.smsn.brain.io.json.JsonWriter;
import net.fortytwo.smsn.brain.model.Atom;
import net.fortytwo.smsn.brain.model.AtomGraph;
import net.fortytwo.smsn.brain.model.AtomList;
import net.fortytwo.smsn.brain.model.Filter;
import net.fortytwo.smsn.brain.model.Note;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TreeViewsTest extends BrainTestBase {
    private final JsonWriter jsonWriter = new JsonWriter();
    private TreeViews queries;
    private Filter filter;

    @Override
    protected AtomGraph createAtomGraph() throws IOException {
        return createTinkerAtomGraph();
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Brain brain = new Brain(atomGraph);
        queries = new TreeViews(brain);
        filter = Filter.noFilter();
    }

    @Test
    public void testEncoding() throws Exception {
        TreeViews.ViewStyle style = TreeViews.forwardViewStyle;

        Atom root = createAtom("11111");
        assertEquals("11111", root.getId());

        //Note superRoot = new Note();
        Note rootNote = new Note();
        rootNote.setId(root.getId());
        //superRoot.addChild(rootNote);
        rootNote.setValue("foo");
        Note child = new Note();
        child.setValue("cheval \u00e0 phynances");
        rootNote.addChild(child);
        assertNull(child.getWeight());
        assertNull(child.getSharability());
        assertNull(child.getCreated());
        //System.out.println(before.getTargetValue());

        queries.update(rootNote, 1, filter, style);

        //new GraphMLWriter(graph).outputGraph(System.out);

        Note after = queries.view(root, 1, filter, style);

        assertEquals("11111", after.getId());
        assertEquals("foo", after.getValue());
        assertEquals(1, after.getChildren().size());

        JSONObject json = jsonWriter.toJson(after);
        //System.out.println(json.toString());
        JSONObject j = json.getJSONArray("children").getJSONObject(0);
        assertEquals("cheval \u00e0 phynances", j.getString("value"));
    }

    @Test
    public void testUpdateRecursion() throws Exception {
        TreeViews.ViewStyle style = TreeViews.forwardViewStyle;
        Atom root = createAtom("wXu5g4v");
        root.setValue("root");
        Note rootNote;
        String s;

        s = "" +
                "* :N5KBOAq: one\n" +
                "* :v8EuMtl: two\n" +
                "* :tOpwKho: three\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        assertNotesEqual(root, "one", "two", "three");

        Atom one = atomGraph.getAtomById("N5KBOAq");
        Atom two = atomGraph.getAtomById("v8EuMtl");

        s = "" +
                "* :N5KBOAq: one\n" +
                "    * :r4zU45R: ten\n" +
                "    * yellow\n" +
                "* :tOpwKho: three\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        // "two" has been removed
        assertNotesEqual(root, "one", "three");
        // grandchildren have been added
        assertNotesEqual(one, "ten", "yellow");
        Atom ten = atomGraph.getAtomById("r4zU45R");

        s = "" +
                "* :N5KBOAq: one\n" +
                "    * :r4zU45R: ten\n" +
                "        * rabbit\n" +
                "    * purple\n" +
                "* :tOpwKho: three\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        // depth is only two, so "rabbit" is not reachable
        assertNotesEqual(ten);

        s = "" +
                "* :N5KBOAq: one\n" +
                "    * :r4zU45R: ten\n" +
                "    * green\n" +
                "        * rabbit\n" +
                "        * kangaroo\n" +
                "* :tOpwKho: three\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        Atom green = one.getNotes().getRest().getFirst();
        // "rabbit" and "kangaroo" are added beneath "green" even though they're
        // deeper than 2 steps in the tree, because "green" is a new note
        assertNotesEqual(green, "rabbit", "kangaroo");

        s = "" +
                "* :v8EuMtl: two\n" +
                "* :tOpwKho: three\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        // "one" has been removed...
        assertNotesEqual(root, "two", "three");
        // but "one" still exists and has its previous notes
        assertNotesEqual(one, "ten", "green");

        s = "" +
                "* :tOpwKho: three\n" +
                "    * red\n" +
                "* :v8EuMtl: two\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        // we swapped the order of "two" and "three"...
        assertNotesEqual(root, "three", "two");
        Atom three = atomGraph.getAtomById("tOpwKho");
        // ...therefore, the children of "three" can't be modified in this update operation
        // (so "red" has been ignored)
        assertNotesEqual(three);

        s = "" +
                "* :v8EuMtl: two\n" +
                "    * elephant\n" +
                "* :v8EuMtl: two\n" +
                "* :tOpwKho: three\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        // duplicates are possible...
        assertNotesEqual(root, "two", "two", "three");
        // ...but when a duplicate is added, children of any matching duplicate will be ignored
        assertNotesEqual(two);

        s = "" +
                "* :v8EuMtl: two\n" +
                "    * elephant\n" +
                "* :v8EuMtl: two\n" +
                "    * gorilla\n" +
                "* :tOpwKho: three\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        assertNotesEqual(root, "two", "two", "three");
        // when duplicates already exist, children of duplicates follow the last-occurring instance
        assertNotesEqual(two, "gorilla");
    }

    @Test
    public void testPathologicalUpdateWithCycles() throws Exception {
        TreeViews.ViewStyle style = TreeViews.forwardViewStyle;
        Atom root = createAtom("0000000");
        root.setValue("root");
        Note rootNote, child, grandChild;
        String s;

        // OK to create an atom which is its own parent
        s = "" +
                "* :001: one\n" +
                "    * :001: one\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        Atom one = atomGraph.getAtomById("001");
        assertNotNull(one);
        Assert.assertEquals(1, one.getNotes().toJavaList().size());
        for (int i = 0; i < 2; i++) {
            assertEquals(1, rootNote.getChildren().size());
            child = rootNote.getChildren().get(0);
            Assert.assertEquals("001", child.getId());
            assertEquals(1, child.getChildren().size());
            grandChild = child.getChildren().get(0);
            assertEquals("001", grandChild.getId());

            rootNote = queries.view(root, 2, filter, style);
        }

        // setting properties at the higher level has no effect, as we are pre-ordered w.r.t. updating of properties
        s = "" +
                "* :001: one - updated\n" +
                "    * :001: one\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        one = atomGraph.getAtomById("001");
        assertNotNull(one);
        Assert.assertEquals(1, one.getNotes().toJavaList().size());
        rootNote = queries.view(root, 2, filter, style);
        assertEquals(1, rootNote.getChildren().size());
        child = rootNote.getChildren().get(0);
        Assert.assertEquals("001", child.getId());
        Assert.assertEquals("one", child.getValue());
        assertEquals(1, child.getChildren().size());
        grandChild = child.getChildren().get(0);
        assertEquals("001", grandChild.getId());

        // setting properties at the lower level (the last visited) does have an effect
        s = "" +
                "* :001: one\n" +
                "    * :001: one - updated\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        one = atomGraph.getAtomById("001");
        assertNotNull(one);
        Assert.assertEquals(1, one.getNotes().toJavaList().size());
        rootNote = queries.view(root, 2, filter, style);
        assertEquals(1, rootNote.getChildren().size());
        child = rootNote.getChildren().get(0);
        Assert.assertEquals("001", child.getId());
        Assert.assertEquals("one - updated", child.getValue());
        assertEquals(1, child.getChildren().size());
        grandChild = child.getChildren().get(0);
        assertEquals("001", grandChild.getId());

        // the preorder rule does not apply when the link from parent to child is not repeated in the view;
        // the children of an atom are updated only once
        s = "" +
                "* :001: one\n" +
                "    * :001: one\n" +
                "    * :002: two\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        one = atomGraph.getAtomById("001");
        assertNotNull(one);
        Assert.assertEquals(2, one.getNotes().toJavaList().size());
        for (int i = 0; i < 2; i++) {
            assertEquals(1, rootNote.getChildren().size());
            child = rootNote.getChildren().get(0);
            Assert.assertEquals("001", child.getId());
            assertEquals(2, child.getChildren().size());
            grandChild = child.getChildren().get(0);
            assertEquals("001", grandChild.getId());
            grandChild = child.getChildren().get(1);
            assertEquals("002", grandChild.getId());

            rootNote = queries.view(root, 2, filter, style);
        }

        // get the height-3 view
        rootNote = queries.view(root, 3, filter, style);
        assertEquals(1, rootNote.getChildren().size());
        child = rootNote.getChildren().get(0);
        Assert.assertEquals("001", child.getId());
        assertEquals(2, child.getChildren().size());
        grandChild = child.getChildren().get(0);
        assertEquals("001", grandChild.getId());
        Assert.assertEquals(2, grandChild.getChildren().size());
        grandChild = child.getChildren().get(1);
        assertEquals("002", grandChild.getId());
        Assert.assertEquals(0, grandChild.getChildren().size());

        // adding or removing at the higher level has no effect, as we are pre-ordered w.r.t. updating of children
        s = "" +
                "* :001: one\n" +
                "    * :001: one\n" +
                "        * :001: one\n" +
                "        * :002: two\n" +
                "    * :002: two\n" +
                "    * :003: three\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 3, filter, style);
        rootNote = queries.view(root, 3, filter, style);
        assertEquals(1, rootNote.getChildren().size());
        child = rootNote.getChildren().get(0);
        Assert.assertEquals("001", child.getId());
        assertEquals(2, child.getChildren().size());
        grandChild = child.getChildren().get(0);
        assertEquals("001", grandChild.getId());
        Assert.assertEquals(2, grandChild.getChildren().size());
        grandChild = child.getChildren().get(1);
        assertEquals("002", grandChild.getId());
        Assert.assertEquals(0, grandChild.getChildren().size());

        // adding or removing children at the lower level (the last visited) does have an effect
        s = "" +
                "* :001: one\n" +
                "    * :001: one\n" +
                "        * :001: one\n" +
                "        * :002: two\n" +
                "        * :003: three\n" +
                "    * :002: two\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 3, filter, style);
        rootNote = queries.view(root, 3, filter, style);
        assertEquals(1, rootNote.getChildren().size());
        child = rootNote.getChildren().get(0);
        Assert.assertEquals("001", child.getId());
        assertEquals(3, child.getChildren().size());
        grandChild = child.getChildren().get(0);
        assertEquals("001", grandChild.getId());
        Assert.assertEquals(3, grandChild.getChildren().size());
        grandChild = child.getChildren().get(1);
        assertEquals("002", grandChild.getId());
        Assert.assertEquals(0, grandChild.getChildren().size());
    }

    @Test
    public void testUpdateSharabilityOrWeight() throws Exception {
        TreeViews.ViewStyle style = TreeViews.forwardViewStyle;
        Atom root = createAtom("wXu5g4v");
        root.setValue("root");
        Note rootNote;
        String s;

        s = "" +
                "* :N5KBOAq: one\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        Atom one = atomGraph.getAtomById("N5KBOAq");
        assertEquals(0.5f, one.getWeight());
        assertEquals(0.5f, one.getSharability());

        s = "" +
                "* :N5KBOAq: one\n" +
                "    @weight 0.75\n" +
                "    @sharability 0.25\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        assertEquals(0.75f, one.getWeight());
        assertEquals(0.25f, one.getSharability());
    }

    @Test
    public void testUpdateAlias() throws Exception {
        TreeViews.ViewStyle style = TreeViews.forwardViewStyle;
        Atom root = createAtom("wXu5g4v");
        root.setValue("root");
        Note rootNote;
        String s;

        s = "" +
                "* :N5KBOAq: one\n" +
                "    @alias http://example.org/ns/one\n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        Atom one = atomGraph.getAtomById("N5KBOAq");
        assertEquals("http://example.org/ns/one", one.getAlias());

        s = "" +
                "* :N5KBOAq: one\n" +
                "    @alias \n";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        assertNull(one.getAlias());
    }

    @Test
    public void testUpdatePriority() throws Exception {
        TreeViews.ViewStyle style = TreeViews.forwardViewStyle;
        Atom root = createAtom("0000000");
        root.setValue("root");
        Note rootNote;
        String s;
        Atom one;

        s = "" +
                "* :0000001: one\n" +
                "    @priority 0.5";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        one = atomGraph.getAtomById("0000001");
        assertEquals(0.5f, one.getPriority());

        // setting priority to 0 has the effect of removing the priority property from the note
        s = "" +
                "* :0000001: one\n" +
                "    @priority 0";
        rootNote = wikiReader.parse(s);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, filter, style);
        one = atomGraph.getAtomById("0000001");
        assertNull(one.getPriority());
    }

    @Test
    public void childAndParentCountsAreCorrect() throws Exception {
        Filter filter = Filter.noFilter();
        TreeViews.ViewStyle style = TreeViews.forwardViewStyle;
        Note before = importNoteFromFile("io/wiki/wiki-example-3.txt");
        Atom root = createAtom("0000000");
        root.setValue("root");
        root.setSharability(1.0f);
        before.setId(root.getId());
        queries.update(before, 2, filter, style);
        Note after = queries.view(root, 2, filter, style);

        assertEquals("root", after.getValue());
        assertEquals(0, after.getNumberOfParents());
        assertEquals(3, after.getNumberOfChildren());

        Note child = after.getChildren().get(0);
        assertEquals("this is a public note", child.getValue());
        assertEquals(1, child.getNumberOfParents());
        assertEquals(2, child.getNumberOfChildren());
    }

    // TODO: test write behavior w.r.t. sharability filters
    @Test
    public void nonSharableItemsAreHidden() throws Exception {
        Filter readFilter = new Filter(0f, 1f, 0.5f, 0.75f, 1f, 0.75f);
        Filter writeFilter = new Filter(0f, 1f, 0.5f, 0f, 1f, 0.5f);
        TreeViews.ViewStyle style = TreeViews.forwardViewStyle;

        Note rootNote = importNoteFromFile("io/wiki/wiki-example-3.txt");
        Atom root = createAtom("0000000");
        root.setValue("root");
        root.setSharability(1.0f);
        rootNote.setId(root.getId());
        queries.update(rootNote, 2, writeFilter, style);

        Atom a1 = atomGraph.getAtomById("0000001");
        assertEquals(1f, a1.getSharability());
        Atom a2 = atomGraph.getAtomById("0000002");
        assertEquals(0.5f, a2.getSharability());

        Note after = queries.view(root, 2, readFilter, style);
        Assert.assertEquals(3, after.getNumberOfChildren());
        Assert.assertEquals(0, after.getNumberOfParents());
        List<Note> children = after.getChildren();
        Note n1 = children.get(0);
        Assert.assertEquals(2, n1.getNumberOfChildren());
        Assert.assertEquals(1, n1.getNumberOfParents());

        // This note is "invisible".
        Note n4 = children.get(1);
        // Its value appears as null.
        assertNull(n4.getValue());
        // This note has a child, but we can't see it.
        assertEquals(0, n4.getChildren().size());
        // We can't even count the parents or children.
        Assert.assertEquals(0, n4.getNumberOfChildren());
        Assert.assertEquals(0, n4.getNumberOfParents());

        Note n5 = children.get(2);
        // children exist, but are not visible
        Assert.assertEquals(0, n5.getNumberOfChildren());
        Assert.assertEquals(1, n5.getNumberOfParents());

        List<Note> grandChildren = n1.getChildren();
        assertEquals(2, grandChildren.size());
        Note n2 = grandChildren.get(0);
        Assert.assertEquals(0, n2.getNumberOfChildren());
        Assert.assertEquals(0, n2.getNumberOfParents());
        Note n3 = grandChildren.get(1);
        Assert.assertEquals(0, n3.getNumberOfChildren());
        Assert.assertEquals(1, n3.getNumberOfParents());
        assertNull(n2.getValue());
        assertEquals("this is a public child of a public note", n3.getValue());
    }

    @Test
    public void testDontOverwriteNotesWithEmptyValues() throws Exception {
        TreeViews.ViewStyle style = TreeViews.forwardViewStyle;

        String before = "* :001: one\n" +
                "* :002: two\n" +
                "* :003: three";
        String after = "* :001: ONE\n" +
                "* :002:\n" +
                "* :003: THREE";

        Note b = wikiReader.parse(before);
        Note a = wikiReader.parse(after);

        // First, check that 'after' was parsed correctly
        assertEquals(3, a.getChildren().size());
        assertEquals("002", a.getChildren().get(1).getId());
        assertNull(a.getChildren().get(1).getValue());

        Atom root = atomGraph.createAtomWithProperties(filter, "000");
        root.setValue("root");

        b.setId(root.getId());
        queries.update(b, 2, filter, style);

        Atom a1 = atomGraph.getAtomById("001");
        Atom a2 = atomGraph.getAtomById("002");
        Atom a3 = atomGraph.getAtomById("003");

        assertEquals("one", a1.getValue());
        assertEquals("two", a2.getValue());
        assertEquals("three", a3.getValue());

        a.setId(root.getId());
        queries.update(a, 2, filter, style);

        // 002's value was unaffected by the update
        assertEquals("ONE", a1.getValue());
        assertEquals("two", a2.getValue());
        assertEquals("THREE", a3.getValue());
    }

    @Test
    public void testAddOnlyUpdate() throws Exception {
        TreeViews.ViewStyle style = TreeViews.forwardAddOnlyViewStyle;

        String before = "* :001: one\n" +
                "* :002: two\n" +
                "* :003: three";
        String after = "* :004: four\n" +
                "* :002: two";

        Note b = wikiReader.parse(before);
        Note a = wikiReader.parse(after);

        Atom root = atomGraph.createAtomWithProperties(filter, "000");
        root.setValue("root");

        b.setId(root.getId());
        queries.update(b, 2, filter, style);

        Atom a1 = atomGraph.getAtomById("001");
        Atom a2 = atomGraph.getAtomById("002");
        Atom a3 = atomGraph.getAtomById("003");

        assertEquals("one", a1.getValue());
        assertEquals("two", a2.getValue());
        assertEquals("three", a3.getValue());
        assertEquals(3, root.getNotes().toJavaList().size());

        a.setId(root.getId());
        queries.update(a, 2, filter, style);

        Atom a4 = atomGraph.getAtomById("004");

        assertEquals("four", a4.getValue());
        List<Atom> children = root.getNotes().toJavaList();
        assertEquals(4, children.size());
        assertEquals("four", children.get(0).getValue());
        assertEquals("one", children.get(1).getValue());
        assertEquals("two", children.get(2).getValue());
    }

    @Test
    public void testFindRootsAndIsolatedAtoms() throws Exception {
        assertEquals(0, queries.findRootAtoms(filter, TreeViews.forwardViewStyle, 1).getChildren().size());
        assertEquals(0, queries.findRootAtoms(filter, TreeViews.backwardViewStyle, 1).getChildren().size());
        assertEquals(0, queries.findIsolatedAtoms(filter).getChildren().size());

        Atom atom0 = atomGraph.createAtomWithProperties(filter, "000");
        atom0.setValue("0");

        assertEquals(1, queries.findRootAtoms(filter, TreeViews.forwardViewStyle, 1).getChildren().size());
        assertEquals(1, queries.findRootAtoms(filter, TreeViews.backwardViewStyle, 1).getChildren().size());
        assertEquals(1, queries.findIsolatedAtoms(filter).getChildren().size());

        Atom atom1 = atomGraph.createAtomWithProperties(filter, "001");
        atom1.setValue("1");

        assertEquals(2, queries.findRootAtoms(filter, TreeViews.forwardViewStyle, 1).getChildren().size());
        assertEquals(2, queries.findRootAtoms(filter, TreeViews.backwardViewStyle, 1).getChildren().size());
        assertEquals(2, queries.findIsolatedAtoms(filter).getChildren().size());

        atom0.addChildAt(atom1, 0);

        assertEquals(1, queries.findRootAtoms(filter, TreeViews.forwardViewStyle, 1).getChildren().size());
        assertEquals(1, queries.findRootAtoms(filter, TreeViews.backwardViewStyle, 1).getChildren().size());
        assertEquals(0, queries.findIsolatedAtoms(filter).getChildren().size());

        Atom atom2 = atomGraph.createAtomWithProperties(filter, "002");
        atom2.setValue("2");

        assertEquals(2, queries.findRootAtoms(filter, TreeViews.forwardViewStyle, 1).getChildren().size());
        assertEquals(2, queries.findRootAtoms(filter, TreeViews.backwardViewStyle, 1).getChildren().size());
        assertEquals(1, queries.findIsolatedAtoms(filter).getChildren().size());

        atom0.addChildAt(atom2, 0);

        assertEquals(1, queries.findRootAtoms(filter, TreeViews.forwardViewStyle, 1).getChildren().size());
        assertEquals(2, queries.findRootAtoms(filter, TreeViews.backwardViewStyle, 1).getChildren().size());
        assertEquals(0, queries.findIsolatedAtoms(filter).getChildren().size());
    }

    private void assertNotesEqual(final Atom a,
                                  final String... expected) {
        String[] actual = new String[countNotes(a)];

        int i = 0;
        AtomList cur = a.getNotes();
        while (null != cur) {
            actual[i++] = cur.getFirst().getValue();
            cur = cur.getRest();
        }

        assertArrayEquals(expected, actual);
    }

    private int countNotes(final Atom a) {
        AtomList cur = a.getNotes();
        int count = 0;
        while (cur != null) {
            count++;
            cur = cur.getRest();
        }

        return count;
    }

    private Atom createAtom(String id) {
        return atomGraph.createAtomWithProperties(filter, id);
    }
}