import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.NonNull;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.concurrent.ThreadLocalRandom;

public class ConditionalCuckooFilter<K> {
	private static final int MAX_KICKS = 2;
	private static final int MAX_TABLES = 2;

	private Entry<K, BloomFilter<CharSequence>>[][] tables;

	public ConditionalCuckooFilter() {
		this(1000);
	}

	public ConditionalCuckooFilter(final int maxTableElements) {
		tables = (Entry<K, BloomFilter<CharSequence>>[][]) Array.newInstance(Entry.class, MAX_TABLES, maxTableElements);
	}

	private int fingerprint(final K key) {
		/*
		 * The paper does not describe the implementation of this function.
		 * From my testing, it appears as if this can be based on the hashCode
		 * of the key, but we have to alter it in away that prevents
		 * hash(key) and hash(fingerprint(key)) from being equal.
		 *
		 * The following math has been randomly chosen and appears to work.
		 */
		return (int) (Math.floor(key.hashCode() * 2.2) / tables[0].length);
	}

	private int hash(final Object object) {
		return Math.abs(object.hashCode()) % tables[0].length;
	}

	private BloomFilter<CharSequence> attributeSketch(final String ... attributes) {
		final var filter = BloomFilter.create(
				Funnels.stringFunnel(Charset.defaultCharset()),
				attributes == null ? 0 : attributes.length
		);

		for (final var attribute : attributes) {
			filter.put(attribute);
		}

		return filter;
	}

	/**
	 * Adds an entry to the table.
	 *
	 * @param key Entry's key.
	 * @param attributes Entry's attributes.
	 * @return
	 * 		True if the entry was added and false if the entry could not be
	 * 		added.
	 */
	public boolean add(final @NonNull K key, final @NonNull String ... attributes) {
		int fingerprint = fingerprint(key);
		final int hashA = hash(key);
		final int hashB = hash(hashA ^ hash(fingerprint));
		var newEntry = new Entry<>(key, attributeSketch(attributes));

		if (tables[0][hashA] == null) {
			tables[0][hashA] = newEntry;
			//System.out.println("tables[0][" + hashA + "]");
			return true;
		}

		if (tables[1][hashB] == null) {
			tables[1][hashB] = newEntry;
			//System.out.println("tables[1][" + hashB + "]");
			return true;
		}

		// Both buckets are full. Randomly relocate existing items.
		//System.out.println("Fuck me, buckets be filled m8. Tryna add " + key + " with " + Arrays.toString(attributes) + " to tables[1][" + hashB + "]");
		int randomTable = Math.random() > 0.5 ? 0 : 1;
		for (int i = 0 ; i < MAX_KICKS ; i++) {
			int randomIndex = ThreadLocalRandom.current().nextInt(tables[randomTable].length);

			// Swap f and the fingerprint in the randomly selected entry.
			var temp = tables[randomTable][randomIndex];
			tables[randomTable][randomIndex] = newEntry;
			newEntry = temp;

			// Paper says to do randomTable = randomTable ^ hash(fingeprint),
			// but that throws an out of bounds exception.
			for (int j = 0 ; j < tables[randomTable].length ; j++) {
				if (tables[randomTable][j] == null) {
					tables[randomTable][j] = newEntry;
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
	 * @param attributes Attributes of the entry.
	 * @return Whether the entry is in the table.
	 */
	public boolean hasKey(final @NonNull K key, final @NonNull String ... attributes) {
		int hash = hash(key);
		if (tables[0][hash] != null) {
			final var bloomFilter = tables[0][hash].value();
			boolean allAttributesInBloomFilter = true;

			for (final var attribute : attributes) {
				if (!bloomFilter.mightContain(attribute)) {
					allAttributesInBloomFilter = false;
					break;
				}
			}

			if (allAttributesInBloomFilter) {
				//System.out.println("Found " + key + " in tables[0][" + hash + "]");
				return true;
			}
		}

		hash = hash(hash(hash) ^ hash(fingerprint(key)));
		if (tables[1][hash] != null) {
			final var bloomFilter = tables[1][hash].value();
			boolean allAttributesInBloomFilter = true;

			for (final var attribute : attributes) {
				if (!bloomFilter.mightContain(attribute)) {
					allAttributesInBloomFilter = false;
					break;
				}
			}

			if (allAttributesInBloomFilter) {
				//System.out.println("Found " + key + " in tables[1][" + hash + "]");
			}
			return allAttributesInBloomFilter;
		}

		return false;
	}

	/**
	 * Removes an entry from the table.
	 *
	 * @param key Key of the entry.
	 * @param attributes Attributes of the entry.
	 * @return Whether the entry was deleted.
	 */
	public boolean remove(final @NonNull K key, final @NonNull String ... attributes) {
		int hash = hash(key);
		if (tables[0][hash] != null) {
			final var bloomFilter = tables[0][hash].value();
			boolean allAttributesInBloomFilter = true;

			for (final var attribute : attributes) {
				if (!bloomFilter.mightContain(attribute)) {
					allAttributesInBloomFilter = false;
					break;
				}
			}

			if (allAttributesInBloomFilter) {
				tables[0][hash] = null;
				return true;
			}
		}

		hash = hash(hash) ^ hash(fingerprint(key));
		if (tables[1][hash] != null) {
			final var bloomFilter = tables[1][hash].value();
			boolean allAttributesInBloomFilter = true;

			for (final var attribute : attributes) {
				if (!bloomFilter.mightContain(attribute)) {
					allAttributesInBloomFilter = false;
					break;
				}
			}

			if (allAttributesInBloomFilter) {
				tables[1][hash] = null;
				return true;
			}
		}

		return false;
	}

	public void reportFillRates() {
		for (int i = 0 ; i < tables.length ; i++) {
			int filledIndices = 0;

			for (int j = 0 ; j < tables[i].length ; j++) {
				if (tables[i][j] != null) {
					filledIndices++;
				}
			}

			System.out.println("Table #" + i + " is " + (((double) filledIndices / tables[i].length) * 100) + "% full with " + filledIndices + "/" + tables[i].length + " indices filled.");
		}
	}
}
