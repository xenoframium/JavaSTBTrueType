package xenoframium.stb;

public class TTPackContext {
    TTRPContext packInfo;
    int width;
    int height;
    int stride_in_bytes;
    int padding;
    int h_oversample, v_oversample;
    byte[] pixels;
    TTRPNode[] nodes;
}
