import java.util.Iterator;
import java.util.List;


public class XORColorCombinationIterator  implements Iterator<Integer[]> {
	
	private List<Integer> colors;
	private int size;
	private int c1 = 0;
	private int c2 = 1;
	private int c3 = 2;
	private int c4 = 3;
	private boolean exhausted = false;
	private Integer[] result;
	
	public XORColorCombinationIterator(List<Integer> colorList) {
		this.colors = colorList;
		this.size = colorList.size();
	}
	
	private boolean nextColorCombination() {
		if (c4 < size-1) {
			c4++;
		} else {
			if (c3 < c4-1) {
				c3++;
				c4 = c3+1;
			} else {
				if (c2 < c3-1) {
					c2++;
					c3 = c2+1;
					c4 = c3+1;
				} else {
					if (c1 < c2-1) {
						c1++;
						c2 = c1+1;
						c3 = c2+1;
						c4 = c3+1;
					} else {
						// exhausted - no more color combinations
						return true;
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean hasNext() {
		while (!exhausted) {
			if ((colors.get(c1) ^ colors.get(c2) ^ colors.get(c3) ^ colors.get(c4)) == 0)
				return true;
			exhausted = nextColorCombination();
		}
		return false;
	}

	@Override
	public Integer[] next() {
		while (!exhausted) {
			if ((colors.get(c1) ^ colors.get(c2) ^ colors.get(c3) ^ colors.get(c4)) == 0) {
				result = new Integer[] {colors.get(c1), colors.get(c2), colors.get(c3), colors.get(c4)};
				exhausted = nextColorCombination();
				return result;
			}
			exhausted = nextColorCombination();
		}
		return null;
	}

	@Override
	public void remove() {
		if (result == null)
			return;
		
		for (int i = 3; i >= 0; i--) {
			colors.remove(result[i]);
		}
		result = null;
		
		if (colors.size() < 4) {
			exhausted = true;
		} else {
			c1 = 0;
			c2 = 1;
			c3 = 2;
			c4 = 3;
		}
	}
}

