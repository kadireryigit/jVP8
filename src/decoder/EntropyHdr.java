package decoder;

public class EntropyHdr {

	public int[][][][] coeff_probs = new int[Constants.BLOCK_TYPES][Constants.COEF_BANDS][Constants.PREV_COEF_CONTEXTS][Constants.ENTROPY_NODES];
	public int[][] mv_probs = new int[Constants.MV_PROB_CNT][2];
	public boolean coeff_skip_enabled;
	public int coeff_skip_prob;
	public int[] y_mode_probs = new int[4];
	public int[] uv_mode_probs = new int[3];
	public int prob_inter;
	public int prob_last;
	public int prob_gf;
	
	
}
