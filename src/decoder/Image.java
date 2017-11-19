package decoder;

public class Image {

	public int w;
	public int h;
	
	public int[] data;
	public int ref_cnt;
	
	
	public void releaseRefFrame(){
		if(ref_cnt>0)
			ref_cnt--;
		else{
			System.err.println("Ref count mustnt be less than 0");
			System.exit(-1);
		}
	}
}
