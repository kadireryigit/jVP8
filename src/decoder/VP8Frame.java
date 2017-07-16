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

	public void decodeFrame(Context ctx,
	             FileInputStream data,
	             int            sz) throws IOException
	{
//	    vpx_codec_err_t  res;
	    BoolDecoder  bool = new BoolDecoder();
	    int                  i, row, partition;

	    ctx.saved_entropy_valid = 0; 

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
	    if (ctx->frame_hdr.is_keyframe)
	    {
	        ARRAY_COPY(ctx->entropy_hdr.coeff_probs,
	                   k_default_coeff_probs);
	        ARRAY_COPY(ctx->entropy_hdr.mv_probs,
	                   k_default_mv_probs);
	        ARRAY_COPY(ctx->entropy_hdr.y_mode_probs,
	                   k_default_y_mode_probs);
	        ARRAY_COPY(ctx->entropy_hdr.uv_mode_probs,
	                   k_default_uv_mode_probs);
	    }
//
//	    if (!ctx->reference_hdr.refresh_entropy)
//	    {
//	        ctx->saved_entropy = ctx->entropy_hdr;
//	        ctx->saved_entropy_valid = 1;
//	    }
//
//	    decode_entropy_header(ctx, &bool, &ctx->entropy_hdr);
//
//	    vp8_dixie_modemv_init(ctx);
//	    vp8_dixie_tokens_init(ctx);
//	    vp8_dixie_predict_init(ctx);
//	    dequant_init(ctx->dequant_factors, &ctx->segment_hdr,
//	                 &ctx->quant_hdr);
//
//	    for (row = 0, partition = 0; row < ctx->mb_rows; row++)
//	    {
//	        vp8_dixie_modemv_process_row(ctx, &bool, row, 0, ctx->mb_cols);
//	        vp8_dixie_tokens_process_row(ctx, partition, row, 0,
//	                                     ctx->mb_cols);
//	        vp8_dixie_predict_process_row(ctx, row, 0, ctx->mb_cols);
//
//	        if (ctx->loopfilter_hdr.level && row)
//	            vp8_dixie_loopfilter_process_row(ctx, row - 1, 0,
//	                                             ctx->mb_cols);
//
//	        if (++partition == ctx->token_hdr.partitions)
//	            partition = 0;
//	    }
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
}
