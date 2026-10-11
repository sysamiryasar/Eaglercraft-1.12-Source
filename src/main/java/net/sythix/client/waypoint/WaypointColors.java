package net.sythix.client.waypoint;

final class WaypointColors
{
    private static final int[] PALETTE = new int[] {0xD81820, 0x2F6BFF, 0x55FF55, 0xFFD24A, 0xB44CFF, 0x6CC6FF, 0xFF8A3D, 0xFFFFFF};

    private WaypointColors()
    {
    }

    static int next(int index)
    {
        return PALETTE[index % PALETTE.length];
    }
}
