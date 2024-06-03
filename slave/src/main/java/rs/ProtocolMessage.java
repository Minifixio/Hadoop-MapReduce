package rs;

public enum ProtocolMessage {
    INIT(0),
    INIT_RECEIVED(1),

    START_MAP(2), 
    MAP_RECEIVED(3),
    MAP_DONE(4), 

    START_SHUFFLE1(5), 
    SHUFFLE1_RECEIVED(6),
    SHUFFLE1_DONE(7),

    START_REDUCE1(8),
    REDUCE1_RECEIVED(9),
    REDUCE1_DONE(10),

    START_SHUFFLE2(11),
    SHUFFLE2_RECEIVED(12),
    SHUFFLE2_DONE(13),

    START_REDUCE2(14),
    REDUCE2_RECEIVED(15),
    REDUCE2_DONE(16),

    TEST(17),
    QUIT(18);

    private final int code;

    private ProtocolMessage(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

}