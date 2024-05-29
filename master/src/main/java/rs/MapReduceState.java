package rs;

public enum MapReduceState {
    STARTING(0),
    MAP(1), 
    SHUFFLE1(2), 
    REDUCE1(3), 
    SHUFFLE2(4),
    REDUCE2(5),
    FINISHED(6);

    private final int code;

    private MapReduceState(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}