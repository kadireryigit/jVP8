package decoder;

public class Constants {
	
	
	//reference Frames
	public static final int CURRENT_FRAME 	= 0;
    public static final int LAST_FRAME		= 1;
    public static final int GOLDEN_FRAME	= 2;
    public static final int ALTREF_FRAME	= 3;
    public static final int NUM_REF_FRAMES	= 4;
	
	
	public static final int MAX_PARTITIONS = 8;
	
	public static final int BLOCK_CONTEXTS = 4;
	
	public static final int MB_FEATURE_TREE_PROBS = 3;
	public static final int   MAX_MB_SEGMENTS = 4;
	
	public static final int FRAME_HEADER_SZ = 3;
	public static final int KEYFRAME_HEADER_SZ = 7;
		    
		    
	/*!\brief Algorithm return codes */
//    typedef enum {
        /*!\brief Operation completed without error */
       public static final int VPX_CODEC_OK = 0;

        /*!\brief Unspecified error */
       public static final int  VPX_CODEC_ERROR = 1;

        /*!\brief Memory operation failed */
       public static final int  VPX_CODEC_MEM_ERROR = 2;

        /*!\brief ABI version mismatch */
       public static final int  VPX_CODEC_ABI_MISMATCH = 3;

        /*!\brief Algorithm does not have required capability */
       public static final int  VPX_CODEC_INCAPABLE = 4;

        /*!\brief The given bitstream is not supported.
         *
         * The bitstream was unable to be parsed at the highest level. The decoder
         * is unable to proceed. This error \ref SHOULD be treated as fatal to the
         * stream. */
       public static final int   VPX_CODEC_UNSUP_BITSTREAM = 5;

        /*!\brief Encoded bitstream uses an unsupported feature
         *
         * The decoder does not implement a feature required by the encoder. This
         * return code should only be used for features that prevent future
         * pictures from being properly decoded. This error \ref MAY be treated as
         * fatal to the stream or \ref MAY be treated as fatal to the current GOP.
         */
       public static final int VPX_CODEC_UNSUP_FEATURE = 6;

        /*!\brief The coded data for this stream is corrupt or incomplete
         *
         * There was a problem decoding the current frame.  This return code
         * should only be used for failures that prevent future pictures from
         * being properly decoded. This error \ref MAY be treated as fatal to the
         * stream or \ref MAY be treated as fatal to the current GOP. If decoding
         * is continued for the current GOP, artifacts may be present.
         */
       public static final int VPX_CODEC_CORRUPT_FRAME = 7;

        /*!\brief An application-supplied parameter is not valid.
         *
         */
       public static final int VPX_CODEC_INVALID_PARAM = 8;

        /*!\brief An iterator reached the end of list.
         *
         */
       public static final int  VPX_CODEC_LIST_END = 9;

//    }
//    vpx_codec_err_t;
}
