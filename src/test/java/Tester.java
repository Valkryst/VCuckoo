import com.github.javafaker.Faker;
import org.apache.commons.lang3.RandomStringUtils;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class Tester {
	private static final Faker faker = new Faker();

	private static CuckooFilter filter;
	private static String[] keys;

	public static void main(final String[] args) {
		final var results = new ArrayList<String>();
		results.add("# Keys,Filter Size,Accuracy,False Positive Rate,Fill Rate\n");

		try {
			for (int totalKeys = 100; totalKeys <= 1_000_000; totalKeys += 100) {
				regenerateFilter(totalKeys, 8);

				results.add(String.format(
					"%d,%d,%.2f,%.3f,%.3f\n",
					totalKeys,
					8,
					calculateAccuracy(),
					calculateFalsePositiveRate(totalKeys),
					filter.getFillRate()
				));
			}
		} catch(final RuntimeException ignored) {
		}

		try (
			final var fileWriter = new FileWriter("results.csv")
		) {
			for (final String result : results) {
				fileWriter.write(result);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private static void regenerateFilter(final int totalKeys, final int filterSize) {
		filter = new CuckooFilter(filterSize);

		keys = new String[totalKeys];
		for (int i = 0 ; i < totalKeys ; i++) {
			keys[i] = generateKey();

			if (!filter.add(keys[i])) {
				System.err.println("Failed to add key to filter with " + totalKeys + " keys, exiting.");
				throw new RuntimeException();
			}
		}
	}

	private static String generateKey() {
		/*
		 * Faker has a small set of words it can draw from. To help avoid
		 * hash collisions, we're generating strings of varying lengths as
		 * the keys.
		 */
		return RandomStringUtils.randomAlphanumeric(ThreadLocalRandom.current().nextInt(1, 500));
	}

	private static double calculateAccuracy() {
		int total = 0;

		for (int i = 0 ; i < keys.length ; i++) {
			if (filter.hasKey(keys[i])) {
				total++;
			}
		}

		// System.out.println(result + "% accuracy when calling hasKey for keys that were inserted into the filter.");
		return (double) total / keys.length;
	}

	private static double calculateFalsePositiveRate(final int totalTestCases) {
		int falsePositives = 0;

		for (int i = 0 ; i < totalTestCases ; i++) {
			final var tmp = generateKey();
			boolean fuckyes = true;

			for (final String existingKey : keys) {
				if (tmp.equals(existingKey)) {
					i++;
					fuckyes = false;
					break;
				}
			}

			if (fuckyes && filter.hasKey(tmp)) {
				falsePositives++;
			}
		}

		return (double) falsePositives / totalTestCases;
		//System.out.println(result + "% false positive rate when calling hashKey for keys that were not inserted into the filter.");
	}
}
