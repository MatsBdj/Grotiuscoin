package nl.apkbaadjou.grotiuscoin;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * De Wallet-klasse is verantwoordelijk voor het beheren van de privésleutel en de 
 * publieke sleutel van de gebruiker.
 *
 */
public class Wallet {
	
	/**
	 * Pad naar het bestand waarin de sleutels worden opgeslagen.
	 */
	public static final String KEY_PATH = Main.DIRECTORY + File.separator + "sleutels";

	private String privesleutel;
	private String publiekeSleutel;
	
	private PrivateKey privateKey;
	
	/**
	 * Initialiseer de wallet.
	 */
	public void initWallet() {
		
		//maak een directory aan voor de sleutels (als dat niet al eerder is gedaan)
		File file = new File(Main.DIRECTORY);
		if (!file.exists()) {
			file.mkdir();
		}
		
		//maak een bestand aan voor de sleutels (als dat nog niet eerder is gedaan)
		file = new File(KEY_PATH);
		FileWriter writer = null;
		if (!file.exists()) {
			try {
				//genereer sleutelpaar
				KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
				SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
				keyGen.initialize(256, random);	
				KeyPair pair = keyGen.generateKeyPair();
					
				String priv = new BigInteger(pair.getPrivate().getEncoded()).toString(16);
				String pub = new BigInteger(pair.getPublic().getEncoded()).toString(16);
					
				writer = new FileWriter(file);
				writer.write(priv + BlockchainManager.NEWLINE +
							 pub);
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (writer != null) {
						writer.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		//laad de sleutels
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(KEY_PATH));
			privesleutel = reader.readLine();
			publiekeSleutel = reader.readLine();
		
			KeyFactory keyFactory = KeyFactory.getInstance("EC");
			EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(new BigInteger(privesleutel, 16).toByteArray());
			privateKey = keyFactory.generatePrivate(privateKeySpec);			
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null) {
					reader.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}	
	}
	
	/**
	 * Voeg een handtekening toe aan alle invoeren van de gegeven transactie.
	 * @param transactie	Transactie die ondertekend moet worden.
	 * @return De getekende transactie.
	 */
	public Transactie ondertekenTransactie(Transactie transactie) {
		try {
		
			//bereken de handtekening van de hash van de transactie
			Signature sig = Signature.getInstance("SHA256withECDSA");
			sig.initSign(privateKey);
			sig.update(transactie.getHash().getBytes("UTF-8"));
			String handtekening = new BigInteger(sig.sign()).toString(16);
			
			//voeg de handtekening toe aan alle invoeren van de transactie
			for (int i=0; i<transactie.getAantalInvoeren(); i++) {
				transactie.getInvoer(i).handtekening = handtekening;
			}

			return transactie;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Controleer of de handtekening van een invoer klopt
	 * @param invoer	Invoer waarvan de handtekening gecontroleerd moet worden.
	 * @param huidigeTransactie		De transactie die de gegeven invoer bevat.
	 * @param voorgaandeTransactie	De transactie die de uitvoer bevat waar de gegeven invoer naar verwijst.
	 * @return True (de handtekening klopt) of false (de handtekening klopt niet).
	 */
	public static boolean controleerHandtekening(Invoer invoer, Transactie huidigeTransactie, Transactie voorgaandeTransactie) {
		
		try {
			//pak de publieke sleutel van de voorgaande transactie 
			String pubSleutel = voorgaandeTransactie.getUitvoer(invoer.uitvoerIndex).publiekeSleutel;
			byte[] pubBytes = new BigInteger(pubSleutel, 16).toByteArray();
			
			//zet publieke sleutel om naar een PublicKey-object
			X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubBytes);
			KeyFactory keyFactory = KeyFactory.getInstance("EC");
			PublicKey pubKey = keyFactory.generatePublic(pubKeySpec);

			//controleer de handtekening
			Signature sig = Signature.getInstance("SHA256withECDSA");
			sig.initVerify(pubKey);
			sig.update(huidigeTransactie.getHash().getBytes("UTF-8"));
			byte[] handtekeningBytes = new BigInteger(invoer.handtekening, 16).toByteArray();
			
			return sig.verify(handtekeningBytes);
		
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} 
	}
	
	public String getPrivesleutel() {
		return privesleutel;
	}
	
	public String getPubliekeSleutel() {
		return publiekeSleutel;
	}
}
