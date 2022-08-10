package dto;

public class Variation {
    public String name;
    public SharedRange shardRange;

    @Override
    public String toString() {
        return "[Name: " + name + "| ShareRange: " + shardRange.toString() + "]";
    }
}
