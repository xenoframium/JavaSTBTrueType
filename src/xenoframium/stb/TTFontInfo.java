package xenoframium.stb;

public class TTFontInfo {
//    void           * userdata;
    byte[] data;              // .ttf file bytes
    int fontstart;         // offset of start of font

    public final int numGlyphs;                     // number of glyphs, needed for range checking

    int loca;
    int head;
    int glyf;
    int hhea;
    int hmtx;
    int kern; // table locations as offset from start of .ttf
    int indexMap;                     // a cmap mapping for our chosen character encoding
    int indexToLocFormat;              // format needed to map from glyph index to glyph

    TTBuf cff;                    // cff font data
    TTBuf charstrings;            // the charstring index
    TTBuf gsubrs;                 // global charstring subroutines index
    TTBuf subrs;                  // charstring subroutines index
    TTBuf fontdicts = new TTBuf(new byte[0]);              // array of font dicts
    TTBuf fdselect = new TTBuf(new byte[0]);               // map from glyph to fontdict
    final int STBTT_PLATFORM_ID_UNICODE   =0;
    final int STBTT_PLATFORM_ID_MAC = 1;
    final int STBTT_PLATFORM_ID_ISO = 2;
    final int STBTT_PLATFORM_ID_MICROSOFT = 3;
    final int STBTT_UNICODE_EID_UNICODE_1_0    =0,
    STBTT_UNICODE_EID_UNICODE_1_1    =1,
    STBTT_UNICODE_EID_ISO_10646      =2,
    STBTT_UNICODE_EID_UNICODE_2_0_BMP=3,
    STBTT_UNICODE_EID_UNICODE_2_0_FULL=4,
            STBTT_MS_EID_SYMBOL        =0,
            STBTT_MS_EID_UNICODE_BMP   =1,
            STBTT_MS_EID_SHIFTJIS      =2,
            STBTT_MS_EID_UNICODE_FULL  =10;

    public TTFontInfo(byte[] data) {
        this(data, 0);
    }

    public TTFontInfo(byte[] data, int fontIndex) {
        int cmap;
        int t;
        int i;
        int numTables;

        this.data = data;
        this.fontstart = STBTT.stbtt_GetFontOffsetForIndex(data, fontIndex);
        cff = new TTBuf();

        cmap = STBTT.stbtt__find_table(data, fontstart, "cmap");       // required
        loca = STBTT.stbtt__find_table(data, fontstart, "loca"); // required
        head = STBTT.stbtt__find_table(data, fontstart, "head"); // required
        glyf = STBTT.stbtt__find_table(data, fontstart, "glyf"); // required
        hhea = STBTT.stbtt__find_table(data, fontstart, "hhea"); // required
        hmtx = STBTT.stbtt__find_table(data, fontstart, "hmtx"); // required
        kern = STBTT.stbtt__find_table(data, fontstart, "kern"); // not required

        if (cmap == 0 || head == 0|| hhea == 0 || hmtx == 0) {
            throw new TTFontException();
        }
        if (glyf != 0) {
            if (loca == 0) {
                throw new TTFontException();
            }
        } else {
            // initialization for CFF / Type2 fonts (OTF)
            TTBuf b;
            TTBuf topdict;
            TTBuf topdictidx;

            int cstype = 2;
            int charstrings = 0;
            int fdarrayoff = 0;
            int fdselectoff = 0;
            int cff;

            cff = STBTT.stbtt__find_table(data, fontstart, "CFF ");
            if (cff == 0) {
                throw new TTFontException();
            }

            // @TODO this should use size from table (not 512MB)
            this.cff = new TTBuf(data, cff, 512*1024*1024);
            b = this.cff;

            // read the header
            b.skip(2);
            b.skip(b.getByte());

            // @TODO the name INDEX could list multiple fonts,
            // but we just use the first one.
            STBTT.stbtt__cff_get_index(b);  // name INDEX
            topdictidx = STBTT.stbtt__cff_get_index(b);
            topdict = STBTT.stbtt__cff_index_get(topdictidx, 0);
            STBTT.stbtt__cff_get_index(b);  // string INDEX
            gsubrs = STBTT.stbtt__cff_get_index(b);

            int[] temp = new int[1];

            STBTT.stbtt__dict_get_ints(topdict, 17, 1, temp);
            charstrings = temp[0];
            STBTT.stbtt__dict_get_ints(topdict, 0x100 | 6, 1, temp);
            cstype = temp[0];
            STBTT.stbtt__dict_get_ints(topdict, 0x100 | 36, 1, temp);
            fdarrayoff = temp[0];
            STBTT.stbtt__dict_get_ints(topdict, 0x100 | 37, 1, temp);
            fdselectoff = temp[0];
            subrs = STBTT.getSubrs(b, topdict);

            // we only support Type 2 charstrings
            if (cstype != 2) {
                throw new TTFontException();
            }
            if (charstrings == 0) {
                throw new TTFontException();
            }

            if (fdarrayoff != 0) {
                // looks like a CID font
                if (fdselectoff == 0) {
                    throw new TTFontException();
                }
                b.seek(fdarrayoff);
                fontdicts = STBTT.stbtt__cff_get_index(b);
                fdselect = b.bufRange(fdselectoff, b.size-fdselectoff);
            }

            b.seek(charstrings);
            this.charstrings = STBTT.stbtt__cff_get_index(b);
        }

        t = STBTT.stbtt__find_table(data, fontstart, "maxp");
        if (t != 0) {
            numGlyphs = STBTT.readUShort(data, t + 4);
        } else {
            numGlyphs = 0xffff;
        }

        // find a cmap encoding table we understand *now* to avoid searching
        // later. (todo: could make this installable)
        // the same regardless of glyph.
        numTables = STBTT.readUShort(data, cmap + 2);
        indexMap = 0;

        for (i=0; i < numTables; ++i) {
            int encoding_record = cmap + 4 + 8 * i;
            // find an encoding we understand:
            switch(STBTT.readUShort(data, encoding_record)) {
                case STBTT_PLATFORM_ID_MICROSOFT:
                    switch (STBTT.readUShort(data, encoding_record+2)) {
                        case STBTT_MS_EID_UNICODE_BMP:
                        case STBTT_MS_EID_UNICODE_FULL:
                            // MS/Unicode
                            indexMap = cmap + (int) STBTT.readULong(data, encoding_record+4);
                            break;
                    }
                    break;
                case STBTT_PLATFORM_ID_UNICODE:
                    // Mac/iOS has these
                    // all the encodingIDs are unicode, so we don't bother to check it
                    indexMap = cmap + (int) STBTT.readULong(data, encoding_record+4);
                    break;
            }
        }
        if (indexMap == 0) {
            throw new TTFontException();
        }

        indexToLocFormat = STBTT.readUShort(data, head + 50);
    }

}
