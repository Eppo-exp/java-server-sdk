package dto;

public class SharedRange {
    public int start;
    public int end;

    @Override
    public String toString() {
        return "[start: " + start + "| end: " + end + "]";
    }
}
