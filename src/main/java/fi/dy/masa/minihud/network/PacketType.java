package fi.dy.masa.minihud.network;

public class PacketType
{
    public record Structures() {
        public static final int PROTOCOL_VERSION = 2;
        public static final int PACKET_S2C_METADATA = 1;
        public static final int PACKET_C2S_REQUEST_METADATA = 2;
        public static final int PACKET_C2S_STRUCTURES_ACCEPT = 3;
        public static final int PACKET_C2S_STRUCTURES_DECLINED = 4;
        public static final int PACKET_S2C_STRUCTURE_DATA = 5;
        public static final int PACKET_C2S_STRUCTURE_TOGGLE = 6;
        public static final int PACKET_S2C_SPAWN_METADATA = 10;
        public static final int PACKET_C2S_REQUEST_SPAWN_METADATA = 11;
    }
    // TODO -- For future expansion from Servux
    /*
    public record Metadata()
    {
        public static final int PROTOCOL_VERSION = 1;
    }
     */
}
