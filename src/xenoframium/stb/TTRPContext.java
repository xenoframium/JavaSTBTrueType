package xenoframium.stb;

class TTRPContext {
    int width;
    int height;
    int x;
    int y;
    int bottomY;

    public TTRPContext(int pw, int ph) {
        width  = pw;
        height = ph;
        x = 0;
        y = 0;
        bottomY = 0;
    }
}
