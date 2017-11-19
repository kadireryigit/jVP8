package decoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class VP8Frame {

	private FileInputStream rawData;
	private File ivf_data;

	public VP8Frame(FileInputStream raw, File ivf) {
		rawData = raw;
		ivf_data = ivf;
	}

	public void decodeFrame(Context ctx, FileInputStream data, int sz) throws IOException
	{
//	    vpx_codec_err_t  res;
	    BoolDecoder  bool = new BoolDecoder();
	    int                  row, partition;

	    ctx.saved_entropy_valid = false; 

	    if (vp8_parse_frame_header(data, sz, ctx)>0)
	      System.err.println("Failed to parse frame header");

	    if (ctx.frame_hdr.is_experimental)
	    	System.err.println("Experimental bitstreams not supported.");

//	    data += FRAME_HEADER_SZ;	//not needed because vp8_parse_frame_header does this
	    sz -= Constants.FRAME_HEADER_SZ;

	    if (ctx.frame_hdr.is_keyframe)
	    {
//	        data += KEYFRAME_HEADER_SZ;
	        sz -= Constants.KEYFRAME_HEADER_SZ;
	        ctx.mb_cols = (ctx.frame_hdr.width + 15) / 16;
	        ctx.mb_rows = (ctx.frame_hdr.height + 15) / 16;
	    }

	    /* Start the bitreader for the header/entropy partition */
	    bool.init_bool_decoder(data, ctx.frame_hdr.part0_sz);

	    /* Skip the colorspace and clamping bits */
	    if (ctx.frame_hdr.is_keyframe)
	        if (bool.bool_get_uint(2)>0)
	            System.err.println("Reserved bits not supported.");

	    decode_segmentation_header(ctx, bool, ctx.segment_hdr);
	    decode_loopfilter_header(ctx, bool, ctx.loopfilter_hdr);
	    FileInputStream part1 = new FileInputStream(ivf_data);
	    part1.getChannel().position(data.getChannel().position()+ctx.frame_hdr.part0_sz); //Set position to part1 beginning
	    decode_and_init_token_partitions(ctx,
	                                     bool,
	                                     part1,
	                                     sz - ctx.frame_hdr.part0_sz,
	                                     ctx.token_hdr);
	    decode_quantizer_header(ctx, bool,ctx.quant_hdr);
	    decode_reference_header(ctx, bool, ctx.reference_hdr);

	    /* Set keyframe entropy defaults. These get updated on keyframes
	     * regardless of the refresh_entropy setting.
	     */
	    if (ctx.frame_hdr.is_keyframe)
	    {
	    	for (int i = 0; i < Constants.BLOCK_TYPES; i++) {
	    		System.arraycopy(Constants.k_default_coeff_probs    , 0 , ctx.entropy_hdr.coeff_probs[i]   , 0 , Constants.BLOCK_TYPES);
		        for (int j = 0; j < Constants.COEF_BANDS; j++) {
		        	System.arraycopy(Constants.k_default_coeff_probs[i]    , 0 , ctx.entropy_hdr.coeff_probs[i]   , 0 , Constants.COEF_BANDS);
		            for (int k = 0; k < Constants.PREV_COEF_CONTEXTS; k++) {
		            	System.arraycopy(Constants.k_default_coeff_probs[i][j]    , 0 , ctx.entropy_hdr.coeff_probs[i][j]   , 0 , Constants.PREV_COEF_CONTEXTS);
	                	 for (int l = 0; l < Constants.ENTROPY_NODES; l++)
	                		 System.arraycopy(Constants.k_default_coeff_probs[i][j][k]    , 0 , ctx.entropy_hdr.coeff_probs[i][j][k]   , 0 , Constants.ENTROPY_NODES);
		            }
		        }
	    	}
	        System.arraycopy(Constants.k_default_mv_probs[0]	, 0 , ctx.entropy_hdr.mv_probs[0]	, 0 , Constants.MV_PROB_CNT);           
	        System.arraycopy(Constants.k_default_mv_probs[1]	, 0 , ctx.entropy_hdr.mv_probs[1]	, 0 , Constants.MV_PROB_CNT);
		    System.arraycopy(Constants.k_default_y_mode_probs	, 0 , ctx.entropy_hdr.y_mode_probs  , 0 , Constants.k_default_y_mode_probs.length);   
			System.arraycopy(Constants.k_default_uv_mode_probs  , 0 , ctx.entropy_hdr.uv_mode_probs , 0 , Constants.k_default_uv_mode_probs.length); 
	    }

	    if (!ctx.reference_hdr.refresh_entropy)
	    {
	    	//need to copy entropy header
	        ctx.saved_entropy = ctx.entropy_hdr;
	        ctx.saved_entropy_valid = true;
	    }

	    decode_entropy_header(ctx, bool, ctx.entropy_hdr);

	    vp8_dixie_modemv_init(ctx);
	    vp8_dixie_tokens_init(ctx);
	    vp8_dixie_predict_init(ctx);
	    dequant_init(ctx.dequant_factors, ctx.segment_hdr,ctx.quant_hdr);

	    for (row = 0, partition = 0; row < ctx.mb_rows; row++)
	    {
	        vp8_dixie_modemv_process_row(ctx, bool, row, 0, ctx.mb_cols);
	        vp8_dixie_tokens_process_row(ctx, partition, row, 0,ctx.mb_cols);
//	        vp8_dixie_predict_process_row(ctx, row, 0, ctx->mb_cols);
//
//	        if (ctx->loopfilter_hdr.level && row)
//	            vp8_dixie_loopfilter_process_row(ctx, row - 1, 0,
//	                                             ctx->mb_cols);
//
//	        if (++partition == ctx->token_hdr.partitions)
//	            partition = 0;
	    }
//
//	    if (ctx->loopfilter_hdr.level)
//	        vp8_dixie_loopfilter_process_row(ctx, row - 1, 0, ctx->mb_cols);
//
//	    ctx->frame_cnt++;
//
//	    if (!ctx->reference_hdr.refresh_entropy)
//	    {
//	        ctx->entropy_hdr = ctx->saved_entropy;
//	        ctx->saved_entropy_valid = 0;
//	    }
//
//	    /* Handle reference frame updates */
//	    if (ctx->reference_hdr.copy_arf == 1)
//	    {
//	        vp8_dixie_release_ref_frame(ctx->ref_frames[ALTREF_FRAME]);
//	        ctx->ref_frames[ALTREF_FRAME] =
//	            vp8_dixie_ref_frame(ctx->ref_frames[LAST_FRAME]);
//	    }
//	    else if (ctx->reference_hdr.copy_arf == 2)
//	    {
//	        vp8_dixie_release_ref_frame(ctx->ref_frames[ALTREF_FRAME]);
//	        ctx->ref_frames[ALTREF_FRAME] =
//	            vp8_dixie_ref_frame(ctx->ref_frames[GOLDEN_FRAME]);
//	    }
//
//	    if (ctx->reference_hdr.copy_gf == 1)
//	    {
//	        vp8_dixie_release_ref_frame(ctx->ref_frames[GOLDEN_FRAME]);
//	        ctx->ref_frames[GOLDEN_FRAME] =
//	            vp8_dixie_ref_frame(ctx->ref_frames[LAST_FRAME]);
//	    }
//	    else if (ctx->reference_hdr.copy_gf == 2)
//	    {
//	        vp8_dixie_release_ref_frame(ctx->ref_frames[GOLDEN_FRAME]);
//	        ctx->ref_frames[GOLDEN_FRAME] =
//	            vp8_dixie_ref_frame(ctx->ref_frames[ALTREF_FRAME]);
//	    }
//
//	    if (ctx->reference_hdr.refresh_gf)
//	    {
//	        vp8_dixie_release_ref_frame(ctx->ref_frames[GOLDEN_FRAME]);
//	        ctx->ref_frames[GOLDEN_FRAME] =
//	            vp8_dixie_ref_frame(ctx->ref_frames[CURRENT_FRAME]);
//	    }
//
//	    if (ctx->reference_hdr.refresh_arf)
//	    {
//	        vp8_dixie_release_ref_frame(ctx->ref_frames[ALTREF_FRAME]);
//	        ctx->ref_frames[ALTREF_FRAME] =
//	            vp8_dixie_ref_frame(ctx->ref_frames[CURRENT_FRAME]);
//	    }
//
//	    if (ctx->reference_hdr.refresh_last)
//	    {
//	        vp8_dixie_release_ref_frame(ctx->ref_frames[LAST_FRAME]);
//	        ctx->ref_frames[LAST_FRAME] =
//	            vp8_dixie_ref_frame(ctx->ref_frames[CURRENT_FRAME]);
//	    }

	}

	public int vp8_parse_frame_header(FileInputStream data, int sz, Context hdr) throws IOException {
		long raw;

		if (sz < 10)
			return Constants.VPX_CODEC_CORRUPT_FRAME;

		/*
		 * The frame header is defined as a three byte little endian value
		 */
		raw = data.read() | (data.read() << 8) | (data.read() << 16);
		hdr.frame_hdr.is_keyframe = (raw & 1) == 0; // !BITS_GET(raw, 0, 1);
													// //bit 0
		hdr.frame_hdr.version = (int) ((raw >> 1) & 3);// BITS_GET(raw, 1, 2);
														// bits 2:1
		hdr.frame_hdr.is_experimental = ((raw >> 3) & 1) > 0;// BITS_GET(raw, 3,
															// 1); bit 3
		hdr.frame_hdr.is_shown = ((raw >> 4) & 1) > 0;// BITS_GET(raw, 4, 1); bit
														// 4
		hdr.frame_hdr.part0_sz = (int) ((raw >> 5) & ((1 << 19) - 1));// BITS_GET(raw,
																		// 5,
		// 19); bits 1

		if (sz <= hdr.frame_hdr.part0_sz + (hdr.frame_hdr.is_keyframe ? 10 : 3))
			return Constants.VPX_CODEC_CORRUPT_FRAME;

		hdr.frame_hdr.frame_size_updated = false;

		if (hdr.frame_hdr.is_keyframe) {

			/*
			 * Keyframe header consists of a three byte sync code followed by
			 * the width and height and associated scaling factors.
			 */
			if (data.read() != 0x9d || data.read() != 0x01 || data.read() != 0x2a)
				return Constants.VPX_CODEC_UNSUP_BITSTREAM;

			raw = data.read() | (data.read() << 8) | (data.read() << 16) | (data.read() << 24);
			int w = (int) (raw & ((1 << 14) - 1)); // BITS_GET(raw, 0, 14),
													// update);
			int scale_w = (int) ((raw >> 14) & 3); // BITS_GET(raw, 14, 2),
													// update);
			int h = (int) (raw >> 16) & ((1 << 16) - 1); // BITS_GET(raw, 16,
															// 14), update);
			int scale_h = (int) ((raw >> 30) & 3); // BITS_GET(raw, 30, 2),
													// update);

			boolean update = w != hdr.frame_hdr.width || scale_w != hdr.frame_hdr.scale_w || h != hdr.frame_hdr.height
					|| scale_h != hdr.frame_hdr.scale_h;
			hdr.frame_hdr.height = h;
			hdr.frame_hdr.width = w;
			hdr.frame_hdr.scale_h = scale_h;
			hdr.frame_hdr.scale_w = scale_w;

			hdr.frame_hdr.frame_size_updated = update;

			if (hdr.frame_hdr.width == 0 || hdr.frame_hdr.height == 0)
				return Constants.VPX_CODEC_UNSUP_BITSTREAM;
		}

		return Constants.VPX_CODEC_OK;
	}

	public void decode_segmentation_header(Context ctx, BoolDecoder bool, SegmentHdr hdr) throws IOException {
		if (ctx.frame_hdr.is_keyframe) {
			hdr = new SegmentHdr();
			ctx.segment_hdr = hdr;
			// memset(hdr, 0, sizeof(*hdr));
		}

		hdr.enabled = bool.bool_get_bit() > 0;

		if (hdr.enabled) {
			int i;

			hdr.update_map = bool.bool_get_bit() > 0;
			hdr.update_data = bool.bool_get_bit() > 0;

			if (hdr.update_data) {
				hdr.abs = bool.bool_get_bit() > 0;

				for (i = 0; i < Constants.MAX_MB_SEGMENTS; i++)
					hdr.quant_idx[i] = bool.bool_maybe_get_int(7);

				for (i = 0; i < Constants.MAX_MB_SEGMENTS; i++)
					hdr.lf_level[i] = bool.bool_maybe_get_int(6);
			}

			if (hdr.update_map) {
				for (i = 0; i < Constants.MB_FEATURE_TREE_PROBS; i++)
					hdr.tree_probs[i] = bool.bool_get_bit() > 0 ? bool.bool_get_uint(8) : 255;
			}
		} else {
			hdr.update_map = false;
			hdr.update_data = false;
		}
	}
	
	public void
	decode_loopfilter_header(Context ctx,
	                         BoolDecoder bool,
	                         LoopfilterHdr hdr) throws IOException
	{
	    if (ctx.frame_hdr.is_keyframe){
	    	hdr = new LoopfilterHdr();
	    	ctx.loopfilter_hdr = hdr;
//	    	memset(hdr, 0, sizeof(*hdr));
	    }
	        

	    hdr.use_simple    = bool.bool_get_bit()>0;
	    hdr.level         = bool.bool_get_uint(6);
	    hdr.sharpness     = bool.bool_get_uint(3);
	    hdr.delta_enabled = bool.bool_get_bit()>0;

	    if (hdr.delta_enabled && bool.bool_get_bit()>0)
	    {
	        int i;

	        for (i = 0; i < Constants.BLOCK_CONTEXTS; i++)
	            hdr.ref_delta[i] = bool.bool_maybe_get_int(6);

	        for (i = 0; i < Constants.BLOCK_CONTEXTS; i++)
	            hdr.mode_delta[i] = bool.bool_maybe_get_int(6);
	    }
	}
	
	public void decode_and_init_token_partitions(Context ctx, BoolDecoder bool, FileInputStream data, int sz,
			TokenHdr hdr) throws IOException {
		int i;

		hdr.partitions = 1 << bool.bool_get_uint(2);

		if (sz < 3 * (hdr.partitions - 1))
			System.err.println("Truncated packet found parsing partition lengths.");

		sz -= 3 * (hdr.partitions - 1);

		for (i = 0; i < hdr.partitions; i++) {
			if (i < hdr.partitions - 1) {
				hdr.partition_sz[i] = data.read() | (data.read() << 8) | (data.read() << 16);
				// data += 3;
			} else
				hdr.partition_sz[i] = sz;

			if (sz < hdr.partition_sz[i])
				System.err.printf("Truncated partition %d", i);

			sz -= hdr.partition_sz[i];
		}

		for (i = 0; i < ctx.token_hdr.partitions; i++) {
			FileInputStream token_data_part = new FileInputStream(ivf_data);
			token_data_part.getChannel().position(data.getChannel().position());
			ctx.tokens[i].bool.init_bool_decoder(token_data_part, ctx.token_hdr.partition_sz[i]);
			data.skip(ctx.token_hdr.partition_sz[i]);
		}
		data.reset();
	}
	
	public void decode_quantizer_header(Context ctx, BoolDecoder bool, QuantHdr hdr) throws IOException {
		boolean update;
		int last_q = hdr.q_index;

		hdr.q_index = bool.bool_get_uint(7);
		update = last_q != hdr.q_index;
		update |= (hdr.y1_dc_delta_q = bool.bool_maybe_get_int(4)) > 0;
		update |= (hdr.y2_dc_delta_q = bool.bool_maybe_get_int(4)) > 0;
		update |= (hdr.y2_ac_delta_q = bool.bool_maybe_get_int(4)) > 0;
		update |= (hdr.uv_dc_delta_q = bool.bool_maybe_get_int(4)) > 0;
		update |= (hdr.uv_ac_delta_q = bool.bool_maybe_get_int(4)) > 0;
		hdr.delta_update = update;
	}
	
	public void decode_reference_header(Context ctx, BoolDecoder bool, ReferenceHdr hdr) throws IOException {
		boolean key = ctx.frame_hdr.is_keyframe;

		hdr.refresh_gf = key ? true : bool.bool_get_bit() > 0;
		hdr.refresh_arf = key ? true : bool.bool_get_bit() > 0;
		hdr.copy_gf = key ? false : !hdr.refresh_gf ? bool.bool_get_uint(2) > 0 : false;
		hdr.copy_arf = key ? false : !hdr.refresh_arf ? bool.bool_get_uint(2) > 0 : false;
		hdr.sign_bias[Constants.GOLDEN_FRAME] = key ? false : bool.bool_get_bit() > 0;
		hdr.sign_bias[Constants.ALTREF_FRAME] = key ? false : bool.bool_get_bit() > 0;
		hdr.refresh_entropy = bool.bool_get_bit() > 0;
		hdr.refresh_last = key ? true : bool.bool_get_bit() > 0;
	}
	
	public void decode_entropy_header( Context    ctx, BoolDecoder       bool, EntropyHdr hdr) throws IOException {
	    int i, j, k, l;

	    /* Read coefficient probability updates */
	    for (i = 0; i < Constants.BLOCK_TYPES; i++)
	        for (j = 0; j < Constants.COEF_BANDS; j++)
	            for (k = 0; k < Constants.PREV_COEF_CONTEXTS; k++)
	                for (l = 0; l < Constants.ENTROPY_NODES; l++)
	                    if (bool.bool_get(Constants.k_coeff_entropy_update_probs[i][j][k][l])>0)
	                        hdr.coeff_probs[i][j][k][l] = bool.bool_get_uint(8);

	    /* Read coefficient skip mode probability */
	    hdr.coeff_skip_enabled = bool.bool_get_bit()>0;

	    if (hdr.coeff_skip_enabled)
	        hdr.coeff_skip_prob = bool.bool_get_uint(8);

	    /* Parse interframe probability updates */
	    if (!ctx.frame_hdr.is_keyframe)
	    {
	        hdr.prob_inter = bool.bool_get_uint(8);
	        hdr.prob_last  = bool.bool_get_uint(8);
	        hdr.prob_gf    = bool.bool_get_uint(8);

	        if (bool.bool_get_bit()>0)
	            for (i = 0; i < 4; i++)
	                hdr.y_mode_probs[i] = bool.bool_get_uint(8);

	        if (bool.bool_get_bit()>0)
	            for (i = 0; i < 3; i++)
	                hdr.uv_mode_probs[i] = bool.bool_get_uint(8);

	        for (i = 0; i < 2; i++)
	            for (j = 0; j < Constants.MV_PROB_CNT; j++)
	                if (bool.bool_get(Constants.k_mv_entropy_update_probs[i][j])>0){
	                    int x = bool.bool_get_uint(7);
	                    hdr.mv_probs[i][j] = x>0 ? x << 1 : 1;
	                }
	    }
	}
	
	public void vp8_dixie_modemv_init(Context ctx)
	{
	    int    mbi_w, mbi_h, i;
	    MbInfo[] mbi;

	    mbi_w = ctx.mb_cols + 1; /* For left border col */
	    mbi_h = ctx.mb_rows + 1; /* For above border row */

	    if (ctx.frame_hdr.frame_size_updated){	// I don't support this
	    	System.out.println("Frame size updated is unsupported.");
//	        free(ctx.mb_info_storage);
//	        ctx.mb_info_storage = NULL;
//	        free(ctx.mb_info_rows_storage);
//	        ctx.mb_info_rows_storage = NULL;
	    }

	    if (ctx.mb_info_storage==null)
	        ctx.mb_info_storage = new MbInfo[mbi_w * mbi_h];

	    //we only generate one 1d array of Macroblock info storage
//	    if (ctx.mb_info_rows_storage == null)
//	        ctx.mb_info_rows_storage = new MbInfo[mbi_h][mbi_w];
//
//	    /* Set up row pointers */
//	    mbi = ctx.mb_info_storage[1];
//
//	    for (i = 0; i < mbi_h; i++)
//	    {
//	        ctx.mb_info_rows_storage[i] = mbi;
//	        mbi += mbi_w;
//	    }
//
//	    ctx.mb_info_rows = ctx.mb_info_rows_storage + 1;
	}
	
	public  void
	vp8_dixie_tokens_init(Context ctx)
	{
	    int  partitions = ctx.token_hdr.partitions;

	    if (ctx.frame_hdr.frame_size_updated)
	    {
	         int i;
	        int coeff_row_sz = ctx.mb_cols * 25 * 16;

	        for (i = 0; i < partitions; i++)
	        {
//	            free(ctx->tokens[i].coeffs);
	            ctx.tokens[i].coeffs = new short[coeff_row_sz];//memalign(16, coeff_row_sz);

//	            if (!ctx->tokens[i].coeffs)
//	                vpx_internal_error(&ctx->error, VPX_CODEC_MEM_ERROR,
//	                                   NULL);
	        }

//	        free(ctx->above_token_entropy_ctx);
	        ctx.above_token_entropy_ctx = new int[ctx.mb_cols*(4 + 2 + 2 + 1)];
//	            calloc(ctx->mb_cols, sizeof(*ctx->above_token_entropy_ctx));

//	        if (!ctx->above_token_entropy_ctx)
//	            vpx_internal_error(&ctx->error, VPX_CODEC_MEM_ERROR, NULL);
	    }
	}
	
	public void
	vp8_dixie_predict_init(Context ctx)
	{

	    int i;
	    char[] this_frame_base;

	    if (ctx.frame_hdr.frame_size_updated)
	    {
	        for (i = 0; i < Constants.NUM_REF_FRAMES; i++)
	        {
	            int w = ctx.mb_cols * 16 + Constants.BORDER_PIXELS * 2;
	            int h = ctx.mb_rows * 16 + Constants.BORDER_PIXELS * 2;

//	            vpx_img_free(&ctx->frame_strg[i].img);
//	            ctx->frame_strg[i].ref_cnt = 0;
	            ctx.ref_frames[i] = null;
//
//	            if (!vpx_img_alloc(&ctx->frame_strg[i].img,
//	                               IMG_FMT_I420, w, h, 16))
//	                vpx_internal_error(&ctx->error, VPX_CODEC_MEM_ERROR,
//	                                   "Failed to allocate %dx%d"
//	                                   " framebuffer",
//	                                   w, h);
//
//	            vpx_img_set_rect(&ctx->frame_strg[i].img,
//	                             BORDER_PIXELS, BORDER_PIXELS,
//	                             ctx->frame_hdr.kf.w, ctx->frame_hdr.kf.h);
	            ctx.frame_strg[i].data = new int[w*h];
	            ctx.frame_strg[i].w = w;
	            ctx.frame_strg[i].h = h;
	            ctx.frame_strg[i].ref_cnt = 0;
	            
	        }

	        if (ctx.frame_hdr.version>0)
	            ctx.subpixel_filters = Constants.bilinear_filters;
	        else
	            ctx.subpixel_filters = Constants.sixtap_filters;
	    }

	    /* Find a free framebuffer to predict into */
	    if (ctx.ref_frames[Constants.CURRENT_FRAME]!=null)
	    	ctx.ref_frames[Constants.CURRENT_FRAME].releaseRefFrame();

	    ctx.ref_frames[Constants.CURRENT_FRAME] = findFreeRefFrame(ctx);
//	    this_frame_base = ctx->ref_frames[CURRENT_FRAME].img.img_data;

	    /* Calculate offsets to the other reference frames */
//	    for (i = 0; i < NUM_REF_FRAMES; i++)
//	    {
//	        struct ref_cnt_img  *ref = ctx->ref_frames[i];
//
//	        ctx->ref_frame_offsets[i] =
//	            ref ? ref->img.img_data - this_frame_base : 0;
//	    }

	    /* TODO: No need to do this on every frame... */ //<-- this todo is from dixie
	}
	
	public Image findFreeRefFrame(Context ctx){
		for (int i = 0; i < Constants.NUM_REF_FRAMES; i++)
	
        if (ctx.frame_strg[i].ref_cnt == 0)
        {
        	ctx.frame_strg[i].ref_cnt = 1;
            return ctx.frame_strg[i];
        }
		
		System.err.println("Couldnt find free ref frame.EXIT.");
		System.exit(-1);
		return null;
	}
	
	public void dequant_init(DequantFactors[] factors, SegmentHdr seg, QuantHdr quant_hdr) {
   int  q;
   DequantFactors dqf;

   for (int i = 0; i < (seg.enabled ? Constants.MAX_MB_SEGMENTS : 1); i++)
   {
	   dqf = factors[i];
       q = quant_hdr.q_index;

       if (seg.enabled)
           q = (!seg.abs) ? q + seg.quant_idx[i] : seg.quant_idx[i];

       if (dqf.quant_idx != q || quant_hdr.delta_update)
       {
    	   short y1_dc_delta_q = (short) ((q + quant_hdr.y1_dc_delta_q)<0 ? 0 : (q + quant_hdr.y1_dc_delta_q)>127 ? 127 : (q + quant_hdr.y1_dc_delta_q));
    	   short y1_ac_delta_q = (short) (q						     <0 ? 0 : q						    >127 ? 127 : q						  );
    	   short uv_dc_delta_q = (short) ((q + quant_hdr.uv_dc_delta_q)<0 ? 0 : (q + quant_hdr.uv_dc_delta_q)>127 ? 127 : (q + quant_hdr.uv_dc_delta_q));
    	   short uv_ac_delta_q = (short) ((q + quant_hdr.uv_ac_delta_q)<0 ? 0 : (q + quant_hdr.uv_ac_delta_q)>127 ? 127 : (q + quant_hdr.uv_ac_delta_q));
    	   short y2_dc_delta_q = (short) ((q + quant_hdr.y2_dc_delta_q)<0 ? 0 : (q + quant_hdr.y2_dc_delta_q)>127 ? 127 : (q + quant_hdr.y2_dc_delta_q));
    	   short y2_ac_delta_q = (short) ((q + quant_hdr.y2_ac_delta_q)<0 ? 0 : (q + quant_hdr.y2_ac_delta_q)>127 ? 127 : (q + quant_hdr.y2_ac_delta_q));
           dqf.factor[Constants.TOKEN_BLOCK_Y1][0] =
        		   Constants.dc_q_lookup[y1_dc_delta_q];
           dqf.factor[Constants.TOKEN_BLOCK_Y1][1] =
        		   Constants.ac_q_lookup[y1_ac_delta_q];
           dqf.factor[Constants.TOKEN_BLOCK_UV][0] =
        		   Constants.dc_q_lookup[uv_dc_delta_q];
           dqf.factor[Constants.TOKEN_BLOCK_UV][1] =
        		   Constants.ac_q_lookup[uv_ac_delta_q];
           dqf.factor[Constants.TOKEN_BLOCK_Y2][0] =
        		   (short) (Constants.dc_q_lookup[y2_dc_delta_q] * 2);
           dqf.factor[Constants.TOKEN_BLOCK_Y2][1] =
        		   (short) (Constants.ac_q_lookup[y2_ac_delta_q] * 155 / 100);

           if (dqf.factor[Constants.TOKEN_BLOCK_Y2][1] < 8)
               dqf.factor[Constants.TOKEN_BLOCK_Y2][1] = 8;

           if (dqf.factor[Constants.TOKEN_BLOCK_UV][0] > 132)
               dqf.factor[Constants.TOKEN_BLOCK_UV][0] = 132;

           dqf.quant_idx = q;
       }

       
   }
}
	
	public void vp8_dixie_modemv_process_row(Context ctx, BoolDecoder    bool, int row, int start_col, int  num_cols) throws IOException{
		
	    MbInfo       above, ths;
	    MvClampRect  bounds = new MvClampRect();

	   
	    /* Calculate the eighth-pel MV bounds using a 1 MB border. */
	    bounds.to_left   = -((start_col + 1) << 7);
	    bounds.to_right  = (ctx.mb_cols - start_col) << 7;
	    bounds.to_top    = -((row + 1) << 7);
	    bounds.to_bottom = (ctx.mb_rows - row) << 7;

	    for (int col = start_col; col < start_col + num_cols; col++)
	    {
	    	ths = ctx.mb_info_storage[row+ col] ;
	    	above = ctx.mb_info_storage[row - 1 + col] ;
	    	
	        if (ctx.segment_hdr.update_map)
	            ths.base.segment_id = bool.bool_get(ctx.segment_hdr.tree_probs[0])>0
	                    ? 2 + bool.bool_get( ctx.segment_hdr.tree_probs[2])
	                    : bool.bool_get(ctx.segment_hdr.tree_probs[1]);//(bool,  ctx.segment_hdr);

	        if (ctx.entropy_hdr.coeff_skip_enabled)
	            ths.base.skip_coeff = bool.bool_get(ctx.entropy_hdr.coeff_skip_prob);

	        if (ctx.frame_hdr.is_keyframe)
	        {
	            if (!ctx.segment_hdr.update_map)
	                ths.base.segment_id = 0;

	            decode_kf_mb_mode(ths, ctx.mb_info_storage[row+ col-1], above, bool);
	        }
	        else
	        {
//	            if (bool.bool_get(ctx.entropy_hdr.prob_inter))
//	                decode_mvs(ctx, ths, ths - 1, above, bounds, bool);
//	            else
//	                decode_intra_mb_mode(ths, ctx.entropy_hdr, bool);

//	            bounds.to_left -= 16 << 3;
//	            bounds.to_right -= 16 << 3;
	        }

	        /* Advance to next mb */
//	        ths++;
//	        above++;
	    }
	}
	
	public void
	decode_kf_mb_mode(MbInfo      ths,
	                  MbInfo      left,
	                  MbInfo      above,
	                  BoolDecoder bool) throws IOException
	{
	    int y_mode, uv_mode;

	    y_mode = bool.bool_read_tree(Constants.kf_y_mode_tree, Constants.kf_y_mode_probs);

	    if (y_mode == Constants.B_PRED)
	    {
	        

	        for (int i = 0; i < 16; i++)
	        {
	            int  a = above_block_mode(ths, above, i);
	            int l = left_block_mode(ths, left, i);
	            int b;

	            b = bool.bool_read_tree(Constants.b_mode_tree,Constants.kf_b_mode_probs[a][l]);
	            ths.modes[i] = b;
	        }
	    }

	    uv_mode = bool.bool_read_tree( Constants.uv_mode_tree, Constants.kf_uv_mode_probs);

	    ths.base.y_mode = y_mode;
	    ths.base.uv_mode = uv_mode;
	    ths.base.mv.raw = 0;
	    ths.base.ref_frame = 0;
	}

	public int above_block_mode(MbInfo ths, MbInfo above, int b) {
		if (b < 4) {
			switch (above.base.y_mode) {
			case Constants.DC_PRED:
				return Constants.B_DC_PRED;
			case Constants.V_PRED:
				return Constants.B_VE_PRED;
			case Constants.H_PRED:
				return Constants.B_HE_PRED;
			case Constants.TM_PRED:
				return Constants.B_TM_PRED;
			case Constants.B_PRED:
				return above.modes[b + 12];
			default:
				System.err.println("Illegal y Mode");
				System.exit(-1);

			}
		}

		return ths.modes[b - 4];
	}

	public int left_block_mode(MbInfo ths, MbInfo left, int b) {
		if ((b & 3) == 0) {
			switch (left.base.y_mode) {
			case Constants.DC_PRED:
				return Constants.B_DC_PRED;
			case Constants.V_PRED:
				return Constants.B_VE_PRED;
			case Constants.H_PRED:
				return Constants.B_HE_PRED;
			case Constants.TM_PRED:
				return Constants.B_TM_PRED;
			case Constants.B_PRED:
				return left.modes[b + 3];
			default:
				System.err.println("Illegal y Mode");
				System.exit(-1);
			}
		}

		return ths.modes[b - 1];
	}

	public void
	vp8_dixie_tokens_process_row(Context ctx,
	                             int            partition,
	                             int            row,
	                             int            start_col,
	                             int            num_cols)
	{
	    TokenDecoder tokens = ctx.tokens[partition];
	    short[] coeffs = tokens.coeffs ; //25 * 16 * start_col
	    int coeffsStart = 25*16*start_col;
	    int          col;
	    int[]  above = ctx.above_token_entropy_ctx;// + start_col;
	    int[] left = tokens.left_token_entropy_ctx;
	    MbInfo mbi = ctx.mb_info_storage[row + start_col];

	    if (row == 0)
	    	for(int zero = 0; zero<num_cols*9;zero++)
	    		above[zero+start_col] = 0;
//	        reset_above_context(above, num_cols);

	    if (start_col == 0)
	    	for(int zero = 0; zero<num_cols*9;zero++)
	    		left[zero] = 0;
//	        reset_row_context(left);

	    for (col = start_col; col < start_col + num_cols; col++)
	    {
	    	mbi = ctx.mb_info_storage[row + col];
	    	
	    	for(int zero = 0; zero<25*16;zero++)
	    		coeffs[coeffsStart+zero] = 0;
//	        memset(coeffs, 0, 25 * 16 * sizeof(short));

	        if (mbi.base.skip_coeff)
	        {
	        	/* Reset the macroblock context on the left and right. We have to
	             * preserve the context of the second order block if this mode
	             * would not have updated it.
	             */
//	            memset(left, 0, sizeof((*left)[0]) * 8);
//	            memset(above, 0, sizeof((*above)[0]) * 8);

	            if (mbi.base.y_mode != Constants.B_PRED && mbi.base.y_mode != Constants.SPLITMV)
	            {
	                (*left)[8] = 0;
	                (*above)[8] = 0;
	            }
//	            reset_mb_context(left, above, mbi->base.y_mode);
	            mbi.base.eob_mask = 0;
	        }
	        else
	        {
	            struct dequant_factors *dqf;

	            dqf = ctx->dequant_factors  + mbi->base.segment_id;
	            mbi->base.eob_mask =
	                decode_mb_tokens(&tokens->bool,
	                                 *left, *above,
	                                 coeffs,
	                                 mbi->base.y_mode,
	                                 ctx->entropy_hdr.coeff_probs,
	                                 dqf->factor);
	        }

	        above++;
//	        mbi++;
	        coeffsStart += 25 * 16;
	    }
	}
}
