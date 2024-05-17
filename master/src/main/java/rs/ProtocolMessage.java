package rs;

public enum ProtocolMessage {
    INIT(0),
    START_MAP(1), 
    MAP_DONE(2), 
    START_SHUFFLE1(3), 
    SHUFFLE1_DONE(4),
    START_REDUCE1(5),
    REDUCE1_DONE(6),
    START_SHUFFLE2(7),
    SHUFFLE2_DONE(8),
    START_REDUCE2(9),
    REDUCE2_DONE(10),
    TEST(11),
    QUIT(12);

    private final int code;

    private ProtocolMessage(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}