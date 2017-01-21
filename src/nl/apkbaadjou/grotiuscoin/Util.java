package nl.apkbaadjou.grotiuscoin;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

/**
 * Bevat handige methoden.
 */
public class Util {
	
	public static String getSha256Hash(String value) {
		try {
			MessageDigest sha256digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = sha256digest.digest(value.getBytes());

			return DatatypeConverter.printHexBinary(hash);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return "";
		}
	}

}
