package decoder;

public class SegmentHdr {

	public boolean enabled;
	public boolean update_data;
	public boolean update_map;
	public boolean abs; /* 0=deltas, 1=absolute values */
	public int[] tree_probs;
	public int[] lf_level;
	public int[] quant_idx;
	
	public SegmentHdr(){
		enabled = false;
		update_data = false;
		update_map = false;
		abs = false;
		tree_probs = new int[Constants.MB_FEATURE_TREE_PROBS];
		lf_level = new int[Constants.MAX_MB_SEGMENTS];        
		quant_idx = new int[Constants.MAX_MB_SEGMENTS];       
		
	}
}
