package decoder;

public class EntropyHdr {

	coeff_probs_table_t   coeff_probs;
    mv_component_probs_t  mv_probs[2];
    unsigned int          coeff_skip_enabled;
    unsigned char         coeff_skip_prob;
    unsigned char         y_mode_probs[4];
    unsigned char         uv_mode_probs[3];
    unsigned char         prob_inter;
    unsigned char         prob_last;
    unsigned char         prob_gf;
}
