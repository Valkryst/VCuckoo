public class Hashing {
	public static long stringHash(String string) {
		//P will be incrementally be raised in power
		long pPowI = 1;

		//Return value
		long hash = 0;

		for(int i = 0; i < string.length(); i++) {
			//Character with lowest value that we're likely to use
			//Space has ASCII value 32 or x20
			int FIRST_CHAR = (' ');
			//Really big prime number
			int m = (int) (1e9 + 9);
			hash = (hash + (string.charAt(i) - FIRST_CHAR + 1) * pPowI) % m;
			//Prime number roughly equal to but larger than set of characters used
			int p = 131;
			pPowI = (pPowI * p) % m;
		}
		return hash;
	}

	public static byte fingerprintHash(byte input) {
		return (byte) Byte.hashCode(input);
	}

	public static void main(String[] args) {
		String testString = "Hello World";

		byte[] byteArray = testString.getBytes();
		byte fingerprint = byteArray[byteArray.length - 1];
		long first_bucket = stringHash(testString);
		//long second_bucket = fingerprintHash(Long.toBinaryString(first_bucket));
		long second_bucket = first_bucket ^ fingerprintHash(fingerprint);

		System.out.println("First bucket: " + first_bucket);
		System.out.println("Second bucket: " + second_bucket);

		if((second_bucket ^ fingerprintHash(fingerprint)) == first_bucket) {
			System.out.println("Bucket check passed");
		} else {
			System.out.println("Bucket check failed");
		}
	}
}