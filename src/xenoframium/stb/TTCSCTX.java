package xenoframium.stb;

class TTCSCTX {
    int bounds;
    int started;
    float firstX, firstY;
    float x, y;
    int minX, maxX, minY, maxY;

    TTVertex[] pVertices;
    int numVertices;

    public TTCSCTX(int bounds) {
        this(bounds, 0, 0,0, 0,0, 0,0,0,0, null, 0);
        this.bounds = bounds;
    }

    public TTCSCTX(int bounds, int started, float firstX, float firstY, float x, float y, int minX, int maxX, int minY, int maxY, TTVertex[] pVertices, int numVertices){
        this.bounds = bounds;
        this.started = started;
        this.firstX = firstX;
        this.firstY = firstY;
        this.x = x;
        this.y = y;
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
        this.pVertices = pVertices;
        this.numVertices = numVertices;
    }

    public void stbtt__csctx_rccurve_to(float dx1, float dy1, float dx2, float dy2, float dx3, float dy3)
    {
        float cx1 = x + dx1;
        float cy1 = y + dy1;
        float cx2 = cx1 + dx2;
        float cy2 = cy1 + dy2;
        x = cx2 + dx3;
        y = cy2 + dy3;
        stbtt__csctx_v(STBTT.STBTT_vcubic, (int) x, (int) y, (int)cx1, (int)cy1, (int)cx2, (int)cy2);
    }

    public void stbtt__csctx_rline_to(float dx, float dy)
    {
        x += dx;
        y += dy;
        stbtt__csctx_v(STBTT.STBTT_vline, (int) x, (int) y, 0, 0, 0, 0);
    }

    public void stbtt__track_vertex(int x, int y)
    {
        if (x > maxX || started == 0) maxX = x;
        if (y > maxY || started == 0) maxY = y;
        if (x < minX || started == 0) minX = x;
        if (y < minY || started == 0) minY = y;
        started = 1;
    }

    public void stbtt__csctx_v(int type, int x, int y, int cx, int cy, int cx1, int cy1) {
        if (bounds != 0) {
            stbtt__track_vertex(x, y);
            if (type == STBTT.STBTT_vcubic) {
                stbtt__track_vertex(cx, cy);
                stbtt__track_vertex(cx1, cy1);
            }
        } else {
            STBTT.stbtt_setvertex(pVertices[numVertices], type, x, y, cx, cy);
            pVertices[numVertices].cx1 = cx1;
            pVertices[numVertices].cy1 = cy1;
        }
        numVertices++;
    }

    public void stbtt__csctx_close_shape() {
        if (firstX != x || firstY != y)
            stbtt__csctx_v(STBTT.STBTT_vline, (int) firstX, (int) firstY, 0, 0, 0, 0);
    }

    public void stbtt__csctx_rmove_to(float dx, float dy)
    {
        stbtt__csctx_close_shape();
        firstX = x = x + dx;
        firstY = y = y + dy;
        stbtt__csctx_v(STBTT.STBTT_vmove, (int) x, (int) y, 0, 0, 0, 0);
    }
}
