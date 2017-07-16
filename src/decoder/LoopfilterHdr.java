package decoder;

public class LoopfilterHdr {
	public boolean use_simple;
	public int level;
	public int sharpness;
	public boolean delta_enabled;
	public int[] ref_delta = new int[Constants.BLOCK_CONTEXTS];
	public int[] mode_delta = new int[Constants.BLOCK_CONTEXTS];

	public LoopfilterHdr() {
		use_simple = false;                                      
		level = 0;                                           
		sharpness = 0;                                       
		delta_enabled = false;                                   
		ref_delta = new int[Constants.BLOCK_CONTEXTS]; 
		mode_delta = new int[Constants.BLOCK_CONTEXTS];
		
	}
}
