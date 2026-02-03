import lombok.NonNull;

import java.lang.reflect.Array;

public class CuckooHashtable<K, V> {
	private static final int MAX_TABLES = 2;

	private Entry<K, V>[][] tables;

	public CuckooHashtable() {
		this(1000);
	}

	public CuckooHashtable(final int initialElementsPerTable) {
		tables = (Entry<K, V>[][]) Array.newInstance(Entry.class, MAX_TABLES, initialElementsPerTable);
	}

	private int hash(final int function, final K key) {
		final int keyHashCode = Math.abs(key.hashCode());

		switch (function) {
			case 0: {
				return keyHashCode % tables[0].length;
			}
			case 1: {
				return (keyHashCode / tables[0].length) % tables[0].length;
			}
			default: {
				throw new IllegalArgumentException(function + " is not a valid hash function.");
			}
		}
	}

	/**
	 * Adds an entry to the table.
	 *
	 * @param key Entry's key.
	 * @param value Entry's value.
	 * @return
	 * 		True if the entry was added and false if the entry could not be
	 * 		added.
	 */
	public boolean add(final @NonNull K key, V value) {
		if (get(key) != null) {
			return true;
		}

		return add(key, value, 0);
	}

	private boolean add(final @NonNull K key, final V value, final int tableIndex) {
		if (tableIndex >= tables.length) {
			lengthenTables();
			return add(key, value, 0);
		}

		final int hash = hash(tableIndex, key);
		final var entry = tables[tableIndex][hash];

		tables[tableIndex][hash] = new Entry<>(key, value);

		if (entry == null) {
			return true;
		} else {
			return add(entry.key(), entry.value(), tableIndex + 1);
		}
	}

	/**
	 * Retrieves an entry from the table.
	 *
	 * @param key Key of the entry to search for.
	 * @return The entry, or null if no entry was found.
	 */
	public Entry<K, V> get(final @NonNull K key) {
		for (int i = 0 ; i < tables.length ; i++) {
			final var entry = tables[i][hash(i, key)];

			if (entry == null) {
				break;
			}

			if (key.equals(entry.key())) {
				return entry;
			}
		}

		return null;
	}

	private void lengthenTables() {
		final int newLength = (int) Math.floor(tables[0].length * 1.1);

		final var oldTables = tables;
		tables = (Entry<K, V>[][]) Array.newInstance(Entry.class, MAX_TABLES, newLength);

		for (int i = 0 ; i < oldTables.length ; i++) {
			for (int j = 0 ; j < oldTables[i].length ; j++) {
				final var entry = oldTables[i][j];

				if (entry != null) {
					add(entry.key(), entry.value());
				}
			}
		}
	}

	/**
	 * Removes an entry from the table.
	 *
	 * @param key Key of the entry to remove.
	 * @return The deleted entry, or null if no entry was found.
	 */
	public Entry<K, V> remove(final @NonNull K key) {
		for (int i = 0 ; i < tables.length ; i++) {
			final int hash = hash(i, key);
			final var entry = tables[i][hash];

			if (entry == null) {
				break;
			}

			if (key.equals(entry.key())) {
				tables[i][hash] = null;
				return entry;
			}
		}

		return null;
	}
}
