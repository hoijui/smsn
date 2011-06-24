package net.fortytwo.myotherbrain.notes;

/**
 * User: josh
 * Date: 5/18/11
 * Time: 6:13 PM
 */
public abstract class NoteNode {
    private final String value;
    protected String targetKey;
    protected String linkKey;

    public NoteNode(final String value) {
        this.value = value;

        //if (null == text) {
        //    throw new IllegalArgumentException("note text must be non-null" +
        //            " (although it may be an empty string)");
        //}
    }

    public String getValue() {
        return value;
    }

    public String getTargetKey() {
        return targetKey;
    }

    public String getLinkKey() {
        return linkKey;
    }

    public void setTargetKey(final String targetKey) {
        this.targetKey = targetKey;
    }

    public void setLinkKey(final String linkKey) {
        this.linkKey = linkKey;
    }
}