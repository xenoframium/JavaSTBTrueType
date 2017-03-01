package xenoframium.stb;

public class TTPackRange {
    float font_size;
    int first_unicode_codepoint_in_range;  // if non-zero, then the chars are continuous, and this is the first codepoint
    int[] array_of_unicode_codepoints;       // if non-zero, then this is an array of unicode codepoints
    int num_chars;
    TTPackedChar[] chardata_for_range; // output
    int h_oversample, v_oversample; // don't set these, they're used internally
}
