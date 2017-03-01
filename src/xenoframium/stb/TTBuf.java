package xenoframium.stb;

class TTBuf {
    private byte[] data;
    int size;
    int cursor;

    public TTBuf() {
        this(new byte[0]);
    }

    public TTBuf(byte[] p) {
        data = p;
        this.size = p.length;
        cursor = 0;
    }

    public TTBuf(byte[] p, int offset, int length) {
        data = p;
        cursor = offset;
        size = length;
    }

    public TTBuf bufRange(int o, int s) {
        if (o < 0 || s < 0 || o > size || s > size - o) {
            return new TTBuf();
        }
        return new TTBuf(data, o, s);
    }

    public void seek(int index) {
        cursor = (index > size || index < 0) ? size : index;
    }

    public void skip(int numberOfBytesToSkip) {
        seek(cursor + numberOfBytesToSkip);
    }

    public int get(int n) {
        int v = 0;
        int i;
        for (i = 0; i < n; i++) {
            v = (v << 8) | getByte();
        }

        return v;
    }

    public int getByte() {
        if (cursor >= size) {
            return 0;
        }
        return Byte.toUnsignedInt(data[cursor++]);
    }

    public int getShort() {
        return get(2);
    }

    public int getInt() {
        return get(4);
    }

    public int peekByte() {
        if (cursor >= size) {
            return 0;
        }
        return Byte.toUnsignedInt(data[cursor]);
    }
}
