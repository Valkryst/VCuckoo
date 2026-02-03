import com.github.javafaker.Faker;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

public class ConditionalCuckooFilterTest {
	private final Faker faker = new Faker();

	private ConditionalCuckooFilter<String> filter;
	private String key;
	private String[] attributes;

	@BeforeEach
	public void before() {
		filter = new ConditionalCuckooFilter<>();
		key = faker.lorem().word();

		attributes = new String[faker.number().randomDigitNotZero()];
		for (int i = 0 ; i  < attributes.length ; i++) {
			attributes[i] = faker.lorem().word();
		}
	}

	@Test
	public void bigTest() {
		final int totalPairs = 50000;

		// The filter must be able to hold
		filter = new ConditionalCuckooFilter<>(totalPairs);

		final var keys = new String[totalPairs];
		final var attributeSets = new ArrayList<ArrayList<String>>();

		for (int i = 0 ; i < totalPairs ; i++) {
			// We have to use variable length sentences, or we're almost
			// guaranteed that the filter will run out of space for new keys.
			// Faker doesn't have a large enough set of words to generate 1000s
			// of unique keys.
			keys[i] = faker.lorem().sentence(faker.number().randomDigitNotZero());

			attributeSets.add(new ArrayList<>());
			for (int j = 0 ; j < faker.number().randomDigitNotZero() ; j++) {
				attributeSets.get(i).add(faker.lorem().word());
			}

			Assertions.assertTrue(filter.add(keys[i], attributeSets.get(i).toArray(new String[0])));
		}

		for (int i = 0 ; i < totalPairs ; i++) {
			Assertions.assertTrue(filter.hasKey(keys[i], attributeSets.get(i).toArray(new String[0])));
		}

		Assertions.assertTrue(true);
	}

	@Test
	public void canAddWithNonNullKey() {
		Assertions.assertTrue(filter.add(key, attributes));
		Assertions.assertTrue(filter.hasKey(key, attributes));
	}

	@Test
	public void cannotAddWithNullKey() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			filter.add(null, attributes);
		});
	}

	@Test
	public void cannotAddWithNullAttributes() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			filter.add(key, null);
		});
	}

	@Test
	public void canCheckHasKeyWhenKeyExists() {
		filter.add(key, attributes);
		Assertions.assertTrue(filter.hasKey(key, attributes));
	}

	@Test
	public void canCheckHasKeyWhenKeyDoesNotExist() {
		Assertions.assertFalse(filter.hasKey(key, attributes));
	}

	@Test
	public void canCheckHasKeyWhenAttributesDiffer() {
		filter.add(key, attributes);

		final var otherAttributes = new String[faker.number().randomDigitNotZero()];
		for (int i = 0 ; i < otherAttributes.length ; i++) {
			otherAttributes[i] = faker.lorem().word();
		}

		System.out.println(Arrays.toString(attributes));
		System.out.println(Arrays.toString(otherAttributes));

		Assertions.assertFalse(filter.hasKey(key, otherAttributes));
	}

	@Test
	public void cannotCheckHasKeyWithNullKey() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			filter.hasKey(null, attributes);
		});
	}

	@Test
	public void cannotCheckHasKeyWithNullAttributes() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			filter.hasKey(key, null);
		});
	}

	@Test
	public void canRemoveWithNonNullKey() {
		filter.add(key, attributes);

		Assertions.assertTrue(filter.remove(key, attributes));
		Assertions.assertFalse(filter.hasKey(key, attributes));
	}

	@Test
	public void canRemoveWhenKeyNotInTable() {
		Assertions.assertFalse(filter.remove(key, attributes));
	}

	@Test
	public void cannotRemoveWithNullKey() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			filter.remove(null, attributes);
		});
	}

	@Test
	public void cannotRemoveWithNullAttributes() {
		Assertions.assertThrows(NullPointerException.class, () -> {
			filter.remove(key, null);
		});
	}
}
