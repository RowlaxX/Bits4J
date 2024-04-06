package fr.rowlaxx.util.bits4j;

public interface BitSet extends ImmutableBitSet {
	public static ImmutableBitSet getImmutableView(BitSet bitmap) {
		return new ImmutableBitSetView(bitmap);
	}
	
	public void setBit(long index, boolean enabled);
	public void setAllBits(long startsAt, long endsAt, boolean enabled);
	
	public void putBit(long index);
	public void putAllBits(long startsAt, long endsAt);
	
	public void removeBit(long index);
	public void removeAllBits(long startsAt, long endsAt);
	
	public void flipBit(long index);
	public void flipAllBits(long startsAt, long endsAt);
	
	default ImmutableBitSet getImmutableView() {
		return getImmutableView(this);
	}
	
}
