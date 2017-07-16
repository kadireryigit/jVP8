package decoder;

public class FrameHdr {

	public boolean is_keyframe;     
	public int version;         
	public boolean is_experimental; 
	public boolean is_shown;        
	public int part0_sz;
	public boolean frame_size_updated;
	
	//KeyFrameHeader
	public int width = 0;
	public int height = 0;
	public int scale_w = 0;
	public int scale_h = 0;
}
