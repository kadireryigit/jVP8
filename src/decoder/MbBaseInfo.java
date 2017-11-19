package decoder;

public class MbBaseInfo {
	public int y_mode    ;
	public int uv_mode   ;
	public int segment_id;
	public int ref_frame ;
	public boolean skip_coeff;
	public int need_mc_border ;
	public int  partitioning ;
	public MotionVector mv;
	public int  eob_mask;
}
