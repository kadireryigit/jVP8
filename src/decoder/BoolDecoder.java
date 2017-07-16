package decoder;

import java.io.FileInputStream;
import java.io.IOException;

public class BoolDecoder {

	private FileInputStream input; /* next compressed data byte */
	private int inputLen; /* length of the input buffer */
	private int range; /* identical to encoder's range */
	private int value; /* contains at least 8 significant bits */
	private int bitCount; /* # of bits shifted out of value, max 7 */

	public BoolDecoder() {

	}

	public void init_bool_decoder(FileInputStream input, int sz) throws IOException {
		if (sz >= 2) {
			value = (input.read() << 8) | input.read();/* first 2 input bytes */
			inputLen = sz - 2;
		} else {
			value = 0;
			input = null;
			inputLen = 0;
		}

		range = 255; /* initial range is full */
		bitCount = 0; /* have not yet shifted out any bits */
	}

	public int bool_get(int probability) throws IOException {
		/*
		 * range and split are identical to the corresponding values used by the
		 * encoder when this bool was written
		 */

		int split = 1 + (((range - 1) * probability) >> 8);
		int SPLIT = split << 8;
		int retval; /* will be 0 or 1 */

		if (value >= SPLIT) /* encoded a one */
		{
			retval = 1;
			range -= split; /* reduce range */
			value -= SPLIT; /* subtract off left endpoint of interval */
		} else /* encoded a zero */
		{
			retval = 0;
			range = split; /* reduce range, no change in left endpoint */
		}

		while (range < 128) /* shift out irrelevant value bits */
		{
			value <<= 1;
			range <<= 1;

			if (++bitCount == 8) /* shift in new bits 8 at a time */
			{
				bitCount = 0;

				if (inputLen > 0) {
					value |= input.read();
					inputLen--;
				}
			}
		}

		return retval;
	}

	public int bool_get_bit() throws IOException {
		return bool_get(128);
	}

	public int bool_get_uint(int bits) throws IOException {
		int z = 0;
		int bit;

		for (bit = bits - 1; bit >= 0; bit--) {
			z |= (bool_get_bit() << bit);
		}

		return z;
	}

	public int bool_get_int(int bits) throws IOException {
		int z = 0;
		int bit;

		for (bit = bits - 1; bit >= 0; bit--) {
			z |= (bool_get_bit() << bit);
		}

		return bool_get_bit() > 0 ? -z : z;
	}

	public int bool_maybe_get_int(int bits) throws IOException {
		return bool_get_bit() > 0 ? bool_get_int(bits) : 0;
	}

	public int bool_read_tree(int[] t, int[] p) throws IOException {
		int i = 0;

		while ((i = t[i + bool_get(p[i >> 1])]) > 0)
			;

		return -i;
	}

}
