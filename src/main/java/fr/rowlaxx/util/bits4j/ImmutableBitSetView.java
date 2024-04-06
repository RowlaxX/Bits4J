package fr.rowlaxx.util.bits4j;

import lombok.AllArgsConstructor;

@AllArgsConstructor
class ImmutableBitSetView implements ImmutableBitSet {
	private final BitSet bitmap;
	
	@Override
	public long bitCount() {
		return bitmap.bitCount();
	}

	@Override
	public boolean hasBit(long bit) {
		return bitmap.hasBit(bit);
	}

	@Override
	public boolean hasAllBits(long start, long end) {
		return bitmap.hasAllBits(start, end);
	}

	@Override
	public boolean hasAnyBits(long start, long end) {
		return bitmap.hasAnyBits(start, end);
	}

	@Override
	public long nextBit(long from, boolean present) {
		return bitmap.nextBit(from, present);
	}

	@Override
	public long previousBit(long from, boolean present) {
		return bitmap.previousBit(from, present);
	}
}