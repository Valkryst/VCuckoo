import com.github.javafaker.Faker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CuckooHashtableTest {
	private final Faker faker = new Faker();

	private CuckooHashtable<String, Integer> table;
	private String key;
	private int value;

	@BeforeEach
	public void before() {
		table = new CuckooHashtable<>();
		key = faker.lorem().word();
		value = faker.number().randomDigit();
	}

	@Test
	public void bigTest() {
		final int totalPairs = 5_000;

		table = new CuckooHashtable<>();

		final var keys = new String[totalPairs];
		final var values = new Integer[totalPairs];

		for (int i = 0 ; i < totalPairs ; i++) {
			keys[i] = faker.lorem().sentence();
			values[i] = faker.number().randomDigit();
			table.add(keys[i], values[i]);
		}

		for (int i = 0 ; i < totalPairs ; i++) {
			Assertions.assertEquals(values[i], table.get(keys[i]).value());
		}
	}

	@Test
	public void canAddWithNonNullKeyAndValue() {
		Assertions.assertTrue(table.add(key, value));

		final var entry = table.get(key);
		Assertions.assertEquals(key, entry.key());
		Assertions.assertEquals(value, entry.value());
	}

	@Test
	public void canAddWithNonNullKeyAndNullValue() {
		Assertions.assertTrue(table.add(key, null));

		final var entry = table.get(key);
		Assertions.assertEquals(key, entry.key());
		Assertions.assertNull(entry.value());
	}

	@Test
	public void cannotAddWithNullKey() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			table.add(null, value);
		});
	}

	@Test
	public void canGetWhenKeyIsInTable() {
		Assertions.assertTrue(table.add(key, value));

		final var entry = table.get(key);
		Assertions.assertEquals(key, entry.key());
		Assertions.assertEquals(value, entry.value());
	}

	@Test
	public void canGetWhenKeyIsNotInTable() {
		Assertions.assertNull(table.get(key));
	}

	@Test
	public void cannotGetWithNullKey() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			table.get(null);
		});
	}

	@Test
	public void canRemoveWithNonNullKey() {
		table.add(key, value);

		final var entry = table.remove(key);
		Assertions.assertEquals(key, entry.key());
		Assertions.assertEquals(value, entry.value());

		Assertions.assertNull(table.get(key));
	}

	@Test
	public void canRemoveWhenKeyNotInTable() {
		Assertions.assertNull(table.remove(key));
	}

	@Test
	public void cannotRemoveWithNullKey() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			table.remove(null);
		});
	}
}
