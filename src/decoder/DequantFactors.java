package decoder;

public class DequantFactors {
	public int   quant_idx;
    public short[][] factor; /* [ Y1, UV, Y2 ] [ DC, AC ] */
}
