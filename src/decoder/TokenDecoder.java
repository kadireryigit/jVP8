package decoder;

public class TokenDecoder {

	public BoolDecoder  bool;
    public int[]  left_token_entropy_ctx = new int[4+2+2+1];
    public short[] coeffs;
    
    public TokenDecoder(){
    	bool = new BoolDecoder();
    }
    
}
