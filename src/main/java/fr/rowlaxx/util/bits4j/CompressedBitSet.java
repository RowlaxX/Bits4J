package fr.rowlaxx.util.bits4j;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import fr.rowlaxx.source.Exclusive;
import fr.rowlaxx.source.Inclusive;
import fr.rowlaxx.source.Verified;

public class CompressedBitSet implements BitSet, Externalizable {
	private class Segment {
		private Segment previous = null;
		private Segment next = null;
		private @Inclusive long startsAt;
		private boolean enabled;
		
		private Segment() {
			this.startsAt = 0;
			this.enabled = false;
			this.previous = null;
			this.next = null;
		}
		
		private Segment(long startsAt, boolean enabled, Segment previous, Segment next) {
			this.startsAt = startsAt;
			this.enabled = enabled;
			this.previous = previous;
			this.next = next;
		}
		
		@Verified
		private Segment flipLeft(@Exclusive long endsAt) {
			if (previous == null) {
				previous = new Segment(this.startsAt, !enabled, null, this);
				first = previous;
			}
			
			this.startsAt = endsAt;
			return previous;
		}
		
		@Verified
		private Segment flipRight(@Inclusive long startsAt) {
			if (next == null) {
				next = new Segment(startsAt, !enabled, this, null);
				last = next;
			}
			else
				next.startsAt = startsAt;
			
			return next;
		}
		
		private Segment flipSegment() {
			if (next == null && previous == null) {
				enabled = !enabled;
				return this;
			}
			else if (next == null) {
				previous.next = null;
				last = previous;
				return previous;
			}
			else if (previous == null) {
				next.previous = null;
				first = next;
				return next;
			}
			else {
				previous.next = next.next;
				next.next.previous = previous;
				return previous;
			}
		}
		
		@Verified
		private Segment flipMiddle(@Inclusive long startsAt, @Exclusive long endsAt) {
			var middle = new Segment(startsAt, !enabled, this, null);
			middle.next = new Segment(endsAt, enabled, middle, next);
			
			if (next == null)
				last = middle.next;
			else
				next.previous = middle.next;

			next = middle;
			return middle;
		}
		
		@Verified
		private Segment flip(@Inclusive long startsAt, @Exclusive long endsAt) {
			var instanceEndsAt = endsAt();
			var instanceStartsAt = this.startsAt;
			
			if (startsAt == instanceStartsAt) {
				if (endsAt < instanceEndsAt)
					return flipLeft(endsAt);
				else
					return flipSegment();
			}
			else {
				if (endsAt < instanceEndsAt)
					return flipMiddle(startsAt, endsAt);
				else
					return flipRight(startsAt);
			}
		}
		
		@Verified
		private void flipAll(@Inclusive long startsAt, @Exclusive long endsAt) {
			var seg = this;
			var tmp = 0l;
			
			do {
				tmp = seg.endsAt();
				seg = flip(startsAt, endsAt);
				startsAt = tmp;
			} while (seg.endsAt() < endsAt);
		}
		
		@Exclusive
		private long endsAt() {
			return next == null ? capacity : next.startsAt;
		}
		
		private long length() {
			return endsAt() - startsAt;
		}
		
		@Verified
		private void setAll(@Inclusive long startsAt, @Exclusive long endsAt, boolean enabled) {
			var seg = this;

			do {
				if (seg.enabled != enabled)
					seg = flip(startsAt, endsAt);
				
				seg = seg.next;
				startsAt = seg.startsAt;
			} while (seg.endsAt() < endsAt);
		}
		
		@Verified
		private Segment getNextSegment(long index) {
			var seg = this;
			while (index >= seg.endsAt())
				seg = seg.next;
			return seg;
		}
	}
	
	private long capacity;
	private Segment first;
	private Segment last;
	
	public CompressedBitSet() {
		this(Long.MAX_VALUE);
	}
	
	public CompressedBitSet(long capacity) {
		if (capacity <= 0)
			throw new IllegalArgumentException("capacity must be greater than 0 : " + capacity);
		this.capacity = capacity;
		this.first = new Segment();
		this.last = first;
	}
	
	@Verified
	private Segment getSegment(long index) {
		if (2*index <= last.startsAt) {
			var seg = first;
			while (index >= seg.endsAt())
				seg = seg.next;
			return seg;
		}
		else {
			var seg = last;
			while (index < seg.startsAt)
				seg = seg.previous;
			return seg;
		}
	}
	
	private void checkIndex(long bit) {
		if (bit < 0 || bit >= capacity)
			throw new IndexOutOfBoundsException("bit must be between 0 and " + capacity + " : " + bit);
	}
	
	private void checkRange(@Inclusive long startsAt, @Exclusive long endsAt) {
		if (startsAt < 0)
			throw new IndexOutOfBoundsException("startsAt must be positive : " + startsAt);
		if (endsAt <= startsAt)
			throw new IndexOutOfBoundsException("endsAt must be greater than startsAt : " + endsAt + " <= " + startsAt);
		if (endsAt >= capacity)
			throw new IndexOutOfBoundsException("endsAt must be less than capacity : " + endsAt + " >= " + capacity);
	}
	
	@Override
	public long bitCount() {
		var count = 0l;
		var seg = first;
		while (seg != null) {
			if (seg.enabled)
				count += seg.length();
			seg = seg.next;
		}
		return count;
	}

	@Override
	public boolean hasBit(long index) {
		checkIndex(index);
		return getSegment(index).enabled;
	}

	@Override
	public boolean hasAllBits(long startsAt, long endsAt) {
		checkRange(startsAt, endsAt);
		var s1 = getSegment(startsAt);
		var s2 = s1.getNextSegment(endsAt - 1);
		return s1 == s2;
	}

	@Override
	public boolean hasAnyBits(long startsAt, long endsAt) {
		checkRange(startsAt, endsAt);
		var s1 = getSegment(startsAt);
		var s2 = s1.getNextSegment(endsAt - 1);
		if (s1 != s2)
			return true;
		return s1.enabled;
	}

	@Override
	public long nextBit(long from, boolean enabled) {
		checkIndex(from);
		var seg = getSegment(from);
		if (seg.enabled == enabled)
			return from;
		return seg.next == null ? -1 : seg.next.startsAt;
	}

	@Override
	public long previousBit(long from, boolean enabled) {
		checkIndex(from);
		var seg = getSegment(from);
		if (seg.enabled == enabled)
			return from;
		return seg.previous == null ? -1 : seg.startsAt - 1; //seg.startsAt ~ seg.previous.endsAt()
	}

	@Override
	public void setBit(long index, boolean enabled) {
		checkIndex(index);
		getSegment(index).setAll(index, index + 1, enabled);
	}

	@Override
	public void setAllBits(long startsAt, long endsAt, boolean enabled) {
		checkRange(startsAt, endsAt);
		getSegment(startsAt).setAll(startsAt, endsAt, enabled);
	}

	@Override
	public void putBit(long index) {
		setBit(index, true);
	}

	@Override
	public void putAllBits(long startsAt, long endsAt) {
		setAllBits(startsAt, endsAt, true);
	}

	@Override
	public void removeBit(long index) {
		setBit(index, false);
	}

	@Override
	public void removeAllBits(long startsAt, long endsAt) {
		setAllBits(startsAt, endsAt, false);
	}

	@Override
	public void flipBit(long index) {
		checkIndex(index);
		getSegment(index).flip(index, index + 1);
	}

	@Override
	public void flipAllBits(long startsAt, long endsAt) {
		checkRange(startsAt, endsAt);
		getSegment(startsAt).flipAll(startsAt, endsAt);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		var seg = this.first;
		
		out.writeLong(capacity);
		out.writeBoolean(seg.enabled);
		
		while ( (seg = seg.next) != null) {
			out.writeBoolean(true);
			out.writeLong(seg.startsAt);
			seg = seg.next;
		}
		out.writeBoolean(false);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException {
		this.capacity = in.readLong();
		this.first = new Segment();
		this.first.enabled = in.readBoolean();
		this.last = first;
		
		var seg = first;
		while ( in.readBoolean() )
			seg = seg.flipRight(in.readLong());
	}
}
