import lombok.NonNull;

import java.lang.reflect.Array;
import java.util.BitSet;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class CuckooFilter {
	private static final int MAX_KICKS = 500;
	private static final int TOTAL_BUCKETS = (int) Math.pow(2, 12);

	private final Byte[][] tables;

	public CuckooFilter(final int initialElementsPerTable) {
		tables = (Byte[][]) Array.newInstance(Byte.class, TOTAL_BUCKETS, initialElementsPerTable);
	}

	/**
	 * Adds an entry to the table.
	 *
	 * @param key Entry's key.
	 * @return
	 * 		True if the entry was added and false if the entry could not be
	 * 		added.
	 */
	public boolean add(final @NonNull String key) {
		validateKey(key);

		byte fingerprint = generateFingerprint(key);
		final int bucketA = generateHash(key);
		final int bucketB = Math.toIntExact((bucketA ^ Byte.hashCode(fingerprint)) % TOTAL_BUCKETS);

		for (int i = 0 ; i < tables[bucketA].length ; i++) {
			if (tables[bucketA][i] == null) {
				tables[bucketA][i] = fingerprint;
				return true;
			}
		}

		for (int i = 0 ; i < tables[bucketB].length ; i++) {
			if (tables[bucketB][i] == null) {
				tables[bucketB][i] = fingerprint;
				return true;
			}
		}

		// Both buckets are full. Randomly relocate existing items.
		int randomTable = Math.random() > 0.5 ? bucketA : bucketB;
		for (int i = 0 ; i < MAX_KICKS ; i++) {
			int randomIndex = ThreadLocalRandom.current().nextInt(tables[randomTable].length);

			// Swap f and the fingerprint in the randomly selected entry.
			byte temp = tables[randomTable][randomIndex];
			tables[randomTable][randomIndex] = fingerprint;
			fingerprint = temp;

			i = i ^ fingerprint;

			for (int j = 0 ; j < tables[bucketA].length ; j++) {
				if (tables[bucketA][j] == null) {
					tables[bucketA][j] = fingerprint;
					return true;
				}
			}

			for (int j = 0 ; j < tables[bucketB].length ; j++) {
				if (tables[bucketB][j] == null) {
					tables[bucketB][j] = fingerprint;
					return true;
				}
			}
		}

		// Hashtable is considered full.
		return false;
	}

	/**
	 * Determines if an entry is in the table.
	 *
	 * @param key Key of the entry.
	 * @return Whether the entry is in the table.
	 */
	public boolean hasKey(final @NonNull String key) {
		validateKey(key);

		final byte fingerprint = generateFingerprint(key);
		final int bucketA = generateHash(key);
		final int bucketB = Math.toIntExact((bucketA ^ Byte.hashCode(fingerprint)) % TOTAL_BUCKETS);

		for (int i = 0 ; i < tables[bucketA].length ; i++) {
			if (tables[bucketA][i] == null) {
				continue;
			}

			if (tables[bucketA][i] == fingerprint) {
				return true;
			}
		}

		for (int i = 0 ; i < tables[bucketB].length ; i++) {
			if (tables[bucketB][i] == null) {
				continue;
			}

			if (tables[bucketB][i] == fingerprint) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Removes an entry from the table.
	 *
	 * @param key Key of the entry.
	 * @return Whether the entry was deleted.
	 */
	public boolean remove(final @NonNull String key) {
		validateKey(key);

		final byte fingerprint = generateFingerprint(key);
		final int bucketA = generateHash(key);
		final int bucketB = Math.toIntExact((bucketA ^ Byte.hashCode(fingerprint)) % TOTAL_BUCKETS);

		for (int i = 0 ; i < tables[bucketA].length ; i++) {
			if (tables[bucketA][i] == null) {
				continue;
			}

			if (tables[bucketA][i] == fingerprint) {
				tables[bucketA][i] = null;
				return true;
			}
		}

		for (int i = 0 ; i < tables[bucketA].length ; i++) {
			if (tables[bucketB][i] == null) {
				continue;
			}

			if (tables[bucketB][i] == fingerprint) {
				tables[bucketB][i] = null;
				return true;
			}
		}

		return false;
	}


	private byte generateFingerprint(final String string) {
		final var bytes = string.getBytes();
		return (byte) Math.abs(bytes[bytes.length - 1]);
	}

	private int generateHash(final String string) {
		//P will be incrementally be raised in power
		long pPowI = 1;

		long hash = 0;

		for(int i = 0; i < string.length(); i++) {
			//Character with lowest value that we're likely to use
			//Space has ASCII value 32 or x20
			int FIRST_CHAR = ' ';
			//Really big prime number
			int m = (int) (1e9 + 9);
			hash = (hash + (string.charAt(i) - FIRST_CHAR + 1) * pPowI) % m;
			//Prime number roughly equal to but larger than set of characters used
			int p = 131;
			pPowI = (pPowI * p) % m;
		}

		return (int) (hash % TOTAL_BUCKETS);
	}

	private void validateKey(final String key) {
		Objects.requireNonNull(key);

		if (key.isEmpty()) {
			throw new IllegalArgumentException("A key cannot be empty.");
		}
	}

	public double getFillRate() {
		long nonNullElements = 0;

		for (int i = 0 ; i < tables.length ; i++) {
			for (int j = 0 ; j < tables[i].length ; j++) {
				if (tables[i][j] != null) {
					nonNullElements++;
				}
			}
		}

		return (double) nonNullElements / (tables.length * tables[0].length);
	}
}
