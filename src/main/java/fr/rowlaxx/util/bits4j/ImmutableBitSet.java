package fr.rowlaxx.util.bits4j;

public interface ImmutableBitSet {

	public long bitCount();
	
	public boolean hasBit(long bit);
	public boolean hasAllBits(long start, long end);
	public boolean hasAnyBits(long start, long end);
	
	public long nextBit(long from, boolean present);
	public long previousBit(long from, boolean present);

}