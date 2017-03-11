package xenoframium.stb;

@SuppressWarnings("unused")
public class STBTT {
    static final int STBTT_vmove=1,
    STBTT_vline=2,
    STBTT_vcurve=3,
    STBTT_vcubic=4,
    STBTT_MAX_OVERSAMPLE = 8,
    STBTT__OVER_MASK = (STBTT_MAX_OVERSAMPLE-1);
    static class OffsetGenericArray<T>{
        T[] array;
        int o;
        public OffsetGenericArray(T[] arr, int off) {
            array = arr;
            o = off;
        }

        public T get(int i) {
            return array[o + i];
        }

        public void set(int i, T v) {
            array[o + i] = v;
        }

        public T getAndPostIncrement() {
            T va = array[o];
            o++;
            return va;
        }

        public void increment(int i) {
            o += i;
        }
    }

    static class OffsetArray{
        byte[] array;
        int o;
        public OffsetArray(byte[] arr, int off) {
            array = arr;
            o = off;
        }

        public byte get(int i) {
            return array[o + i];
        }

        public void set(int i, byte v) {
            array[o + i] = v;
        }

        public byte getAndPostIncrement() {
            byte va = array[o];
            o++;
            return va;
        }

        public void increment(int i) {
            o += i;
        }
    }

    static class OffsetFloatArray{
        float[] array;
        int o;
        public OffsetFloatArray(float[] arr, int off) {
            array = arr;
            o = off;
        }

        public float get(int i) {
            return array[o + i];
        }

        public void set(int i, float v) {
            array[o + i] = v;
        }

        public float getAndPostIncrement() {
            float va = array[o];
            o++;
            return va;
        }

        public void increment(int i) {
            o += i;
        }
    }

    static class OffsetIntArray{
        int[] array;
        int o;
        public OffsetIntArray(int[] arr, int off) {
            array = arr;
            o = off;
        }

        public int get(int i) {
            return array[o + i];
        }

        public void set(int i, int v) {
            array[o + i] = v;
        }

        public int getAndPostIncrement() {
            int va = array[o];
            o++;
            return va;
        }

        public void increment(int i) {
            o += i;
        }
    }

    public static int stbtt_PackBegin(TTPackContext spc, byte[] pixels, int pw, int ph, int strideInBytes, int padding) {
        TTRPContext context = new TTRPContext(pw-padding, ph-padding);
        int numNodes = pw - padding;
        TTRPNode[] nodes = new TTRPNode[numNodes];

        spc.width = pw;
        spc.height = ph;
        spc.pixels = pixels;
        spc.packInfo = context;
        spc.nodes = nodes;
        spc.padding = padding;
        spc.stride_in_bytes = strideInBytes != 0 ? strideInBytes : pw;
        spc.h_oversample = 1;
        spc.v_oversample = 1;

        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = 0;
        }

        return 1;
    }

    public static float stbtt_ScaleForPixelHeight(TTFontInfo info, float height)
    {
        int fheight = readShort(info.data, info.hhea + 4) - readShort(info.data, info.hhea + 6);
        return height / fheight;
    }

    public static float stbtt_ScaleForMappingEmToPixels(TTFontInfo info, float pixels)
    {
        int unitsPerEm = readUShort(info.data, info.head + 18);
        return pixels / unitsPerEm;
    }

    public static void stbtt_GetCodepointBitmapBoxSubpixel(TTFontInfo font, int codepoint, float scale_x, float scale_y, float shift_x, float shift_y, int[] ix0, int[] iy0, int[] ix1, int[] iy1)
    {
        stbtt_GetGlyphBitmapBoxSubpixel(font, stbtt_FindGlyphIndex(font,codepoint), scale_x, scale_y,shift_x,shift_y, ix0,iy0,ix1,iy1);
    }

    public static void stbtt_GetCodepointBitmapBox(TTFontInfo font, int codepoint, float scale_x, float scale_y, int[] ix0, int[] iy0, int[] ix1, int[] iy1)
    {
        stbtt_GetCodepointBitmapBoxSubpixel(font, codepoint, scale_x, scale_y,0.0f,0.0f, ix0,iy0,ix1,iy1);
    }

    public static int stbtt_FindGlyphIndex(TTFontInfo info, int unicode_codepoint)
    {
        byte[] data = info.data;
        int index_map = info.indexMap;

        int format = readUShort(data, index_map);
        if (format == 0) { // apple byte encoding
            int bytes = readUShort(data, index_map + 2);
            if (unicode_codepoint < bytes-6)
                return Byte.toUnsignedInt(data[index_map + 6 + unicode_codepoint]);
            return 0;
        } else if (format == 6) {
            int first = readUShort(data, index_map + 6);
            int count = readUShort(data,index_map + 8);
            if (unicode_codepoint >= first && unicode_codepoint < first+count)
                return readUShort(data, index_map + 10 + (unicode_codepoint - first)*2);
            return 0;
        } else if (format == 2) {
            return 0;
        } else if (format == 4) { // standard mapping for windows fonts: binary search collection of ranges
            int segcount = readUShort(data, index_map+6) >> 1;
            int searchRange = readUShort(data, index_map+8) >> 1;
            int entrySelector = readUShort(data, index_map+10);
            int rangeShift = readUShort(data, index_map+12) >> 1;

            // do a binary search of the segments
            int endCount = index_map + 14;
            int search = endCount;

            if (unicode_codepoint > 0xffff)
                return 0;

            // they lie from endCount .. endCount + segCount
            // but searchRange is the nearest power of two, so...
            if (unicode_codepoint >= readUShort(data, search + rangeShift*2))
                search += rangeShift*2;

            // now decrement to bias correctly to find smallest
            search -= 2;
            while (entrySelector != 0) {
                int end;
                searchRange >>= 1;
                end = readUShort(data, search + searchRange*2);
                if (unicode_codepoint > end)
                    search += searchRange*2;
                --entrySelector;
            }
            search += 2;

            {
                int offset, start;
                int item = ((search - endCount) >> 1);

                start = readUShort(data, index_map + 14 + segcount*2 + 2 + 2*item);
                if (unicode_codepoint < start)
                    return 0;

                offset = readUShort(data, index_map + 14 + segcount*6 + 2 + 2*item);
                if (offset == 0) {
                    return (unicode_codepoint + readShort(data, index_map + 14 + segcount * 4 + 2 + 2 * item));
                }

                return readUShort(data, offset + (unicode_codepoint-start)*2 + index_map + 14 + segcount*6 + 2 + 2*item);
            }
        } else if (format == 12 || format == 13) {
            int ngroups = (int) readULong(data, index_map+12);
            int low,high;
            low = 0; high = ngroups;
            // Binary search the right group.
            while (low < high) {
                int mid = low + ((high-low) >> 1); // rounds down, so low <= mid < high
                int start_char = (int) readULong(data, index_map+16+mid*12);
                int end_char = (int) readULong(data, index_map+16+mid*12+4);
                if (unicode_codepoint < start_char) {
                    high = mid;
                } else if (unicode_codepoint > end_char) {
                    low = mid + 1;
                } else {
                    int start_glyph = (int) readULong(data, index_map+16+mid*12+8);
                    if (format == 12) {
                        return start_glyph + unicode_codepoint - start_char;
                    } else { // format == 13
                        return start_glyph;
                    }
                }
            }
            return 0; // not found
        }
        // @TODO
        return 0;
    }

    public static void stbtt_setvertex(TTVertex v, int type, int x, int y, int cx, int cy) {
        v.type = type;
        v.x = x;
        v.y = y;
        v.cx = cx;
        v.cy = cy;
    }

    static TTBuf stbtt__get_subrs(TTBuf cff, TTBuf fontdict)
    {
        int subrsoff = 0;
        int[] private_loc = { 0, 0 };
        TTBuf pdict;
        stbtt__dict_get_ints(fontdict, 18, 2, private_loc);
        if (private_loc[1] == 0 || private_loc[0] == 0) return new TTBuf();
        pdict = cff.bufRange(private_loc[1], private_loc[0]);
        int[] temp = {0};
        stbtt__dict_get_ints(pdict, 19, 1, temp);
        subrsoff = temp[0];
        if (subrsoff == 0) return new TTBuf();
        cff.seek(private_loc[1]+subrsoff);
        return stbtt__cff_get_index(cff);
    }

    private static TTBuf stbtt__cid_get_glyph_subrs(TTFontInfo info, int glyph_index)
    {
        TTBuf fdselect = info.fdselect;
        int nranges, start, end, v, fmt, fdselector = -1, i;

        fdselect.seek(0);
        fmt = fdselect.getByte();
        if (fmt == 0) {
            // untested
            fdselect.skip(glyph_index);
            fdselector = fdselect.getByte();
        } else if (fmt == 3) {
            nranges = fdselect.getShort();
            start = fdselect.getShort();
            for (i = 0; i < nranges; i++) {
                v = fdselect.getByte();
                end = fdselect.getShort();
                if (glyph_index >= start && glyph_index < end) {
                    fdselector = v;
                    break;
                }
                start = end;
            }
        }
        if (fdselector == -1) new TTBuf();
        return stbtt__get_subrs(info.cff, stbtt__cff_index_get(info.fontdicts, fdselector));
    }

    private static int stbtt__cff_index_count(TTBuf b)
    {
        b.seek(0);
        return b.getShort();
    }

    private static TTBuf stbtt__get_subr(TTBuf idx, int n)
    {
        int count = stbtt__cff_index_count(idx);
        int bias = 107;
        if (count >= 33900)
            bias = 32768;
        else if (count >= 1240)
            bias = 1131;
        n += bias;
        if (n < 0 || n >= count)
            return new TTBuf();
        return stbtt__cff_index_get(idx, n);
    }

    private static int stbtt__run_charstring(TTFontInfo info, int glyph_index, TTCSCTX c)
    {
        int in_header = 1, maskbits = 0, subr_stack_height = 0, sp = 0, v, i, b0;
        int has_subrs = 0, clear_stack;
        float[] s = new float[48];
        TTBuf[] subr_stack = new TTBuf[10];
        TTBuf subrs = info.subrs, b;
        float f;
        boolean isFirst = false;

        // this currently ignores the initial width value, which isn't needed if we have hmtx
        b = stbtt__cff_index_get(info.charstrings, glyph_index);
        while (b.cursor < b.size) {
            i = 0;
            clear_stack = 1;
            b0 = b.getByte();
            switch (b0) {
                // @TODO implement hinting
                case 0x13: // hintmask
                case 0x14: // cntrmask
                    if (in_header != 0)
                        maskbits += (sp / 2); // implicit "vstem"
                    in_header = 0;
                    b.skip(maskbits + 7 / 8);
                    break;

                case 0x01: // hstem
                case 0x03: // vstem
                case 0x12: // hstemhm
                case 0x17: // vstemhm
                    maskbits += (sp / 2);
                    break;

                case 0x15: // rmoveto
                    in_header = 0;
                    c.stbtt__csctx_rmove_to(s[sp-2], s[sp-1]);
                    break;
                case 0x04: // vmoveto
                    in_header = 0;
                    c.stbtt__csctx_rmove_to(0, s[sp-1]);
                    break;
                case 0x16: // hmoveto
                    in_header = 0;
                    c.stbtt__csctx_rmove_to(s[sp-1], 0);
                    break;

                case 0x05: // rlineto
                    for (; i + 1 < sp; i += 2)
                        c.stbtt__csctx_rline_to(s[i], s[i+1]);
                    break;

                // hlineto/vlineto and vhcurveto/hvcurveto alternate horizontal and vertical
                // starting from a different place.

                case 0x07: // vlineto
                    isFirst = true;
                    for (;;) {
                        if (!isFirst) {
                            if (i >= sp) break;
                            c.stbtt__csctx_rline_to(s[i], 0);
                            i++;
                        }

                        if (i >= sp) break;
                        c.stbtt__csctx_rline_to(0, s[i]);
                        i++;

                        isFirst = false;
                    }
                    break;
                case 0x06: // hlineto
                    for (;;) {
                        if (i >= sp) break;
                        c.stbtt__csctx_rline_to(s[i], 0);
                        i++;
                        if (i >= sp) break;
                        c.stbtt__csctx_rline_to(0, s[i]);
                        i++;
                    }
                    break;

                case 0x1F: // hvcurveto
                    isFirst = true;
                    for (;;) {
                        if (!isFirst) {
                            if (i + 3 >= sp) break;
                            c.stbtt__csctx_rccurve_to(0, s[i], s[i + 1], s[i + 2], s[i + 3], (sp - i == 5) ? s[i + 4] : 0.0f);
                            i += 4;
                        }
                        if (i + 3 >= sp) break;
                        c.stbtt__csctx_rccurve_to(s[i], 0, s[i+1], s[i+2], (sp - i == 5) ? s[i+4] : 0.0f, s[i+3]);
                        i += 4;
                        isFirst = false;
                    }
                    break;
                case 0x1E: // vhcurveto
                    for (;;) {
                        if (i + 3 >= sp) break;
                        c.stbtt__csctx_rccurve_to(0, s[i], s[i+1], s[i+2], s[i+3], (sp - i == 5) ? s[i + 4] : 0.0f);
                        i += 4;
                        hvcurveto:
                        if (i + 3 >= sp) break;
                        c.stbtt__csctx_rccurve_to(s[i], 0, s[i+1], s[i+2], (sp - i == 5) ? s[i+4] : 0.0f, s[i+3]);
                        i += 4;
                    }
                    break;

                case 0x08: // rrcurveto
                    for (; i + 5 < sp; i += 6)
                        c.stbtt__csctx_rccurve_to(s[i], s[i+1], s[i+2], s[i+3], s[i+4], s[i+5]);
                    break;

                case 0x18: // rcurveline
                    for (; i + 5 < sp - 2; i += 6)
                        c.stbtt__csctx_rccurve_to(s[i], s[i+1], s[i+2], s[i+3], s[i+4], s[i+5]);
                    c.stbtt__csctx_rline_to(s[i], s[i+1]);
                    break;

                case 0x19: // rlinecurve
                    if (sp < 8) return 0;
                    for (; i + 1 < sp - 6; i += 2)
                        c.stbtt__csctx_rline_to(s[i], s[i+1]);
                    if (i + 5 >= sp) return 0;
                    c.stbtt__csctx_rccurve_to(s[i], s[i+1], s[i+2], s[i+3], s[i+4], s[i+5]);
                    break;

                case 0x1A: // vvcurveto
                case 0x1B: // hhcurveto
                    if (sp < 4) return 0;
                    f = 0.0f;
                    if ((sp & 1) != 0) { f = s[i]; i++; }
                    for (; i + 3 < sp; i += 4) {
                        if (b0 == 0x1B)
                            c.stbtt__csctx_rccurve_to(s[i], f, s[i+1], s[i+2], s[i+3], 0.0f);
                        else
                            c.stbtt__csctx_rccurve_to(f, s[i], s[i+1], s[i+2], 0.0f, s[i+3]);
                        f = 0.0f;
                    }
                    break;

                case 0x0A: // callsubr
                    if (has_subrs == 0) {
                        if (info.fdselect.size != 0)
                            subrs = stbtt__cid_get_glyph_subrs(info, glyph_index);
                        has_subrs = 1;
                    }
                    // fallthrough
                case 0x1D: // callgsubr
                    if (sp < 1) return 0;
                    v = (int) s[--sp];
                    if (subr_stack_height >= 10) return 0;
                    subr_stack[subr_stack_height++] = b;
                    b = stbtt__get_subr(b0 == 0x0A ? subrs : info.gsubrs, v);
                    if (b.size == 0) return 0;
                    b.cursor = 0;
                    clear_stack = 0;
                    break;

                case 0x0B: // return
                    if (subr_stack_height <= 0) return 0;
                    b = subr_stack[--subr_stack_height];
                    clear_stack = 0;
                    break;

                case 0x0E: // endchar
                    c.stbtt__csctx_close_shape();
                    return 1;

                case 0x0C: { // two-byte escape
                    float dx1, dx2, dx3, dx4, dx5, dx6, dy1, dy2, dy3, dy4, dy5, dy6;
                    float dx, dy;
                    int b1 = b.getByte();
                    switch (b1) {
                        // @TODO These "flex" implementations ignore the flex-depth and resolution,
                        // and always draw beziers.
                        case 0x22: // hflex
                            if (sp < 7) return 0;
                            dx1 = s[0];
                            dx2 = s[1];
                            dy2 = s[2];
                            dx3 = s[3];
                            dx4 = s[4];
                            dx5 = s[5];
                            dx6 = s[6];
                            c.stbtt__csctx_rccurve_to(dx1, 0, dx2, dy2, dx3, 0);
                            c.stbtt__csctx_rccurve_to(dx4, 0, dx5, -dy2, dx6, 0);
                            break;

                        case 0x23: // flex
                            if (sp < 13) return 0;
                            dx1 = s[0];
                            dy1 = s[1];
                            dx2 = s[2];
                            dy2 = s[3];
                            dx3 = s[4];
                            dy3 = s[5];
                            dx4 = s[6];
                            dy4 = s[7];
                            dx5 = s[8];
                            dy5 = s[9];
                            dx6 = s[10];
                            dy6 = s[11];
                            //fd is s[12]
                            c.stbtt__csctx_rccurve_to(dx1, dy1, dx2, dy2, dx3, dy3);
                            c.stbtt__csctx_rccurve_to(dx4, dy4, dx5, dy5, dx6, dy6);
                            break;

                        case 0x24: // hflex1
                            if (sp < 9) return 0;
                            dx1 = s[0];
                            dy1 = s[1];
                            dx2 = s[2];
                            dy2 = s[3];
                            dx3 = s[4];
                            dx4 = s[5];
                            dx5 = s[6];
                            dy5 = s[7];
                            dx6 = s[8];
                            c.stbtt__csctx_rccurve_to(dx1, dy1, dx2, dy2, dx3, 0);
                            c.stbtt__csctx_rccurve_to(dx4, 0, dx5, dy5, dx6, -(dy1+dy2+dy5));
                            break;

                        case 0x25: // flex1
                            if (sp < 11) return 0;
                            dx1 = s[0];
                            dy1 = s[1];
                            dx2 = s[2];
                            dy2 = s[3];
                            dx3 = s[4];
                            dy3 = s[5];
                            dx4 = s[6];
                            dy4 = s[7];
                            dx5 = s[8];
                            dy5 = s[9];
                            dx6 = dy6 = s[10];
                            dx = dx1+dx2+dx3+dx4+dx5;
                            dy = dy1+dy2+dy3+dy4+dy5;
                            if (Math.abs(dx) > Math.abs(dy))
                                dy6 = -dy;
                            else
                                dx6 = -dx;
                            c.stbtt__csctx_rccurve_to(dx1, dy1, dx2, dy2, dx3, dy3);
                            c.stbtt__csctx_rccurve_to(dx4, dy4, dx5, dy5, dx6, dy6);
                            break;

                        default:
                            return 0;
                    }
                } break;

                default:
                    if (b0 != 255 && b0 != 28 && (b0 < 32 || b0 > 254))
                        return 0;

                    // push immediate
                    if (b0 == 255) {
                        f = (float) b.getInt() / 0x10000;
                    } else {
                        b.skip(-1);
                        f = (float)(int) stbtt__cff_int(b);
                    }
                    if (sp >= 48) return 0;
                    s[sp++] = f;
                    clear_stack = 0;
                    break;
            }
            if (clear_stack != 0) sp = 0;
        }
        return 0;
    }

    private static int stbtt__GetGlyphInfoT2(TTFontInfo info, int glyph_index, int[] x0, int[] y0, int[] x1, int[] y1) {
        TTCSCTX c = new TTCSCTX(1);
        int r = stbtt__run_charstring(info, glyph_index, c);
        if (x0 != null) {
            x0[0] = r != 0 ? c.minX: 0;
            y0[0] = r != 0 ? c.minY : 0;
            x1[0] = r != 0 ? c.maxX : 0;
            y1[0] = r != 0 ? c.maxY : 0;
        }
        return r  != 0 ? c.numVertices: 0;
    }

    public static TTAlignedQuad stbtt_GetPackedQuad(TTPackedChar[] chardata, int pw, int ph, int char_index, float[] xpos, float[] ypos, boolean align_to_integer)
    {
        TTAlignedQuad q = new TTAlignedQuad();

        float ipw = 1.0f / pw, iph = 1.0f / ph;
        OffsetGenericArray<TTPackedChar> b = new OffsetGenericArray<>(chardata, char_index);

        if (align_to_integer) {
            float x = (float) Math.floor((xpos[0] + b.get(0).xoff) + 0.5f);
            float y = (float) Math.floor((ypos[0] + b.get(0).yoff) + 0.5f);
            q.x0 = x;
            q.y0 = y;
            q.x1 = x + b.get(0).xoff2 - b.get(0).xoff;
            q.y1 = y + b.get(0).yoff2 - b.get(0).yoff;
        } else {
            q.x0 = xpos[0] + b.get(0).xoff;
            q.y0 = ypos[0] + b.get(0).yoff;
            q.x1 = xpos[0] + b.get(0).xoff2;
            q.y1 = ypos[0] + b.get(0).yoff2;
        }

        q.s0 = b.get(0).x0 * ipw;
        q.t0 = b.get(0).y0 * iph;
        q.s1 = b.get(0).x1 * ipw;
        q.t1 = b.get(0).y1 * iph;

        xpos[0] += b.get(0).xadvance;
        return q;
    }

    static int stbtt__GetGlyfOffset(TTFontInfo info, int glyph_index)
    {
        int g1,g2;

        if (glyph_index >= info.numGlyphs) return -1; // glyph index out of range
        if (info.indexToLocFormat >= 2)    return -1; // unknown index.glyph map format

        if (info.indexToLocFormat == 0) {
            g1 = info.glyf + readUShort(info.data, info.loca + glyph_index * 2) * 2;
            g2 = info.glyf + readUShort(info.data, info.loca + glyph_index * 2 + 2) * 2;
        } else {
            g1 = info.glyf + (int) readULong(info.data, info.loca + glyph_index * 4);
            g2 = info.glyf + (int) readULong(info.data, info.loca + glyph_index * 4 + 4);
        }

        return g1==g2 ? -1 : g1; // if length is 0, return -1
    }

    public static int stbtt_GetGlyphBox(TTFontInfo info, int glyph_index, int[] x0, int[] y0, int[] x1, int[] y1)
    {
        if (info.cff.size != 0) {
            stbtt__GetGlyphInfoT2(info, glyph_index, x0, y0, x1, y1);
        } else {
            int g = stbtt__GetGlyfOffset(info, glyph_index);
            if (g < 0) return 0;

            if (x0 != null) x0[0] = readShort(info.data, g + 2);
            if (y0 != null) y0[0] = readShort(info.data, g + 4);
            if (x1 != null) x1[0] = readShort(info.data, g + 6);
            if (y1 != null) y1[0] = readShort(info.data, g + 8);
        }
        return 1;
    }

    public static void stbtt_GetGlyphBitmapBoxSubpixel(TTFontInfo font, int glyph, float scale_x, float scale_y, float shift_x, float shift_y, int[] ix0, int[] iy0, int[] ix1, int[] iy1)
    {
        int[] x0={0},y0={0},x1={0},y1={0}; // =0 suppresses compiler warning
        if (stbtt_GetGlyphBox(font, glyph, x0, y0, x1, y1) == 0) {
        // e.g. space character
            if (ix0 != null) ix0[0] = 0;
            if (iy0 != null) iy0[0] = 0;
            if (ix1 != null) ix1[0] = 0;
            if (iy1 != null) iy1[0] = 0;
        } else {
            // move to integral bboxes (treating pixels as little squares, what pixels get touched)?
            if (ix0 != null) ix0[0] = (int) Math.floor(x0[0] * scale_x + shift_x);
            if (iy0 != null) iy0[0] = (int) Math.floor(-y1[0] * scale_y + shift_y);
            if (ix1 != null) ix1[0] = (int) Math.ceil(x1[0] * scale_x + shift_x);
            if (iy1 != null) iy1[0] = (int) Math.ceil(-y0[0] * scale_y + shift_y);
        }
    }

    private static int stbtt_PackFontRangesGatherRects(TTPackContext spc, TTFontInfo info, TTPackRange[] ranges, int num_ranges, TTRPRect[] rects)
    {
        int i,j,k;

        k=0;
        for (i=0; i < num_ranges; ++i) {
            float fh = ranges[i].font_size;
            float scale = fh > 0 ? stbtt_ScaleForPixelHeight(info, fh) : stbtt_ScaleForMappingEmToPixels(info, -fh);
            ranges[i].h_oversample = spc.h_oversample;
            ranges[i].v_oversample = spc.v_oversample;
            for (j=0; j < ranges[i].num_chars; ++j) {
                int[] x0 = {0}, y0 = {0}, x1 = {0}, y1 = {0};
                int codepoint = ranges[i].array_of_unicode_codepoints == null ? ranges[i].first_unicode_codepoint_in_range + j : ranges[i].array_of_unicode_codepoints[j];
                int glyph = stbtt_FindGlyphIndex(info, codepoint);
                stbtt_GetGlyphBitmapBoxSubpixel(info,glyph,
                        scale * spc.h_oversample,
                        scale * spc.v_oversample,
                        0,0,
                        x0,y0,x1,y1);
                rects[k].w =  (x1[0]-x0[0] + spc.padding + spc.h_oversample -1);
                rects[k].h =  (y1[0]-y0[0] + spc.padding + spc.v_oversample -1);
                ++k;
            }
        }

        return k;
    }

    public static int stbtt_PackFontRange(TTPackContext spc, byte[] fontdata, int font_index, float font_size,
                                          int first_unicode_codepoint_in_range, int num_chars_in_range, TTPackedChar[] chardata_for_range)
    {
        TTPackRange range = new TTPackRange();
        range.first_unicode_codepoint_in_range = first_unicode_codepoint_in_range;
        range.array_of_unicode_codepoints = null;
        range.num_chars                   = num_chars_in_range;
        range.chardata_for_range          = chardata_for_range;
        range.font_size                   = font_size;
        TTPackRange[] tr = new TTPackRange[1];
        tr[0] = range;
        return stbtt_PackFontRanges(spc, fontdata, font_index, tr, 1);
    }

    public static int stbtt_PackFontRanges(TTPackContext spc, byte[] fontdata, int font_index, TTPackRange[] ranges, int num_ranges)
    {
        TTFontInfo info = new TTFontInfo(fontdata, font_index);
        int i,j,n, return_value = 1;
        //stbrp_context *context = (stbrp_context *) spc.pack_info;
        TTRPRect[] rects;

        // flag all characters as NOT packed
        for (i=0; i < num_ranges; ++i)
            for (j=0; j < ranges[i].num_chars; ++j)
                ranges[i].chardata_for_range[j].x0 =
                        ranges[i].chardata_for_range[j].y0 =
                                ranges[i].chardata_for_range[j].x1 =
                                        ranges[i].chardata_for_range[j].y1 = 0;

        n = 0;
        for (i=0; i < num_ranges; ++i)
            n += ranges[i].num_chars;

        rects = new TTRPRect[n];

        for (int l = 0; l < n; l++) {
            rects[l] = new TTRPRect();
        }

        n = stbtt_PackFontRangesGatherRects(spc, info, ranges, num_ranges, rects);

        stbtt_PackFontRangesPackRects(spc, rects, n);

        return_value = stbtt_PackFontRangesRenderIntoRects(spc, info, ranges, num_ranges, rects);

        return return_value;
    }

    private static float stbtt__oversample_shift(int oversample)
    {
        if (oversample == 0)
            return 0.0f;

        // The prefilter is a box filter of width "oversample",
        // which shifts phase by (oversample - 1)/2 pixels in
        // oversampled space. We want to shift in the opposite
        // direction to counter this.
        return (float)-(oversample - 1) / (2.0f * (float)oversample);
    }

    public static void stbtt_GetGlyphHMetrics(TTFontInfo info, int glyph_index, int[] advanceWidth, int[] leftSideBearing)
    {
        int numOfLongHorMetrics = readUShort(info.data, info.hhea + 34);
        if (glyph_index < numOfLongHorMetrics) {
            if (advanceWidth != null)     advanceWidth[0]    = readShort(info.data, info.hmtx + 4*glyph_index);
            if (leftSideBearing != null)  leftSideBearing[0] = readShort(info.data, info.hmtx + 4*glyph_index + 2);
        } else {
            if (advanceWidth != null)     advanceWidth[0]    = readShort(info.data, info.hmtx + 4*(numOfLongHorMetrics-1));
            if (leftSideBearing != null)  leftSideBearing[0] = readShort(info.data, info.hmtx + 4*numOfLongHorMetrics + 2*(glyph_index - numOfLongHorMetrics));
        }
    }

    public static void stbtt_GetGlyphBitmapBox(TTFontInfo font, int glyph, float scale_x, float scale_y, int[] ix0, int[] iy0, int[] ix1, int[] iy1)
    {
        stbtt_GetGlyphBitmapBoxSubpixel(font, glyph, scale_x, scale_y,0.0f,0.0f, ix0, iy0, ix1, iy1);
    }

    static int stbtt__close_shape(TTVertex[] vertices, int num_vertices, int was_off, int start_off,
                                  int sx, int sy, int scx, int scy, int cx, int cy)
    {
        if (start_off != 0) {
            if (was_off != 0)
                stbtt_setvertex(vertices[num_vertices++], STBTT_vcurve, (cx+scx)>>1, (cy+scy)>>1, cx,cy);
            stbtt_setvertex(vertices[num_vertices++], STBTT_vcurve, sx,sy,scx,scy);
        } else {
            if (was_off != 0) {
                stbtt_setvertex(vertices[num_vertices++], STBTT_vcurve, sx, sy, cx, cy);
            } else
                stbtt_setvertex(vertices[num_vertices++], STBTT_vline,sx,sy,0,0);
        }
        return num_vertices;
    }

    static int stbtt__GetGlyphShapeTT(TTFontInfo info, int glyph_index, TTVertex[][] pvertices)
    {
        int numberOfContours;

        OffsetArray endPtsOfContours;
        byte[] data = info.data;
        TTVertex[] vertices=null;
        int num_vertices=0;
        int g = stbtt__GetGlyfOffset(info, glyph_index);

        pvertices[0][0] = null;

        if (g < 0) return 0;

        numberOfContours = readShort(data, g);

        if (numberOfContours > 0) {
            int flags=0,flagcount;
            int ins, i,j=0,m,n, next_move, was_off=0, off, start_off=0;
            int x,y,cx,cy,sx,sy, scx,scy;
            endPtsOfContours = new OffsetArray(data, g + 10);
            ins = readUShort(data, g + 10 + numberOfContours * 2);
            OffsetArray points = new OffsetArray(data, g + 10 + numberOfContours * 2 + 2 + ins);

            n = 1+readUShort(data, endPtsOfContours.o + numberOfContours*2-2);

            m = n + 2*numberOfContours;  // a loose bound on how many vertices we might need
            vertices = new TTVertex[m];

            for (int l = 0; l < m; l++) {
                vertices[l] = new TTVertex();
            }

            next_move = 0;
            flagcount=0;

            // in first pass, we load uninterpreted data into the allocated array
            // above, shifted to the end of the array so we won't overwrite it when
            // we create our final data starting from the front

            off = m - n; // starting offset for uninterpreted data, regardless of how m ends up being calculated

            // first load flags

            for (i=0; i < n; ++i) {
                if (flagcount == 0) {
                    flags = Byte.toUnsignedInt(points.getAndPostIncrement());
                    if ((flags & 8) != 0)
                        flagcount = Byte.toUnsignedInt(points.getAndPostIncrement());
                } else
                    --flagcount;
                vertices[off+i].type = flags;
            }

            // now load x coordinates
            x=0;
            for (i=0; i < n; ++i) {
                flags = vertices[off+i].type;
                if ((flags & 2) != 0) {
                    short dx = (short) Byte.toUnsignedInt(points.getAndPostIncrement());
                    x += (flags & 16) != 0 ? dx : -dx; // ???
                } else {
                    if ((flags & 16) == 0) {
                        x = x + (short) ((Byte.toUnsignedInt(points.get(0))) * 256 + Byte.toUnsignedInt(points.get(1)));
                        points.increment(2);
                    }
                }
                vertices[off+i].x = x;
            }

            // now load y coordinates
            y=0;
            for (i=0; i < n; ++i) {
                flags = vertices[off+i].type;
                if ((flags & 4) != 0) {
                    short dy = (short) Byte.toUnsignedInt(points.getAndPostIncrement());
                    y += (flags & 32) != 0? dy : -dy; // ???
                } else {
                    if ((flags & 32) == 0) {
                        y = y + (short) ((Byte.toUnsignedInt(points.get(0))*256) + Byte.toUnsignedInt(points.get(1)));
                        points.increment(2);
                    }
                }
                vertices[off+i].y = y;
            }

            // now convert them to our format
            num_vertices=0;
            sx = sy = cx = cy = scx = scy = 0;
            for (i=0; i < n; ++i) {
                flags = vertices[off+i].type;
                x     = vertices[off+i].x;
                y     = vertices[off+i].y;

                if (next_move == i) {
                    if (i != 0)
                        num_vertices = stbtt__close_shape(vertices, num_vertices, was_off, start_off, sx,sy,scx,scy,cx,cy);

                    // now start the new one
                    if ((flags & 1) == 0) {
                        start_off = 1;
                    } else {
                        start_off = 0;
                    }

                    if (start_off != 0) {
                        // if we start off with an off-curve point, then when we need to find a point on the curve
                        // where we can start, and we need to save some state for when we wraparound.
                        scx = x;
                        scy = y;
                        if ((vertices[off+i+1].type & 1) == 0) {
                            // next point is also a curve point, so interpolate an on-point curve
                            sx = (x + vertices[off+i+1].x) >> 1;
                            sy = (y + vertices[off+i+1].y) >> 1;
                        } else {
                            // otherwise just use the next point as our start point
                            sx = vertices[off+i+1].x;
                            sy = vertices[off+i+1].y;
                            ++i; // we're using point i+1 as the starting point, so skip it
                        }
                    } else {
                        sx = x;
                        sy = y;
                    }
                    stbtt_setvertex(vertices[num_vertices++], STBTT_vmove,sx,sy,0,0);
                    was_off = 0;
                    next_move = 1 + readUShort(data, endPtsOfContours.o+j*2);
                    ++j;
                } else {
                    if ((flags & 1) == 0) { // if it's a curve
                        if (was_off != 0) // two off-curve control points in a row means interpolate an on-curve midpoint
                            stbtt_setvertex(vertices[num_vertices++], STBTT_vcurve, (cx+x)>>1, (cy+y)>>1, cx, cy);
                        cx = x;
                        cy = y;
                        was_off = 1;
                    } else {
                        if (was_off != 0) {
                            stbtt_setvertex(vertices[num_vertices++], STBTT_vcurve, x, y, cx, cy);
                        } else
                            stbtt_setvertex(vertices[num_vertices++], STBTT_vline, x,y,0,0);
                        was_off = 0;
                    }
                }
            }
            num_vertices = stbtt__close_shape(vertices, num_vertices, was_off, start_off, sx,sy,scx,scy,cx,cy);
        } else if (numberOfContours == -1) {
            // Compound shapes.
            int more = 1;
            OffsetArray comp = new OffsetArray(data, g + 10);
            num_vertices = 0;
            vertices = null;
            while (more != 0) {
                int flags, gidx;
                int comp_num_verts = 0, i;
                TTVertex[] comp_verts = new TTVertex[1], tmp = null;
                float[] mtx = {1,0,0,1,0,0};
                float m, n;

                flags = readShort(comp.array, comp.o); comp.increment(2);
                gidx = readShort(comp.array, comp.o); comp.increment(2);

                if ((flags & 2) != 0) { // XY values
                    if ((flags & 1) != 0) { // shorts
                        mtx[4] = readShort(comp.array, comp.o); comp.increment(2);
                        mtx[5] = readShort(comp.array, comp.o); comp.increment(2);
                    } else {
                        mtx[4] = comp.get(0); comp.increment(1);
                        mtx[5] = comp.get(0); comp.increment(1);
                    }
                }
                else {
                    return 0;
                }
                if ((flags & (1<<3)) != 0) { // WE_HAVE_A_SCALE
                    mtx[0] = mtx[3] = readShort(comp.array, comp.o)/16384.0f; comp.increment(2);
                    mtx[1] = mtx[2] = 0;
                } else if ((flags & (1<<6)) != 0) { // WE_HAVE_AN_X_AND_YSCALE
                    mtx[0] = readShort(comp.array, comp.o)/16384.0f; comp.increment(2);
                    mtx[1] = mtx[2] = 0;
                    mtx[3] = readShort(comp.array, comp.o)/16384.0f; comp.increment(2);
                } else if ((flags & (1<<7)) != 0) { // WE_HAVE_A_TWO_BY_TWO
                    mtx[0] = readShort(comp.array, comp.o)/16384.0f; comp.increment(2);
                    mtx[1] = readShort(comp.array, comp.o)/16384.0f; comp.increment(2);
                    mtx[2] = readShort(comp.array, comp.o)/16384.0f; comp.increment(2);
                    mtx[3] = readShort(comp.array, comp.o)/16384.0f; comp.increment(2);
                }

                // Find transformation scales.
                m = (float) Math.sqrt(mtx[0]*mtx[0] + mtx[1]*mtx[1]);
                n = (float) Math.sqrt(mtx[2]*mtx[2] + mtx[3]*mtx[3]);

                // Get indexed glyph.
                TTVertex[][] tcomp_verts = new TTVertex[1][];
                tcomp_verts[0] = comp_verts;
                comp_num_verts = stbtt_GetGlyphShape(info, gidx, tcomp_verts);
                comp_verts = tcomp_verts[0];
                if (comp_num_verts > 0) {
                    // Transform vertices.
                    for (i = 0; i < comp_num_verts; ++i) {
                        TTVertex v = comp_verts[i];
                        int x,y;
                        x=v.x; y=v.y;
                        v.x = (int) (m * (mtx[0]*x + mtx[2]*y + mtx[4]));
                        v.y = (int) (n * (mtx[1]*x + mtx[3]*y + mtx[5]));
                        x=v.cx; y=v.cy;
                        v.cx = (int) (m * (mtx[0]*x + mtx[2]*y + mtx[4]));
                        v.cy = (int ) (n * (mtx[1]*x + mtx[3]*y + mtx[5]));
                    }
                    // Append vertices.
                    tmp = new TTVertex[num_vertices + comp_num_verts];
                    if (num_vertices > 0) {
                        for (int z = 0; z < num_vertices; z++) {
                            tmp[z] = vertices[z];
                        }
                    }
                    for (int z = 0; z < comp_num_verts; z++) {
                        tmp[num_vertices + z] = comp_verts[z];
                    }
                    vertices = tmp;
                    num_vertices += comp_num_verts;
                }
                // More components ?
                more = flags & (1<<5);
            }
        } else if (numberOfContours < 0) {
            // @TODO other compound variations?
            return 0;
        } else {
            // numberOfCounters == 0, do nothing
        }

        pvertices[0] = vertices;
        return num_vertices;
    }

    static int stbtt__GetGlyphShapeT2(TTFontInfo info, int glyph_index, TTVertex[][] pvertices)
    {
        // runs the charstring twice, once to count and once to output (to avoid realloc)
        TTCSCTX count_ctx = new TTCSCTX(1);
        TTCSCTX output_ctx = new TTCSCTX(0);
        if (stbtt__run_charstring(info, glyph_index, count_ctx) != 0) {
            pvertices[0] = new TTVertex[count_ctx.numVertices];
            output_ctx.pVertices = pvertices[0];
            if (stbtt__run_charstring(info, glyph_index, output_ctx) != 0) {
                if (output_ctx.numVertices == count_ctx.numVertices);
                return output_ctx.numVertices;
            }
        }
        pvertices[0] = null;
        return 0;
    }

    public static int stbtt_GetGlyphShape(TTFontInfo info, int glyph_index, TTVertex[][] pvertices)
    {
        if (info.cff.size == 0)
            return stbtt__GetGlyphShapeTT(info, glyph_index, pvertices);
        else
            return stbtt__GetGlyphShapeT2(info, glyph_index, pvertices);
    }

    static void stbtt__rasterize(TTBitmap result, TTPoint[] pts, int[] wcount, int windings, float scale_x, float scale_y, float shift_x, float shift_y, int off_x, int off_y, boolean invert)
    {
        float y_scale_inv = invert ? -scale_y : scale_y;
        TTEdge[] e;
        int n,i,j,k,m;
        int vsubsample = 1;
            // vsubsample should divide 255 evenly; otherwise we won't reach full opacity

            // now we have to blow out the windings into explicit edge lists
            n = 0;
        for (i=0; i < windings; ++i)
            n += wcount[i];

        e = new TTEdge[n+1];
        for (int l = 0; l < n+1; l++) {
            e[l] = new TTEdge();
        }
        n = 0;

        m=0;
        for (i=0; i < windings; ++i) {
            TTPoint[] p = pts;
            int l = m;
            m += wcount[i];
            j = wcount[i]-1;
            for (k=0; k < wcount[i]; j=k++) {
                int a=k,b=j;
                // skip the edge if horizontal
                if (p[j+l].y == p[k+l].y)
                    continue;
                // add edge from j to k to the list
                e[n].invert = 0;
                if (invert ? p[j+l].y > p[k+l].y : p[j+l].y < p[k+l].y) {
                    e[n].invert = 1;
                    a=j;
                    b=k;
                }
                e[n].x0 = p[a+l].x * scale_x + shift_x;
                e[n].y0 = (p[a+l].y * y_scale_inv + shift_y) * vsubsample;
                e[n].x1 = p[b+l].x * scale_x + shift_x;
                e[n].y1 = (p[b+l].y * y_scale_inv + shift_y) * vsubsample;
                ++n;
            }
        }

        // now sort the edges by their highest point (should snap to integer, and then by x)
        stbtt__sort_edges(e, n);
        // now, traverse the scanlines and find the intersections on each scanline, use xor winding rule
        stbtt__rasterize_sorted_edges(result, e, n, vsubsample, off_x, off_y);
    }

    static TTActiveEdge stbtt__new_active(TTEdge e, int off_x, float start_point)
    {
        TTActiveEdge z = new TTActiveEdge();
        float dxdy = (e.x1 - e.x0) / (e.y1 - e.y0);
        //STBTT_assert(e.y0 <= start_point);
        z.fdx = dxdy;
        z.fdy = dxdy != 0.0f ? (1.0f/dxdy) : 0.0f;
        z.fx = e.x0 + dxdy * (start_point - e.y0);
        z.fx -= off_x;
        z.direction = e.invert != 0? 1.0f : -1.0f;
        z.sy = e.y0;
        z.ey = e.y1;
        z.next = null;
        return z;
    }

// the edge passed in here does not cross the vertical line at x or the vertical line at x+1
// (i.e. it has already been clipped to those)

    static void stbtt__handle_clipped_edge(OffsetFloatArray scnline, int x, TTActiveEdge e, float x0, float y0, float x1, float y1)
    {
        OffsetFloatArray scanline = new OffsetFloatArray(scnline.array, scnline.o);
        if (y0 == y1) return;
        if (y0 > e.ey) return;
        if (y1 < e.sy) return;
        if (y0 < e.sy) {
            x0 += (x1-x0) * (e.sy - y0) / (y1-y0);
            y0 = e.sy;
        }
        if (y1 > e.ey) {
            x1 += (x1-x0) * (e.ey - y1) / (y1-y0);
            y1 = e.ey;
        }

        if (x0 <= x && x1 <= x)
            scanline.set(x, scanline.get(x) + e.direction * (y1-y0));
        else if (x0 >= x+1 && x1 >= x+1)
            ;
        else {
            scanline.set(x, scanline.get(x) + e.direction * (y1-y0) * (1-((x0-x)+(x1-x))/2)); // coverage = 1 - average x position
        }
    }

    static void stbtt__fill_active_edges_new(float[] scanline, OffsetFloatArray scanline_fill, int len, TTActiveEdge e, float y_top)
    {
        float y_bottom = y_top+1;

        while (e != null) {
            // brute force every pixel

            // compute intersection points with top & bottom

            if (e.fdx == 0) {
                float x0 = e.fx;
                if (x0 < len) {
                    if (x0 >= 0) {
                        stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),(int) x0, e, x0,y_top, x0,y_bottom);
                        stbtt__handle_clipped_edge(new OffsetFloatArray(scanline_fill.array, scanline_fill.o-1),(int) x0+1,e, x0,y_top, x0,y_bottom);
                    } else {
                        stbtt__handle_clipped_edge(new OffsetFloatArray(scanline_fill.array, scanline_fill.o-1),0,e, x0,y_top, x0,y_bottom);
                    }
                }
            } else {
                float x0 = e.fx;
                float dx = e.fdx;
                float xb = x0 + dx;
                float x_top, x_bottom;
                float sy0,sy1;
                float dy = e.fdy;

                // compute endpoints of line segment clipped to this scanline (if the
                // line segment starts on this scanline. x0 is the intersection of the
                // line with y_top, but that may be off the line segment.
                if (e.sy > y_top) {
                    x_top = x0 + dx * (e.sy - y_top);
                    sy0 = e.sy;
                } else {
                    x_top = x0;
                    sy0 = y_top;
                }
                if (e.ey < y_bottom) {
                    x_bottom = x0 + dx * (e.ey - y_top);
                    sy1 = e.ey;
                } else {
                    x_bottom = xb;
                    sy1 = y_bottom;
                }

                if (x_top >= 0 && x_bottom >= 0 && x_top < len && x_bottom < len) {
                    // from here on, we don't have to range check x values

                    if ((int) x_top == (int) x_bottom) {
                        float height;
                        // simple case, only spans one pixel
                        int x = (int) x_top;
                        height = sy1 - sy0;
                        scanline[x] += e.direction * (1-((x_top - x) + (x_bottom-x))/2)  * height;
                        scanline_fill.set(x, scanline_fill.get(x) + e.direction * height); // everything right of this pixel is filled
                    } else {
                        int x,x1,x2;
                        float y_crossing, step, sign, area;
                        // covers 2+ pixels
                        if (x_top > x_bottom) {
                            // flip scanline vertically; signed area is the same
                            float t;
                            sy0 = y_bottom - (sy0 - y_top);
                            sy1 = y_bottom - (sy1 - y_top);
                            t = sy0;
                            sy0 = sy1;
                            sy1 = t;
                            t = x_bottom;
                            x_bottom = x_top;
                            x_top = t;
                            dx = -dx;
                            dy = -dy;
                            t = x0;
                            x0 = xb;
                            xb = t;
                        }

                        x1 = (int) x_top;
                        x2 = (int) x_bottom;
                        // compute intersection with y axis at x1+1
                        y_crossing = (x1+1 - x0) * dy + y_top;

                        sign = e.direction;
                        // area of the rectangle covered from y0..y_crossing
                        area = sign * (y_crossing-sy0);
                        // area of the triangle (x_top,y0), (x+1,y0), (x+1,y_crossing)
                        scanline[x1] += area * (1-((x_top - x1)+(x1+1-x1))/2);

                        step = sign * dy;
                        for (x = x1+1; x < x2; ++x) {
                            scanline[x] += area + step/2;
                            area += step;
                        }
                        y_crossing += dy * (x2 - (x1+1));

                        scanline[x2] += area + sign * (1-((x2-x2)+(x_bottom-x2))/2) * (sy1-y_crossing);

                        scanline_fill.set(x2, scanline_fill.get(x2) + sign * (sy1-sy0));
                    }
                } else {
                    // if edge goes outside of box we're drawing, we require
                    // clipping logic. since this does not match the intended use
                    // of this library, we use a different, very slow brute
                    // force implementation
                    int x;
                    for (x=0; x < len; ++x) {
                        // cases:
                        //
                        // there can be up to two intersections with the pixel. any intersection
                        // with left or right edges can be handled by splitting into two (or three)
                        // regions. intersections with top & bottom do not necessitate case-wise logic.
                        //
                        // the old way of doing this found the intersections with the left & right edges,
                        // then used some simple logic to produce up to three segments in sorted order
                        // from top-to-bottom. however, this had a problem: if an x edge was epsilon
                        // across the x border, then the corresponding y position might not be distinct
                        // from the other y segment, and it might ignored as an empty segment. to avoid
                        // that, we need to explicitly produce segments based on x positions.

                        // rename variables to clear pairs
                        float y0 = y_top;
                        float x1 = (float) (x);
                        float x2 = (float) (x+1);
                        float x3 = xb;
                        float y3 = y_bottom;
                        float y1,y2;

                        // x = e.x + e.dx * (y-y_top)
                        // (y-y_top) = (x - e.x) / e.dx
                        // y = (x - e.x) / e.dx + y_top
                        y1 = (x - x0) / dx + y_top;
                        y2 = (x+1 - x0) / dx + y_top;

                        if (x0 < x1 && x3 > x2) {         // three segments descending down-right
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x0,y0, x1,y1);
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x1,y1, x2,y2);
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x2,y2, x3,y3);
                        } else if (x3 < x1 && x0 > x2) {  // three segments descending down-left
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x0,y0, x2,y2);
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x2,y2, x1,y1);
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x1,y1, x3,y3);
                        } else if (x0 < x1 && x3 > x1) {  // two segments across x, down-right
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x0,y0, x1,y1);
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x1,y1, x3,y3);
                        } else if (x3 < x1 && x0 > x1) {  // two segments across x, down-left
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x0,y0, x1,y1);
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x1,y1, x3,y3);
                        } else if (x0 < x2 && x3 > x2) {  // two segments across x+1, down-right
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x0,y0, x2,y2);
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x2,y2, x3,y3);
                        } else if (x3 < x2 && x0 > x2) {  // two segments across x+1, down-left
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0),x,e, x0,y0, x2,y2);
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0), x,e, x2,y2, x3,y3);
                        } else {  // one segment
                            stbtt__handle_clipped_edge(new OffsetFloatArray(scanline, 0), x,e, x0,y0, x3,y3);
                        }
                    }
                }
            }
            e = e.next;
        }
    }

    static void stbtt__rasterize_sorted_edges(TTBitmap result, TTEdge[] e, int n, int vsubsample, int off_x, int off_y)
    {

        TTActiveEdge head = null;
        int y,j=0, i;
        float[] scanline_data = new float[129];
        float[] scanline;
        OffsetFloatArray scanline2;

        if (result.w > 64) {
            scanline = new float[result.w * 2 + 1];
        }
        else
            scanline = scanline_data;

        scanline2 = new OffsetFloatArray(scanline, result.w);

        y = off_y;
        e[n].y0 = (float) (off_y + result.h) + 1;

        int l = 0;
        while (j < result.h) {
            for (int o = 0; o < result.w; o++) {
                scanline[o] = 0;
            }
            for (int o = 0; o < result.w+1; o++) {
                scanline2.set(o, 0);
            }

            // find center of pixel for this scanline
            float scan_y_top    = y + 0.0f;
            float scan_y_bottom = y + 1.0f;

            TTActiveEdge curr = head;
            TTActiveEdge prev = null;

            while (curr != null) {
                if (curr.ey <= scan_y_top) {
                    if (curr == head) {
                        head = curr.next;
                    } else {
                        prev.next = curr.next;
                    }
                } else {
                    prev = curr;
                }
                curr = curr.next;
            }

            for (; e[l].y0 <= scan_y_bottom; l++) {
                if (e[l].y0 != e[l].y1) {
                    TTActiveEdge newNode = stbtt__new_active(e[l], off_x, scan_y_top);
                    newNode.next = head;
                    head = newNode;
                }
            }
            // now process all active edges
            if (head != null)
                stbtt__fill_active_edges_new(scanline, new OffsetFloatArray(scanline2.array, scanline2.o+1), result.w, head, scan_y_top);

            {
                float sum = 0;
                for (i=0; i < result.w; ++i) {
                    float k;
                    int m;
                    sum += scanline2.get(i);
                    k = scanline[i] + sum;
                    k = Math.abs(k)*255 + 0.5f;
                    m = (int) k;
                    if (m > 255) m = 255;
                    result.pixels.set(j*result.stride + i, (byte) m);
                }
            }
            // advance all the edges
            curr = head;
            while (curr != null) {
                curr.fx += curr.fdx;
                curr = curr.next;
            }

            ++y;
            ++j;
        }
    }

    private static void stbtt__sort_edges(TTEdge[] p, int n)
    {
        stbtt__sort_edges_quicksort(new OffsetGenericArray<TTEdge>(p, 0), n);
        stbtt__sort_edges_ins_sort(p, n);
    }


    private static boolean STBTT__COMPARE(TTEdge a, TTEdge b) {
        return a.y0 < b.y0;
    }

    static void stbtt__sort_edges_ins_sort(TTEdge[] p, int n)
    {
        int i,j;
        for (i=1; i < n; ++i) {
            TTEdge t = p[i];
            j = i;
            while (j > 0) {
                TTEdge b = p[j-1];
                boolean c = STBTT__COMPARE(t,b);
                if (!c) break;
                p[j] = p[j-1];
                --j;
            }
            if (i != j)
                p[j] = t;
        }
    }

    private static void stbtt__sort_edges_quicksort(OffsetGenericArray<TTEdge> p, int n)
    {
   /* threshhold for transitioning to insertion sort */
        while (n > 12) {
            TTEdge t;
            int m,i,j;
            boolean c01, c12, c;
      /* compute median of three */
            m = n >> 1;
            c01 = STBTT__COMPARE(p.get(0),p.get(m));
            c12 = STBTT__COMPARE(p.get(m),p.get(n-1));
      /* if 0 >= mid >= end, or 0 < mid < end, then use mid */
            if (c01 != c12) {
         /* otherwise, we'll need to swap something else to middle */
                int z;
                c = STBTT__COMPARE(p.get(0),p.get(n-1));
         /* 0>mid && mid<n:  0>n => n; 0<n => 0 */
         /* 0<mid && mid>n:  0>n => 0; 0<n => n */
                z = (c == c12) ? 0 : n-1;
                t = p.get(z);
                p.set(z, p.get(m));
                p.set(m, t);
            }
      /* now p[m] is the median-of-three */
      /* swap it to the beginning so it won't move around */
            t = p.get(0);
            p.set(0, p.get(m));
            p.set(m, t);

      /* partition loop */
            i=1;
            j=n-1;
            for(;;) {
         /* handling of equality is crucial here */
         /* for sentinels & efficiency with duplicates */
                for (;;++i) {
                    if (!STBTT__COMPARE(p.get(i), p.get(0))) break;
                }
                for (;;--j) {
                    if (!STBTT__COMPARE(p.get(0), p.get(j))) break;
                }
         /* make sure we haven't crossed */
                if (i >= j) break;
                t = p.get(i);
                p.set(i, p.get(j));
                p.set(j, t);

                ++i;
                --j;
            }
      /* recurse on smaller side, iterate on larger */
            if (j < (n-i)) {
                stbtt__sort_edges_quicksort(new OffsetGenericArray<>(p.array, p.o),j);
                p.increment(i);
                n = n-i;
            } else {
                stbtt__sort_edges_quicksort(new OffsetGenericArray<>(p.array, p.o + i), n-i);
                n = j;
            }
        }
    }

    public static void stbtt_MakeGlyphBitmapSubpixel(TTFontInfo info, OffsetArray output, int out_w, int out_h, int out_stride, float scale_x, float scale_y, float shift_x, float shift_y, int glyph)
    {
        int[] ix0 = {0},iy0 = {0}, ix1 = {0}, iy1 = {0};
        TTVertex[] vertices;
        TTVertex[][] tpVertices = new TTVertex[1][];
        tpVertices[0] = new TTVertex[1];
        int num_verts = stbtt_GetGlyphShape(info, glyph, tpVertices);
        vertices = tpVertices[0];
        TTBitmap gbm = new TTBitmap();

        stbtt_GetGlyphBitmapBoxSubpixel(info, glyph, scale_x, scale_y, shift_x, shift_y, ix0, iy0, ix1, iy1);
        gbm.pixels = new OffsetArray(output.array, output.o);
        gbm.w = out_w;
        gbm.h = out_h;
        gbm.stride = out_stride;

        if (gbm.w != 0 && gbm.h != 0)
            stbtt_Rasterize(gbm, 0.35f, vertices, num_verts, scale_x, scale_y, shift_x, shift_y, ix0[0], iy0[0], true);

    }

    private static void stbtt__add_point(TTPoint[] points, int n, float x, float y)
    {
        if (points == null) return; // during first pass, it's unallocated
        points[n].x = x;
        points[n].y = y;
    }

    // tesselate until threshhold p is happy... @TODO warped to compensate for non-linear stretching
    static int stbtt__tesselate_curve(TTPoint[] points, int[] num_points, float x0, float y0, float x1, float y1, float x2, float y2, float objspace_flatness_squared, int n)
    {
        // midpoint
        float mx = (x0 + 2*x1 + x2)/4;
        float my = (y0 + 2*y1 + y2)/4;
        // versus directly drawn line
        float dx = (x0+x2)/2 - mx;
        float dy = (y0+y2)/2 - my;
        if (n > 16) // 65536 segments on one curve better be enough!
            return 1;
        if (dx*dx+dy*dy > objspace_flatness_squared) { // half-pixel error allowed... need to be smaller if AA
            stbtt__tesselate_curve(points, num_points, x0,y0, (x0+x1)/2.0f,(y0+y1)/2.0f, mx,my, objspace_flatness_squared,n+1);
            stbtt__tesselate_curve(points, num_points, mx,my, (x1+x2)/2.0f,(y1+y2)/2.0f, x2,y2, objspace_flatness_squared,n+1);
        } else {
            stbtt__add_point(points, num_points[0],x2,y2);
            num_points[0]++;
        }
        return 1;
    }

    static void stbtt__tesselate_cubic(TTPoint[] points, int[] num_points, float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, float objspace_flatness_squared, int n)
    {
        // @TODO this "flatness" calculation is just made-up nonsense that seems to work well enough
        float dx0 = x1-x0;
        float dy0 = y1-y0;
        float dx1 = x2-x1;
        float dy1 = y2-y1;
        float dx2 = x3-x2;
        float dy2 = y3-y2;
        float dx = x3-x0;
        float dy = y3-y0;
        float longlen = (float) (Math.sqrt(dx0*dx0+dy0*dy0)+Math.sqrt(dx1*dx1+dy1*dy1)+Math.sqrt(dx2*dx2+dy2*dy2));
        float shortlen = (float) Math.sqrt(dx*dx+dy*dy);
        float flatness_squared = longlen*longlen-shortlen*shortlen;

        if (n > 16) // 65536 segments on one curve better be enough!
            return;

        if (flatness_squared > objspace_flatness_squared) {
            float x01 = (x0+x1)/2;
            float y01 = (y0+y1)/2;
            float x12 = (x1+x2)/2;
            float y12 = (y1+y2)/2;
            float x23 = (x2+x3)/2;
            float y23 = (y2+y3)/2;

            float xa = (x01+x12)/2;
            float ya = (y01+y12)/2;
            float xb = (x12+x23)/2;
            float yb = (y12+y23)/2;

            float mx = (xa+xb)/2;
            float my = (ya+yb)/2;

            stbtt__tesselate_cubic(points, num_points, x0,y0, x01,y01, xa,ya, mx,my, objspace_flatness_squared,n+1);
            stbtt__tesselate_cubic(points, num_points, mx,my, xb,yb, x23,y23, x3,y3, objspace_flatness_squared,n+1);
        } else {
            stbtt__add_point(points, num_points[0],x3,y3);
            num_points[0]++;
        }
    }

    public static TTPoint[] stbtt_FlattenCurves(TTVertex[] vertices, int num_verts, float objspace_flatness, int[][] contour_lengths, int[] num_contours)
    {
        TTPoint[] points=null;
        int num_points=0;

        float objspace_flatness_squared = objspace_flatness * objspace_flatness;
        int i,n=0,start=0, pass;

        // count how many "moves" there are to get the contour count
        for (i=0; i < num_verts; ++i)
            if (vertices[i].type == STBTT_vmove)
                ++n;

        num_contours[0] = n;
        if (n == 0) return null;

        contour_lengths[0] = new int[n];

        if (contour_lengths == null) {
            num_contours[0] = 0;
            return null;
        }

        // make two passes through the points so we don't need to realloc
        int[] tnp = {0};
        for (pass=0; pass < 2; ++pass) {
            float x=0,y=0;
            if (pass == 1) {
                points = new TTPoint[num_points];
                for (int l = 0; l < num_points; l++) {
                    points[l] = new TTPoint();
                }
            }
            num_points = 0;
            n= -1;
            for (i=0; i < num_verts; ++i) {
                switch (vertices[i].type) {
                    case STBTT_vmove:
                        // start the next contour
                        if (n >= 0)
                            contour_lengths[0][n] = num_points - start;
                        ++n;
                        start = num_points;

                        x = vertices[i].x;
                        y = vertices[i].y;
                        stbtt__add_point(points, num_points++, x,y);
                        break;
                    case STBTT_vline:
                        x = vertices[i].x;
                        y = vertices[i].y;
                        stbtt__add_point(points, num_points++, x, y);
                        break;
                    case STBTT_vcurve:
                        tnp[0] = num_points;
                        stbtt__tesselate_curve(points, tnp, x,y,
                                vertices[i].cx, vertices[i].cy,
                                vertices[i].x,  vertices[i].y,
                                objspace_flatness_squared, 0);
                        num_points = tnp[0];
                        x = vertices[i].x;
                        y = vertices[i].y;
                        break;
                    case STBTT_vcubic:
                        tnp[0] = num_points;
                        stbtt__tesselate_cubic(points, tnp, x,y,
                                vertices[i].cx, vertices[i].cy,
                                vertices[i].cx1, vertices[i].cy1,
                                vertices[i].x,  vertices[i].y,
                                objspace_flatness_squared, 0);
                        num_points = tnp[0];
                        x = vertices[i].x;
                        y = vertices[i].y;
                        break;
                }
            }
            contour_lengths[0][n] = num_points - start;
        }

        return points;
    }

    public static void stbtt_Rasterize(TTBitmap result, float flatness_in_pixels, TTVertex[] vertices, int num_verts, float scale_x, float scale_y, float shift_x, float shift_y, int x_off, int y_off, boolean invert)
    {
        float scale = scale_x > scale_y ? scale_y : scale_x;
        int[] winding_count = {0};
        int[][] winding_lengths = new int[1][];
        winding_lengths[0] = new int[1];
        TTPoint[] windings = stbtt_FlattenCurves(vertices, num_verts, flatness_in_pixels / scale, winding_lengths, winding_count);
        if (windings != null) {
            stbtt__rasterize(result, windings, winding_lengths[0], winding_count[0], scale_x, scale_y, shift_x, shift_y, x_off, y_off, invert);
        }
    }

    public static void stbtt_GetCodepointHMetrics(TTFontInfo info, int codepoint, int[] advanceWidth, int[] leftSideBearing)
    {
        stbtt_GetGlyphHMetrics(info, stbtt_FindGlyphIndex(info,codepoint), advanceWidth, leftSideBearing);
    }

    public int stbtt_GetCodepointKernAdvance(TTFontInfo info, int ch1, int ch2)
    {
        if (info.kern == 0) // if no kerning table, don't waste time looking up both codepoint->glyphs
            return 0;
        return stbtt_GetGlyphKernAdvance(info, stbtt_FindGlyphIndex(info,ch1), stbtt_FindGlyphIndex(info,ch2));
    }

    public static int stbtt_GetGlyphKernAdvance(TTFontInfo info, int glyph1, int glyph2)
    {
        OffsetArray data = new OffsetArray(info.data, info.kern);
        int needle;
        long straw;
        int l, r, m;

        // we only look at the first table. it must be 'horizontal' and format 0.
        if (info.kern == 0)
            return 0;
        if (readUShort(data.array, data.o + 2) < 1) // number of tables, need at least 1
            return 0;
        if (readUShort(data.array, data.o + 8) != 1) // horizontal flag must be set in format
            return 0;

        l = 0;
        r = readUShort(data.array, data.o + 10) - 1;
        needle = glyph1 << 16 | glyph2;
        while (l <= r) {
            m = (l + r) >> 1;
            straw = readULong(data.array, data.o+18+(m*6)); // note: unaligned read
            if (needle < straw)
                r = m - 1;
            else if (needle > straw)
                l = m + 1;
            else
                return readShort(data.array, data.o+22+(m*6));
        }
        return 0;
    }

    static void stbtt__h_prefilter(OffsetArray pixs, int w, int h, int stride_in_bytes, int kernel_width)
    {

        OffsetArray pixels = new OffsetArray(pixs.array, pixs.o);
        byte[] buffer = new byte[STBTT_MAX_OVERSAMPLE];
        int safe_w = w - kernel_width;
        int j;
        for (j=0; j < h; ++j) {
            int i;
            int total;
            for (int l = 0; l < kernel_width; l++) {
                buffer[l] = 0;
            }

            total = 0;

            // make kernel_width a constant in common cases so compiler can optimize out the divide
            switch (kernel_width) {
                case 2:
                    for (i=0; i <= safe_w; ++i) {
                        total += Byte.toUnsignedInt(pixels.get(i)) - Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                        buffer[(i+kernel_width) & STBTT__OVER_MASK] = pixels.get(i);
                        pixels.set(i, (byte) (total / 2));
                    }
                    break;
                case 3:
                    for (i=0; i <= safe_w; ++i) {
                        total += Byte.toUnsignedInt(pixels.get(i)) - Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                        buffer[(i+kernel_width) & STBTT__OVER_MASK] = pixels.get(i);
                        pixels.set(i, (byte) (total / 3));
                    }
                    break;
                case 4:
                    for (i=0; i <= safe_w; ++i) {
                        total += Byte.toUnsignedInt(pixels.get(i)) - Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                        buffer[(i+kernel_width) & STBTT__OVER_MASK] = pixels.get(i);
                        pixels.set(i, (byte) (total / 4));
                    }
                    break;
                case 5:
                    for (i=0; i <= safe_w; ++i) {
                        total += Byte.toUnsignedInt(pixels.get(i)) - Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                        buffer[(i+kernel_width) & STBTT__OVER_MASK] = pixels.get(i);
                        pixels.set(i, (byte) (total / 5));
                    }
                    break;
                default:
                    for (i=0; i <= safe_w; ++i) {
                        total += Byte.toUnsignedInt(pixels.get(i)) - Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                        buffer[(i+kernel_width) & STBTT__OVER_MASK] = pixels.get(i);
                        pixels.set(i, (byte) (total / kernel_width));
                    }
                    break;
            }

            for (; i < w; ++i) {
                total -= Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                pixels.set(i, (byte) (total / kernel_width));
            }

            pixels.increment(stride_in_bytes);
        }

    }

    static void stbtt__v_prefilter(OffsetArray pixs, int w, int h, int stride_in_bytes, int kernel_width)
    {

        OffsetArray pixels = new OffsetArray(pixs.array, pixs.o);
        byte[] buffer = new byte[STBTT_MAX_OVERSAMPLE];
        int safe_h = h - kernel_width;
        int j;
        for (j=0; j < w; ++j) {
            int i;
            int total;for (int l = 0; l < kernel_width; l++) {
                buffer[l] = 0;
            }

            total = 0;

            // make kernel_width a constant in common cases so compiler can optimize out the divide
            switch (kernel_width) {
                case 2:
                    for (i=0; i <= safe_h; ++i) {
                        total += Byte.toUnsignedInt(pixels.get(i*stride_in_bytes)) -Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                        buffer[(i+kernel_width) & STBTT__OVER_MASK] = pixels.get(i*stride_in_bytes);
                        pixels.set(i*stride_in_bytes, (byte) (total / 2));
                    }
                    break;
                case 3:
                    for (i=0; i <= safe_h; ++i) {
                        total += Byte.toUnsignedInt(pixels.get(i*stride_in_bytes)) -Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                        buffer[(i+kernel_width) & STBTT__OVER_MASK] = pixels.get(i*stride_in_bytes);
                        pixels.set(i*stride_in_bytes, (byte) (total / 3));
                    }
                    break;
                case 4:
                    for (i=0; i <= safe_h; ++i) {
                        total += Byte.toUnsignedInt(pixels.get(i*stride_in_bytes)) -Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                        buffer[(i+kernel_width) & STBTT__OVER_MASK] = pixels.get(i*stride_in_bytes);
                        pixels.set(i*stride_in_bytes, (byte) (total / 4));
                    }
                    break;
                case 5:
                    for (i=0; i <= safe_h; ++i) {
                        total += Byte.toUnsignedInt(pixels.get(i*stride_in_bytes)) -Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                        buffer[(i+kernel_width) & STBTT__OVER_MASK] = pixels.get(i*stride_in_bytes);
                        pixels.set(i*stride_in_bytes, (byte) (total / 5));
                    }
                    break;
                default:
                    for (i=0; i <= safe_h; ++i) {
                        total += Byte.toUnsignedInt(pixels.get(i*stride_in_bytes)) -Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                        buffer[(i+kernel_width) & STBTT__OVER_MASK] = pixels.get(i*stride_in_bytes);
                        pixels.set(i*stride_in_bytes, (byte) (total / kernel_width));
                    }
                    break;
            }

            for (; i < h; ++i) {
                total -= Byte.toUnsignedInt(buffer[i & STBTT__OVER_MASK]);
                pixels.set(i*stride_in_bytes, (byte) (total / kernel_width));
            }

            pixels.increment(1);
        }
    }

    public static int stbtt_PackFontRangesRenderIntoRects(TTPackContext spc, TTFontInfo info, TTPackRange[] ranges, int num_ranges, TTRPRect[] rects)
    {
        int i,j,k, return_value = 1;

        // save current values
        int old_h_over = spc.h_oversample;
        int old_v_over = spc.v_oversample;

        k = 0;
        for (i=0; i < num_ranges; ++i) {
            float fh = ranges[i].font_size;
            float scale = fh > 0 ? stbtt_ScaleForPixelHeight(info, fh) : stbtt_ScaleForMappingEmToPixels(info, -fh);
            float recip_h,recip_v,sub_x,sub_y;
            spc.h_oversample = ranges[i].h_oversample;
            spc.v_oversample = ranges[i].v_oversample;
            recip_h = 1.0f / spc.h_oversample;
            recip_v = 1.0f / spc.v_oversample;
            sub_x = stbtt__oversample_shift(spc.h_oversample);
            sub_y = stbtt__oversample_shift(spc.v_oversample);
            for (j=0; j < ranges[i].num_chars; ++j) {
                TTRPRect r = rects[k];
                if (r.wasPacked != 0) {
                    TTPackedChar bc = ranges[i].chardata_for_range[j];
                    int advance, lsb, x0,y0,x1,y1;
                    int codepoint = ranges[i].array_of_unicode_codepoints == null ? ranges[i].first_unicode_codepoint_in_range + j : ranges[i].array_of_unicode_codepoints[j];
                    int glyph = stbtt_FindGlyphIndex(info, codepoint);
                    int pad = spc.padding;

                    // pad on left and top
                    r.x += pad;
                    r.y += pad;
                    r.w -= pad;
                    r.h -= pad;
                    int[] tadvance = {0}, tlsb = {0}, tx0 = {0}, ty0 = {0}, tx1 = {0}, ty1 = {0};
                    stbtt_GetGlyphHMetrics(info, glyph, tadvance, tlsb);
                    advance = tadvance[0];
                    lsb = tlsb[0];

                    stbtt_GetGlyphBitmapBox(info, glyph,
                            scale * spc.h_oversample,
                            scale * spc.v_oversample,
                            tx0,ty0,tx1,ty1);
                    x0 = tx0[0];
                    x1 = tx1[0];
                    y0 = ty0[0];
                    y1 = ty1[0];
                    stbtt_MakeGlyphBitmapSubpixel(info,
                            new OffsetArray(spc.pixels, r.x + r.y*spc.stride_in_bytes),
                            r.w - spc.h_oversample +1,
                            r.h - spc.v_oversample +1,
                            spc.stride_in_bytes,
                            scale * spc.h_oversample,
                            scale * spc.v_oversample,
                            0,0,
                            glyph);

                    if (spc.h_oversample > 1)
                        stbtt__h_prefilter(new OffsetArray(spc.pixels, r.x + r.y*spc.stride_in_bytes),
                                r.w, r.h, spc.stride_in_bytes,
                                spc.h_oversample);

                    if (spc.v_oversample > 1)
                        stbtt__v_prefilter(new OffsetArray(spc.pixels, r.x + r.y*spc.stride_in_bytes),
                                r.w, r.h, spc.stride_in_bytes,
                                spc.v_oversample);

                    bc.x0       = r.x;
                    bc.y0       = r.y;
                    bc.x1       = (r.x + r.w);
                    bc.y1       =  (r.y + r.h);
                    bc.xadvance =                scale * advance;
                    bc.xoff     =       (float)  x0 * recip_h + sub_x;
                    bc.yoff     =       (float)  y0 * recip_v + sub_y;
                    bc.xoff2    =                (x0 + r.w) * recip_h + sub_x;
                    bc.yoff2    =                (y0 + r.h) * recip_v + sub_y;
                } else {
                    return_value = 0; // if any fail, report failure
                }

                ++k;
            }
        }

        // restore original values
        spc.h_oversample = old_h_over;
        spc.v_oversample = old_v_over;

        return return_value;
    }

    public static void stbrp_pack_rects(TTRPContext con, TTRPRect[] rects, int num_rects)
    {
        int i;
        for (i=0; i < num_rects; ++i) {
            if (con.x + rects[i].w > con.width) {
                con.x = 0;
                con.y = con.bottomY;
            }
            if (con.y + rects[i].h > con.height)
                break;
            rects[i].x = con.x;
            rects[i].y = con.y;
            rects[i].wasPacked = 1;
            con.x += rects[i].w;
            if (con.y + rects[i].h > con.bottomY)
                con.bottomY = con.y + rects[i].h;
        }
        for (   ; i < num_rects; ++i)
            rects[i].wasPacked = 0;
    }

    public static void stbtt_PackFontRangesPackRects(TTPackContext spc, TTRPRect[] rects, int num_rects)
    {
        stbrp_pack_rects(spc.packInfo, rects, num_rects);
    }

    static short readShort(byte[] data, int offset) {
        return (short) (Byte.toUnsignedInt(data[offset]) * 256 + Byte.toUnsignedInt(data[offset + 1]));
    }

    static int readUShort(byte[] data, int offset) {
        return Byte.toUnsignedInt(data[offset]) * 256 + Byte.toUnsignedInt(data[offset + 1]);
    }

    static long readULong(byte[] data, int offset) {
        return (Byte.toUnsignedLong(data[offset])<<24) + (Byte.toUnsignedLong(data[offset + 1])<<16) + (Byte.toUnsignedLong(data[offset + 2])<<8) + Byte.toUnsignedLong(data[offset + 3]);
    }

    static TTBuf getSubrs(TTBuf cff, TTBuf fontdict)
    {
        int subrsoff = 0;
        int[] private_loc = { 0, 0 };
        TTBuf pdict;
        stbtt__dict_get_ints(fontdict, 18, 2, private_loc);
        if (private_loc[1] == 0 || private_loc[0] == 0) {
            return new TTBuf();
        }
        pdict = cff.bufRange(private_loc[1], private_loc[0]);
        int[] temp = new int[0];
        stbtt__dict_get_ints(pdict, 19, 1, temp);
        subrsoff = temp[0];
        if (subrsoff == 0) {
            return new TTBuf();
        }
        cff.seek(private_loc[1]+subrsoff);
        return stbtt__cff_get_index(cff);
    }

    static  boolean isFont(byte[] font)
    {
        // check the version number
        if (stbtt_tag4(font, 0, '1', (char) 0, (char) 0, (char) 0)) {
            return true; // STBTT 1
        }
        if (stbtt_tag(font, 0, "typ1")) {
            return true; // STBTT with type 1 font -- we don't support this!
        }
        if (stbtt_tag(font, 0, "OTTO")) {
            return true; // OpenType with CFF
        }
        if (stbtt_tag4(font, 0, (char) 0, (char) 1, (char) 0, (char) 0)) {
            return true; // OpenType 1.0
        }
        if (stbtt_tag(font, 0, "true")) {
            return true; // Apple specification for STBTT fonts
        }
        return false;
    }

    static int stbtt_GetFontOffsetForIndex(byte[] data, int index) {
// if it's just a font, there's only one valid index
        if (isFont(data))
            return index == 0 ? 0 : -1;

        // check if it's a TTC
        if (stbtt_tag(data, 0, "ttcf")) {
            // version 1?
            if (readULong(data, 4) == 0x00010000 || readULong(data, 4) == 0x00020000) {
                int n = (int) readULong(data, 8);
                if (index >= n)
                    return -1;
                return (int) readULong(data, 12+index*4);
            }
        }
        return -1;
    }

    private static int stbtt__cff_int(TTBuf b)
    {
        int b0 = b.getByte();
        if (b0 >= 32 && b0 <= 246) {
            return b0 - 139;
        } else if (b0 >= 247 && b0 <= 250) {
            return (b0 - 247)*256 + b.getByte() + 108;
        } else if (b0 >= 251 && b0 <= 254) {
            return -(b0 - 251)*256 - b.getByte() - 108;
        } else if (b0 == 28) {
            return b.getShort();
        } else if (b0 == 29) {
            return b.getInt();
        }
        return 0;
    }

    public static void stbtt_GetFontVMetrics(TTFontInfo info, int[] ascent, int[] descent, int[] lineGap) {
        if (ascent != null) ascent[0]  = readShort(info.data, info.hhea + 4);
        if (descent != null) descent[0] = readShort(info.data, info.hhea + 6);
        if (lineGap != null) lineGap[0] = readShort(info.data, info.hhea + 8);
    }

    private static void stbtt__cff_skip_operand(TTBuf b) {
        int v, b0 = b.peekByte();
        if (b0 == 30) {
            b.skip(1);
            while (b.cursor < b.size) {
                v = b.getByte();
                if ((v & 0xF) == 0xF || (v >> 4) == 0xF) {
                    break;
                }
            }
        } else {
            stbtt__cff_int(b);
        }
    }

    private static TTBuf stbtt__dict_get(TTBuf b, int key)
    {
        b.seek(0);
        while (b.cursor < b.size) {
            int start = b.cursor, end, op;
            while (b.peekByte() >= 28)
                stbtt__cff_skip_operand(b);
            end = b.cursor;
            op = b.getByte();
            if (op == 12)  {
                op = b.getByte() | 0x100;
            }

            if (op == key) {
                return b.bufRange(start, end-start);
            }
        }
        return b.bufRange(0, 0);
    }

    static void stbtt__dict_get_ints(TTBuf b, int key, int outcount, int[] out)
    {
        int i;
        TTBuf operands = stbtt__dict_get(b, key);
        for (i = 0; i < outcount && operands.cursor < operands.size; i++)
            out[i] = stbtt__cff_int(operands);
    }

    static TTBuf stbtt__cff_index_get(TTBuf b, int i)
    {
        int count, offsize, start, end;
        b.seek(0);
        count = b.getInt();
        offsize = b.getByte();
        b.skip(i * offsize);
        start = b.get(offsize);
        end = b.get(offsize);
        return b.bufRange(2+(count+1)*offsize+start, end - start);
    }

    static TTBuf stbtt__cff_get_index(TTBuf b)
    {
        int count, start, offsize;
        start = b.cursor;
        count = b.getShort();
        if (count != 0) {
            offsize = b.getShort();
            b.skip(offsize * count);
            b.skip(b.get(offsize) - 1);
        }
        return b.bufRange(start, b.cursor - start);
    }

    private static boolean stbtt_tag4(byte[] data, int offset, char c0, char c1, char c2, char c3) {
        return (data[offset] == c0 && data[offset + 1] == c1 && data[offset + 2] == c2 && data[offset + 3] == c3);
    }

    private static boolean stbtt_tag(byte[] data, int offset, String tag) {
        return stbtt_tag4(data, offset, tag.charAt(0), tag.charAt(1), tag.charAt(2), tag.charAt(3));
    }

    static int stbtt__find_table(byte[] data, int fontStart, String tag) {
        int num_tables = readUShort(data, fontStart + 4);
        int tabledir = fontStart + 12;

        for (int i=0; i < num_tables; ++i) {
            int loc = tabledir + 16 * i;
            if (stbtt_tag(data, loc, tag)) {
                return (int) readULong(data, loc + 8);
            }
        }
        return 0;
    }
}
