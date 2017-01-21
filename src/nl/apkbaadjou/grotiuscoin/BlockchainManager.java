package nl.apkbaadjou.grotiuscoin;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

import org.json.JSONObject;


/**
 * Deze klasse is verantwoordelijk voor het beheren van de blockchain.
 * 
 * De blockchain wordt opgeslagen in een bestand op de harde schijf. Het bestand heeft de volgende
 * opbouw:
 * 		-Aan het begin staat op elke regel een blok in JSON-formaat. Deze blokken zitten in de
 * 		 hoofdketen, en zijn gerangschikt op blokhoogte.
 * 		-Vervolgens komt een regel met de tekst "zijketen"
 * 		-Hierna staat op elke regel een blok in de zijketen (ook in JSON-formaat). Deze blokken
 * 		 kunnen in willekeurige volgorde staan.
 * 
 * Het eerste blok heet het 'genesisblok'. Dit blok staat in het programma vastgelegd en kan niet worden
 * gewijzigd. De eerste regel van het blockchainbestand bevat altijd het genesisblok.
 *
 */
public class BlockchainManager {
	
	/**
	 * Pad naar het blockchainbestand.
	 */
	public static final String BLOCKCHAIN_PATH = Main.DIRECTORY + File.separator + "blockchain";
	
	/**
	 * Maximale waarde die de target mag hebben.
	 */
	public static final String MAX_TARGET = "0000FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF";
	
	/**
	 * Het aantal blokken dat gegenereerd moet worden voordat de target bijgesteld wordt.
	 */
	public static final int AANTAL_BLOKKEN_TOT_RETARGET = 100;
	
	/**
	 * Aantal minuten dat gemiddeld nodig is om één blok te genereren.
	 */
	public static final double AANTAL_MINUTEN_PER_BLOK = 10;
	
	/**
	 * Systeemonafhankelijk newline-teken.
	 */
	public static final String NEWLINE = System.getProperty("line.separator");
	
	/**
	 * Verwijst naar het eerste blok in de blockchain.
	 */
	private BlokIndex genesisBlok;
	
	/**
	 * Verwijst naar het laatste blok in de langste keten van de blockchain.
	 */
	private BlokIndex besteBlok;
	
	/**
	 * Blokhoogte van het laatste blok in de langste keten van de blockchain.
	 */
	private int besteBlokhoogte;
	
	/**
	 * Het blok dat deze node probeert te genereren.
	 */
	private Blok eigenBlok;

	/**
	 * Weesblokken zijn blokken waarvan het ouderblok nog niet is ontvangen. 
	 * Een weesblok wordt tijdelijk bewaard in deze lijst. Zodra het 
	 * ouderblok is ontvangen, wordt het blok toegevoegd aan de blockchain.
	 */
	private ArrayList<Blok> weesblokken;
	
	/**
	 * Blokken die in een zijketen zitten worden in deze lijst bewaard.
	 */
	private ArrayList<BlokIndex> blokkenZijketen;
	
	/**
	 * Deze lijst bevat geldige transacties die nog niet in een blok zijn opgenomen.
	 */
	private ArrayList<Transactie> transactiePool;
	
	/**
	 * Publieke sleutel van degene die de coinbasebeloning moet ontvangen als een blok gegenereerd wordt.
	 */
	private String coinbaseSleutel;
	
	/**
	 * @param coinbaseSleutel 	Publieke sleutel van degene die de coinbasebeloning moet ontvangen als een blok gegenereerd wordt.
	 */
	public BlockchainManager(String coinbaseSleutel) {
		weesblokken = new ArrayList<Blok>();
		blokkenZijketen = new ArrayList<BlokIndex>();
		transactiePool = new ArrayList<Transactie>();
		this.coinbaseSleutel = coinbaseSleutel;
	}
	
	/**
	 * Initialiseer de blockchain.
	 */
	public void initBlockchain() {
		
		//maak een directory aan voor de blockchain (als dat niet al eerder is gedaan)
		File file = new File(Main.DIRECTORY);
		if (!file.exists()) {
			file.mkdir();
		}
		
		//maak een blockchainbestand (als dat niet al eerder is gedaan)
		file = new File(BLOCKCHAIN_PATH);
		FileWriter writer = null;
		if (!file.exists()) {
			try {
	
				writer = new FileWriter(file);
				
				//voeg het genesisblok toe 
				Blok genesisBlok = new Blok("0000000000000000000000000000000000000000000000000000000000000000", 0);
				genesisBlok.setNonce(22106);
				Transactie coinbaseTx = new Transactie();
				coinbaseTx.voegUitvoerToe(new Uitvoer(5000, "00000000000000000000000000000000"));
				Invoer in = new Invoer("0000000000000000000000000000000000000000000000000000000000000000", 0);
				in.handtekening = "";
				coinbaseTx.voegInvoerToe(in);
				genesisBlok.voegTransactieToe(coinbaseTx);			
				
				writer.write(genesisBlok.toJSON().toString() + NEWLINE +
							 "zijketen" + NEWLINE);
			} catch (IOException e) {
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
		


		//laad de blockchain in het geheugen
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(BLOCKCHAIN_PATH));
			genesisBlok = null;
			blokkenZijketen.clear();
			
			String line;
			while (!(line = reader.readLine()).equals("zijketen")) {
				
				//hoofdketen
				if (genesisBlok == null) {
					//eerste blok
					genesisBlok = new BlokIndex(new Blok(new JSONObject(line)), null, null);
					besteBlok = genesisBlok;
					besteBlokhoogte = 0;
					continue;
				}
				
				BlokIndex nieuweBlokIndex = new BlokIndex(new Blok(new JSONObject(line)), 
														  besteBlok, null);
				besteBlok.setVolgendeBlokIndex(nieuweBlokIndex);
				besteBlok = nieuweBlokIndex;
				besteBlokhoogte++;
			}
			
			while ((line = reader.readLine()) != null) {
				//zijketen
				blokkenZijketen.add(new BlokIndex(new Blok(new JSONObject(line)),
												  null, null));
			}
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
		
		//rangschik de BlokIndexen van de zijketens
		for (BlokIndex blokIndex : blokkenZijketen) {
			String vorigeHash = blokIndex.getBlok().getVorigeBlokHash();
			
			//zoek voor vorige BlokIndex in de zijketens
			for (BlokIndex b : blokkenZijketen) {
				if (b.getBlok().getHash().equals(vorigeHash)) {
					blokIndex.setVorigeBlokIndex(b);
					break;
				}
			}
			
			//Geen BlokIndex gevonden in de zijketens? Zoek dan in de hoofdketen.
			if (blokIndex.getVorigeBlokIndex() == null) {
				//begin te zoeken bij het beste blok en werk dan terug
				BlokIndex b = besteBlok;
				while (!b.getBlok().getVorigeBlokHash().equals(vorigeHash)) {
					b = b.getVorigeBlokIndex();
				}
				
				blokIndex.setVorigeBlokIndex(b.getVorigeBlokIndex());
			}
		}
		
		//initialiseer eigenBlok
		initEigenBlok();
	}
	
	/**
	 * Probeer een blok te genereren.
	 * @return Het gegenereerde blok of null (als er geen geldig blok gevonden is).
	 */
	public Blok mine() {

		//voeg transacties uit transactiePool toe
		for (Transactie tx : transactiePool) {
			if (!eigenBlok.getTransacties().contains(tx)) {
				eigenBlok.voegTransactieToe(tx);
			}
		}
		eigenBlok.setTimestamp(System.currentTimeMillis());
		int nonce = 0;
		
		//probeer 50 ms lang een blok te genereren alvorens verder te gaan
		while (System.currentTimeMillis() - eigenBlok.getTimestamp() < 50) {
			eigenBlok.setNonce(nonce);
			
			if (eigenBlok.getHash().compareTo(eigenBlok.getTarget()) <= 0) {
				//geldig blok gevonden
				System.out.println("blok gevonden: " + eigenBlok.getHash() + "  JSON: " + eigenBlok.toJSON());
				
				//maak een nieuw eigenBlok en voeg het oude toe aan de blockchain
				Blok oudeEigenBlok = eigenBlok;
				boolean toegevoegd = voegBlokToe(oudeEigenBlok);
				initEigenBlok();
				if (toegevoegd) {
					return oudeEigenBlok;
				} else {
					return null;
				}
			}
			
			nonce++;
		}
		
		//geen geldig blok gevonden
		return null;
	}

	/**
	 * Controleert of de blockchain een blok met de gegeven hash bevat.
	 * @param hash	De hashwaarde van het gezochte blok.
	 * @return True (het blok zit in de blockchain) of false (het blok zit niet in de blockchain).
	 */
	private boolean blockchainBevat(String hash) {
		return (getBlokIndex(hash) != null);
	}
	
	/**
	 * Zoek de BlokIndex van een blok met de gegeven hash.
	 * @param blokHash	Hash van het gezochte blok.
	 * @return BlokIndex van het gezochte blok (of null als het blok niet gevonden is).
	 */
	private BlokIndex getBlokIndex(String blokHash) {
		//zoek in de hoofdketen
		if (genesisBlok.getBlok().getHash().equals(blokHash)) {
			return genesisBlok;
		}
		BlokIndex b = genesisBlok;
		while (b.getVolgendeBlokIndex() != null) {
			b = b.getVolgendeBlokIndex();
			if (b.getBlok().getHash().equals(blokHash)) {
				return b;
			}
		}	
		
		//Niets gevonden in de hoofdketen? Zoek in de zijketens
		for (BlokIndex blokIndex : blokkenZijketen) {
			if (blokIndex.getBlok().getHash().equals(blokHash)) {
				return blokIndex;
			}
		}
		
		//niets gevonden
		return null;
	}
	
	/**
	 * Controleer een blok en voeg het toe aan de blockchain.
	 * @param blok	Het blok dat moet worden toegevoegd.
	 * @return True als het blok wordt toegevoegd, anders false.
	 */
	public boolean voegBlokToe(Blok blok) {
		
		//controleer of de hash klopt
		if (blok.getHash().compareTo(blok.getTarget()) > 0 ||
			blok.getTarget().compareTo(BlockchainManager.MAX_TARGET) > 0) {
			System.out.println("blok hash is onjuist");
			return false;
		}
		
		//controleer of het blok niet al eerder is verwerkt
		if (blockchainBevat(blok.getHash()) ||
			weesblokken.contains(blok)) {
			System.out.println("blok is al verwerkt");
			return false;
		}
	
		//controleer of we het ouderblok hebben
		if (!blockchainBevat(blok.getVorigeBlokHash())) {
			
			//geen ouderblok in de blockchain; voeg het blok toe aan de weesblokkenlijst
			weesblokken.add(blok);
			
			System.out.println("blok heeft geen ouder");
			System.out.println("	beste blok: " + besteBlok.getBlok().toJSON());
			System.out.println("	genesisblok: " + genesisBlok.getBlok().toJSON());
			System.out.println("	hoofdketen:");
			BlokIndex index = genesisBlok;
			System.out.println("	" + genesisBlok.getBlok().toJSON());
			while (!(index.getVolgendeBlokIndex() == null)) {
				index = index.getVolgendeBlokIndex();
				System.out.println("	" + index.getBlok().toJSON());
			}
			return false;
		}
		
		//controleer timestamp
		if (blok.getTimestamp() > (System.currentTimeMillis() + 60 * 60 * 1000) ||
			blok.getTimestamp() < getMinimumTimestamp(blok)) {
			//Timestamp is verder dan één uur in de toekomst of verder dan 
			//(ongeveer) één uur in het verleden.
			System.out.println("Blok timestamp is onjuist");
			return false;
		}
			
		//controleer of de target klopt
		if (!blok.getTarget().equals(bepaalTarget(blok))) {
			System.out.println("Blok target is onjuist");
			return false;
		}
		
		//controleer coinbasetransactie
		if (blok.getTransacties().size() == 0) {
			System.out.println("Blok bevat geen transacties");
			return false;
		} 
		Transactie coinbaseTx = blok.getTransacties().get(0);
		if (coinbaseTx.getAantalInvoeren() != 1 ||
			coinbaseTx.getAantalUitvoeren() != 1) {
			System.out.println("Blok bevat coinbasetransactie met onjuiste aantal invoeren of uitvoeren");
			return false;
		}
		if (coinbaseTx.getUitvoer(0).bedrag != getCoinbaseUitbetaling(bepaalBlokhoogte(getBlokIndex(blok.getVorigeBlokHash()))+1)) {
			System.out.println("Blok bevat onjuiste coinbasebeloning");
			System.out.println("bedrag: " + coinbaseTx.getUitvoer(0).bedrag);
			System.out.println("coinbaseUitbetaling: " + getCoinbaseUitbetaling(bepaalBlokhoogte(getBlokIndex(blok.getVorigeBlokHash()))+1));
			return false;
		}
		
		//controleer andere transacties
		if (!controleerBlokTransacties(blok.getTransacties())) {
			System.out.println("Blok bevat een onjuiste transactie");
			return false;
		}
	
		//voeg het blok toe aan de blockchain
		BlokIndex ouderBlokIndex = getBlokIndex(blok.getVorigeBlokHash());
		BlokIndex nieuweBlokIndex = new BlokIndex(blok, ouderBlokIndex, null);
		if (!ouderBlokIndex.getBlok().equals(besteBlok.getBlok())) {
			//blok verlengt niet de hoofdketen, dus zit het in een zijketen
			blokkenZijketen.add(nieuweBlokIndex);
			
			System.out.println("Blok zit in een zijketen");
		} else {
			//vorige blok is het beste blok, dus dit blok zit in de hoofdketen
			//zorg ervoor dat het ouderblok verwijst naar dit blok
			ouderBlokIndex.setVolgendeBlokIndex(nieuweBlokIndex);
		}
		
		//controleer of het blok het nieuwe beste blok is
		if (bepaalBlokhoogte(nieuweBlokIndex) > besteBlokhoogte) {
			
			System.out.println("Blok is nieuwe beste blok");
			
			BlokIndex oudeBesteBlok = besteBlok;
			besteBlok = nieuweBlokIndex;
			besteBlokhoogte++;
			
			//zorg ervoor dat het eigenBlok weer naar het beste blok verwijst en pas de target aan indien nodig
			if (!besteBlok.getBlok().equals(eigenBlok)) {
				eigenBlok.setVorigeBlokHash(besteBlok.getBlok().getHash());
				eigenBlok.setTarget(bepaalTarget(blok));
			}
			
			//controleer of het nieuwe beste blok in een zijketen zit (en er dus een
			//nieuwe langste keten is)
			if (blokkenZijketen.contains(besteBlok)) {
				System.out.println("REORGANISEER");
				reorganiseer(besteBlok, oudeBesteBlok);
			}
		}
			
		//Als het toegevoegde blok de ouder van een weesblok is, voeg dan
		//het weesblok toe aan de blockchain.
		ArrayList<Blok> verwijder = new ArrayList<Blok>();
		for (Blok weesblok : weesblokken) {
			if (weesblok.getVorigeBlokHash().equals(blok.getHash())) {
				verwijder.add(weesblok);
			}
		}
		for (Blok weesblok : verwijder) {
			System.out.println("Probeer weesblok toe te voegen aan blockchain...");
			weesblokken.remove(weesblok);
			voegBlokToe(weesblok);
		}
		
		//Door het toevoegen van dit blok is het mogelijk dat sommige transacties in
		//de transactiePool ongeldig zijn geworden, doordat ze verwijzen naar een uitvoer
		//die al uitgegeven is door een transactie in dit blok. Haal deze transacties uit
		//de transactiePool.
		ArrayList<Transactie> verwijderTx = new ArrayList<Transactie>();
		for (Transactie blokTx : blok.getTransacties()) {
			for (int i=0; i<blokTx.getAantalInvoeren(); i++) {
				Invoer blokTxInvoer = blokTx.getInvoer(i);
				
				for (Transactie poolTx : transactiePool) {
					for (int j=0; j<poolTx.getAantalInvoeren(); j++) {
						Invoer poolTxInvoer = poolTx.getInvoer(j);
						
						if (blokTxInvoer.hashVorigeTransactie.equals(poolTxInvoer.hashVorigeTransactie) &&
							blokTxInvoer.uitvoerIndex == poolTxInvoer.uitvoerIndex) {
							
							if (!verwijderTx.contains(poolTx)) {
								System.out.println("Verwijder transactie uit pool: " + poolTx.toJSON().toString());
								verwijderTx.add(poolTx);
							}
							break;
						}
					}
				}
			}
		}
		for (Transactie tx : verwijderTx) {
			transactiePool.remove(tx);
		}
		
		//haal alle transacties uit het eigenBlok zodat we zeker weten dat er geen ongeldige transacties meer in zitten
		if (!besteBlok.getBlok().equals(eigenBlok)) {
				
			eigenBlok.getTransacties().clear();
			
			//voeg coinbasetransactie toe en zorg ervoor dat deze de hash van het nieuwe blok bevat (zodat de
			//transactiehash uniek is) en de juiste coinbasebeloning.
			Transactie coinbaseTransactie = new Transactie();
			coinbaseTransactie.voegUitvoerToe(new Uitvoer(getCoinbaseUitbetaling(besteBlokhoogte+1), coinbaseSleutel));
			Invoer in = new Invoer(besteBlok.getBlok().getHash(), 0);
			in.handtekening = "";
			coinbaseTransactie.voegInvoerToe(in);
			eigenBlok.voegTransactieToe(coinbaseTransactie);
		}
		
		return true;
	}	
	
	/**
	 * Controleer of de transacties in een blok geldig zijn. 
	 * @param transacties	Lijst met alle transacties van een blok.
	 * @return True (alle transacties zijn geldig) of false (niet alle transacties zijn geldig).
	 */
	private boolean controleerBlokTransacties(ArrayList<Transactie> transacties) {
			
		//Deze lijst houdt bij welke invoeren al zijn gebruikt door transacties in dit blok.
		ArrayList<Invoer> gebruikteInvoeren = new ArrayList<Invoer>();	
		
		//begin met index 1 (0 is de coinbasetransactie, die hebben we al gecontroleerd)
		for (int i=1; i<transacties.size(); i++) {
			Transactie transactie = transacties.get(i);

			int somInvoeren = 0;
			int somUitvoeren = 0;
			
			//controleer het formaat van de transactie
			if (transactie.getAantalInvoeren() == 0 || transactie.getAantalUitvoeren() == 0) {
				System.out.println("Transactie heeft geen invoeren of geen uitvoeren");
				return false;
			}
			
			for (int j=0; j<transactie.getAantalUitvoeren(); j++) {
				Uitvoer uitvoer = transactie.getUitvoer(j);
				somUitvoeren += uitvoer.bedrag;
				if (uitvoer.bedrag <= 0) {
					System.out.println("Transactie bevat een uitvoer met een ongeldig bedrag");
					return false;
				}
			}
			
			//controleer de transactie-invoeren
			for (int j=0; j<transactie.getAantalInvoeren(); j++) {
				Invoer invoer = transactie.getInvoer(j);
				
				//controleer eerst of de oudertransactie in de blockchain zit
				Transactie vorigeTransactie = zoekTransactieInBlockchain(invoer.hashVorigeTransactie);
				if (vorigeTransactie == null) {
					//oudertransactie zit niet in blockchain
					System.out.println("Transactie heeft geen ouder in de blockchain");
					return false;
				}
				
				//controleer of de uitvoer van de oudertransactie niet al is uitgegeven door een transactie in de blockchain
				if (isUitvoerUitgegeven(invoer.hashVorigeTransactie, invoer.uitvoerIndex)) {
					System.out.println("Transactie verwijst naar een uitvoer die al uitgegeven is in de blockchain");
					return false;
				}
				
				//controleer of de uitvoer van de oudertransactie niet al is uitgegeven door een transactie in dit blok
				for (Invoer gebruikteInvoer : gebruikteInvoeren) {
					if (invoer.hashVorigeTransactie.equals(gebruikteInvoer.hashVorigeTransactie) &&
						invoer.uitvoerIndex == gebruikteInvoer.uitvoerIndex) {
						
						//uitvoer is al uitgegeven door een andere transactie in dit blok
						System.out.println("Transactie verwijst naar een uitvoer die al uitgegeven is in dit blok");
						return false;
					}
				}
				
				//controleer of de handtekening van de invoer klopt
				if (!Wallet.controleerHandtekening(invoer, transactie, vorigeTransactie)) {
					//handtekening klopt niet
					System.out.println("Transactie bevat een invoer met een onjuiste handtekening");
					return false;
				}
				
				somInvoeren += vorigeTransactie.getUitvoer(invoer.uitvoerIndex).bedrag;
				gebruikteInvoeren.add(invoer);
			}
			
			//controleer of de transactie-uitvoeren niet te veel uitgeven
			if (somUitvoeren > somInvoeren) {
				System.out.println("Transactie-uitvoeren geven meer uit dan toegestaan");
				return false;
			}
		}
		
		//alle transacties zijn geldig 
		return true;
	}

	/**
	 * Bepaal de blokhoogte van een blok met de gegeven blokIndex
	 * @param blokIndex	BlokIndex van het gegeven blok.
	 * @return Blokhoogte van het gegeven blok.
	 */
	private int bepaalBlokhoogte(BlokIndex blokIndex) {
		
		//ga terug in de blockchain tot aan het genesisblok, en tel het aantal blokken
		int aantalBlokken = 0;
		while (!blokIndex.getBlok().equals(genesisBlok.getBlok())) {
			blokIndex = blokIndex.getVorigeBlokIndex();
			aantalBlokken++;
		}
		
		return aantalBlokken;
	}

	
	/**
	 * De blockchain wordt gereorganiseerd als een zijketen langer is geworden dan de hoofdketen.
	 * De verwijzingen in de blockchain worden aangepast zodat ze naar de nieuwe langste keten
	 * wijzen.
	 * @param blokIndex	Het laatste blok in de nieuwe langste keten.
	 */
	private void reorganiseer(BlokIndex nieuweBesteBlok, BlokIndex oudeBesteBlok) {
		
		//Werk terug tot het laatste gemeenschappelijke blok van de twee ketens,
		//en zorg ervoor dat de BlokIndexen naar de langste keten wijzen.
		
		BlokIndex blokIndexLangeKeten = nieuweBesteBlok.getVorigeBlokIndex();	
		BlokIndex blokIndexKorteKeten = oudeBesteBlok;
		
		BlokIndex vorig = nieuweBesteBlok;
		while (!blokIndexLangeKeten.getBlok().equals(blokIndexKorteKeten.getBlok())) {
			
			//Zorg ervoor dat de blokken in blokkenZijketen worden uitgewisseld.
			blokkenZijketen.remove(blokIndexLangeKeten);
			blokkenZijketen.add(blokIndexKorteKeten);
			
			blokIndexLangeKeten.setVolgendeBlokIndex(vorig);
			vorig = blokIndexLangeKeten;
			
			blokIndexLangeKeten = blokIndexLangeKeten.getVorigeBlokIndex();
			blokIndexKorteKeten = blokIndexKorteKeten.getVorigeBlokIndex();
		}
		
		blokIndexLangeKeten.setVolgendeBlokIndex(vorig);
	}

	/**
	 * Sla de blockchain op in een bestand. 
	 */
	public void slaBlockchainOp() {

		FileWriter writer = null;
		
		try {
			writer = new FileWriter(BLOCKCHAIN_PATH);
			writer.write(blockchainNaarString());			
		} catch (IOException e) {
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
	
	/**
	 * Bepaal hoe groot de timestamp van het gegeven blok minimaal moet zijn.
	 * @param blok	Het blok waarvan de minimumtimestamp moet worden bepaald.
	 * @return De kleinst mogelijke geldige waarde voor de timestamp van het blok.
	 */
	private long getMinimumTimestamp(Blok blok) {
		//Het is niet mogelijk om de minimale timestampwaarde relatief t.o.v.
		//de huidige tijd te nemen, aangezien we ook de timestamps van oude
		//blokken moeten kunnen controleren. Daarom wordt het gemiddelde van 
		//de timestamps van de 12 voorgaande blokken gebruikt als minimum. 
		//Het duurt ongeveer 10 minuten om één blok te genereren, dus het gemiddelde
		//van de timestamps van de 12 voorgaande zal ongeveer één uur eerder aangeven
		//dan de timestamp van het blok zelf.
		
		long som = 0;
		BlokIndex index = getBlokIndex(blok.getVorigeBlokHash());
		for (int i=0; i<12; i++) {
			som += index.getBlok().getTimestamp();
			if (index.getVorigeBlokIndex() != null) {
				index = index.getVorigeBlokIndex();
			} else {
				//Het begin van de keten is een uitzondering
				return 0;
			}
		}
		
		return som/12;
	}
	
	/**
	 * Bepaal de nodige target van een blok.
	 * Deze methode gaat ervanuit dat het ouderblok in de blockchain zit.
	 * @param blok	Het blok waarvan de target bepaald moet worden.
	 * @return De target die het gegeven blok moet hebben.
	 */
	private String bepaalTarget(Blok blok) {
		
		//begin met ouderindex
		BlokIndex index = getBlokIndex(blok.getVorigeBlokHash());
		
		int blokhoogteOuder = bepaalBlokhoogte(index);
		if (blokhoogteOuder <= AANTAL_BLOKKEN_TOT_RETARGET) {
			return MAX_TARGET;
		}
		
		int aantalPlaatsenTerug = (blokhoogteOuder-1) % AANTAL_BLOKKEN_TOT_RETARGET;
		while (aantalPlaatsenTerug > 0) {
			index = index.getVorigeBlokIndex();
			aantalPlaatsenTerug--;
		}

		//eerste index
		BlokIndex eersteIndex = index;
		for (int i=0; i<AANTAL_BLOKKEN_TOT_RETARGET; i++) {
			eersteIndex = eersteIndex.getVorigeBlokIndex();
		}
		
		//laatste index
		index = index.getVorigeBlokIndex();			
		
		//vermenigvuldig de target met een bepaalde factor, zodat het aantal minuten per blok constant blijft 
		long verstrekenTijd = index.getBlok().getTimestamp() - eersteIndex.getBlok().getTimestamp();
	
		double aantalMinutenPerBlok = ((double) verstrekenTijd) / ((double) AANTAL_BLOKKEN_TOT_RETARGET*60*1000);
		double factor = aantalMinutenPerBlok/AANTAL_MINUTEN_PER_BLOK;	
		
		BigInteger oudeTarget = new BigInteger(index.getBlok().getTarget(), 16);
		BigDecimal temp = new BigDecimal(oudeTarget).multiply(new BigDecimal(factor));
		String nieuweTarget = temp.toBigInteger().toString(16);
		nieuweTarget = String.format("%64s", nieuweTarget.toUpperCase()).replace(' ', '0');
		
		//zorg ervoor dat nieuwe target niet groter is dan MAX_TARGET
		if (nieuweTarget.compareTo(MAX_TARGET) > 0) {
			nieuweTarget = MAX_TARGET;
		}
		
		return nieuweTarget;
	}
	
	/**
	 * Initaliseer het eigenBlok.
	 */
	private void initEigenBlok() {
		eigenBlok = new Blok(besteBlok.getBlok().getHash(), System.currentTimeMillis());
		eigenBlok.setTarget(bepaalTarget(eigenBlok));
		
		//voeg coinbasetransactie toe
		Transactie coinbaseTransactie = new Transactie();
		coinbaseTransactie.voegUitvoerToe(new Uitvoer(getCoinbaseUitbetaling(besteBlokhoogte+1), coinbaseSleutel));
		//Voeg een invoer toe die de hash van het voorgaande blok bevat. Deze invoer heeft geen betekenis, maar
		//zorgt er alleen maar voor dat de hash van de coinbasetransactie uniek is.
		Invoer in = new Invoer(besteBlok.getBlok().getHash(), 0);
		in.handtekening = "";
		coinbaseTransactie.voegInvoerToe(in);
		eigenBlok.voegTransactieToe(coinbaseTransactie);
	}
	
	/**
	 * Bepaal de grootte van de coinbasebeloning voor een bepaalde blokhoogte.
	 * @param blokhoogte	Blokhoogte waarvoor de coinbasebeloning moet worden berekend.
	 * @return De coinbasebeloning van een blok met de gegeven blokhoogte.
	 */
	private int getCoinbaseUitbetaling(int blokhoogte) {
		//Begin bij 5000 en halveer de uitbetaling elke 200 blokken
		int uitbetaling = 5000;
		int aantalHalveringen = blokhoogte/200;
		for (int i=0; i<aantalHalveringen; i++) {
			uitbetaling *= 0.5;
		}
	
		return uitbetaling;
	}
	
	/**
	 * Zoek de transactie met de gegeven hash op in de blockchain.
	 * @param txHash	Hash van de gezochte transactie.
	 * @return De gezochte transactie (of null als er geen transactie gevonden is).
	 */
	private Transactie zoekTransactieInBlockchain(String txHash) {
		
		BlokIndex index = genesisBlok;
		
		while (index.getVolgendeBlokIndex() != null) {
			index = index.getVolgendeBlokIndex();
			
			Transactie tx = index.getBlok().getTransactie(txHash);
			if (tx != null) {
				return tx;
			}
		}
		
		//niets gevonden
		return null;
	}
	
	/**
	 * Bepaal of de uitvoer met de gegeven transactiehash en uitvoerIndex al is uitgegeven 
	 * door een transactie in de blockchain.
	 * @param txHash		Hash van de transactie waarin de uitvoer zit.
	 * @param uitvoerIndex	Index van de uitvoer.
	 * @return True (de uitvoer is al uitgegeven) of false (uitvoer is nog niet uitgegeven).
	 */
	private boolean isUitvoerUitgegeven(String txHash, int uitvoerIndex) {
				
		//controleer alle blokken in de blockchain
		BlokIndex index = genesisBlok;
		while (index.getVolgendeBlokIndex() != null) {
			index = index.getVolgendeBlokIndex();
			
			//controleer alle transacties in het blok
			for (Transactie tx : index.getBlok().getTransacties()) {
				
				//controleer alle invoeren van de transactie
				for (int i=0; i<tx.getAantalInvoeren(); i++) {
					Invoer invoer = tx.getInvoer(i);
					if (invoer.hashVorigeTransactie.equals(txHash) &&
						invoer.uitvoerIndex == uitvoerIndex) {
						//verwijzing naar uitvoer gevonden
						//de uitvoer is dus al uitgegeven
						return true;
					}
				}
			}
		}
		
		//geen verwijzing naar deze uitvoer gevonden in de blockchain
		return false;
	}
	
	/**
	 * Controleer een transactie en voeg deze (indien geldig) toe aan de transactiePool
	 * @param transactie	De transactie die moet worden toegevoegd aan de transactiePool.
	 * @return True (de transactie is toegevoegd) of false (transactie is ongeldig en niet toegevoegd).
	 */
	public boolean voegTransactieToe(Transactie transactie) {
		
		System.out.println("controleer transactie: " + transactie.toJSON());
		
		int somInvoeren = 0;
		int somUitvoeren = 0;
		
		//controleer het formaat van de transactie
		if (transactie.getAantalInvoeren() == 0 || transactie.getAantalUitvoeren() == 0) {
			System.out.println("Transactie heeft geen invoeren of geen uitvoeren");
			return false;
		}
		
		for (int i=0; i<transactie.getAantalUitvoeren(); i++) {
			Uitvoer uitvoer = transactie.getUitvoer(i);
			somUitvoeren += uitvoer.bedrag;
			if (uitvoer.bedrag <= 0) {
				System.out.println("Transactie bevat een uitvoer met een ongeldig bedrag");
				return false;
			}
		}
		
		//controleer de transactie-invoeren
		for (int i=0; i<transactie.getAantalInvoeren(); i++) {
			Invoer invoer = transactie.getInvoer(i);
			
			//controleer eerst of de oudertransactie in de blockchain zit
			Transactie vorigeTransactie = zoekTransactieInBlockchain(invoer.hashVorigeTransactie);
			if (vorigeTransactie == null) {
				//oudertransactie zit niet in blockchain
				System.out.println("Transactie heeft geen ouder in de blockchain");
				return false;
			}
			
			//controleer of de uitvoer van de oudertransactie niet al is uitgegeven door een transactie in de blockchain
			if (isUitvoerUitgegeven(invoer.hashVorigeTransactie, invoer.uitvoerIndex)) {
				System.out.println("Transactie verwijst naar een uitvoer die al uitgegeven is");
				return false;
			}
			
			//controleer of de uitvoer van de oudertransactie niet al is uitgegeven door een transactie in de transactiePool
			for (Transactie poolTx : transactiePool) {
				for (int j=0; j<poolTx.getAantalInvoeren(); j++) {
					Invoer poolTxInvoer = poolTx.getInvoer(j);
					if (invoer.uitvoerIndex == poolTxInvoer.uitvoerIndex &&
						invoer.hashVorigeTransactie.equals(poolTxInvoer.hashVorigeTransactie)) {
					
						//de uitvoer is al uitgegeven door een andere transactie
						System.out.println("Transactie verwijst naar een uitvoer die al uitgegeven is");
						return false;
					
					}
				}
			}
			
			//controleer of de handtekening van de invoer klopt
			if (!Wallet.controleerHandtekening(invoer, transactie, vorigeTransactie)) {
				//handtekening klopt niet
				System.out.println("Transactie bevat een invoer met een onjuiste handtekening");
				return false;
			}
			
			somInvoeren += vorigeTransactie.getUitvoer(invoer.uitvoerIndex).bedrag;
		}
		
		//controleer of de transactie-uitvoeren niet te veel uitgeven
		if (somUitvoeren > somInvoeren) {
			System.out.println("Transactie-uitvoeren geven meer uit dan toegestaan");
			return false;
		}
		
		//transactie is geldig; voeg toe aan transactiePool
		System.out.println("Transactie is geldig; wordt toegevoegd aan transactiePool");
		transactiePool.add(transactie);
		return true;
	}
	
	/**
	 * @return	De blokhoogte van het laatste blok in de langste keten van de blockchain.
	 */
	public int getBlokhoogte() {
		return besteBlokhoogte;
	}
	
	/**
	 * Zet de blockchain om in een string. 
	 * @return De blockchain in stringformaat.
	 */
	public String blockchainNaarString() {
		StringBuilder data = new StringBuilder();
		
		//schrijf op elke regel een blok uit de hoofdketen
		BlokIndex index = genesisBlok;
		while (!index.getBlok().equals(besteBlok.getBlok())) {
			data.append(index.getBlok().toJSON().toString() + NEWLINE);
			index = index.getVolgendeBlokIndex();
		}
		data.append(index.getBlok().toJSON().toString() + NEWLINE +
					 "zijketen" + NEWLINE);
		
		//schrijf op elke regel een blok uit de zijketens
		for (BlokIndex blokIndex : blokkenZijketen) {
			data.append(blokIndex.getBlok().toJSON().toString() + NEWLINE);
		}
		
		return data.toString();
	}
	
	/**
	 * Zoekt UTXO's in de blockchain die geld sturen naar de gegeven publieke sleutel.
	 * @param publiekeSleutel	De publieke sleutel die de gezochte UTXO's moeten bevatten.
	 * @return Lijst van alle transacties in de blockchain met uitvoeren die de gegeven publieke sleutel bevatten en nog niet zijn uitgegeven.
	 */
	public ArrayList<Transactie> zoekUTXOs(String publiekeSleutel) {
		
		ArrayList<Transactie> gevondenTransacties = new ArrayList<Transactie>();
		ArrayList<Integer> gevondenUitvoerIndex = new ArrayList<Integer>();
		
		BlokIndex index = genesisBlok;
		
		//zoek door elk blok in de blockchain, beginnende bij het genesisblok
		while (index.getVolgendeBlokIndex() != null) {
			index = index.getVolgendeBlokIndex();
						
			ArrayList<Transactie> transacties = index.getBlok().getTransacties();
			
			//zoek in elke transactie van het blok
			for (Transactie tx : transacties) {
						
				//controleer of een van de tot nu gevonden uitvoeren wordt uitgegeven in deze transactie
				for (int i=0; i<tx.getAantalInvoeren(); i++) {
					Invoer invoer = tx.getInvoer(i);
					
					int verwijderIndex = -1;
					for (int j=0; j<gevondenTransacties.size(); j++) {
						if (invoer.uitvoerIndex == gevondenUitvoerIndex.get(j).intValue()) {
							Transactie gevondenTx = gevondenTransacties.get(j);
							if (invoer.hashVorigeTransactie.equals(gevondenTx.getHash())) {
								//uitvoer wordt uitgegeven in deze transactie; verwijder hem uit de lijst
								//met gevonden transacties 
								verwijderIndex = j;
								break;
							}
						}
					}
					
					if (verwijderIndex != -1) {
						gevondenTransacties.remove(verwijderIndex);
						gevondenUitvoerIndex.remove(verwijderIndex);
					}	
				}
				
				//ga voor elke uitvoer na of het de gegeven publieke sleutel bevat
				for (int i=0; i<tx.getAantalUitvoeren(); i++) {
					Uitvoer uitvoer = tx.getUitvoer(i);
					if (uitvoer.publiekeSleutel.equals(publiekeSleutel)) {
						
						//mogelijke UTXO gevonden; ga door naar volgende transactie
						gevondenTransacties.add(tx);
						gevondenUitvoerIndex.add(new Integer(i));
						break;
					}
				}
			}
		}
		
		return gevondenTransacties;
	}
	
	/**
	 * Vervang het blockchainbestand en laad de blockchain opnieuw.
	 * @param blockchain	Inhoud van het nieuwe blockchainbestand.
	 */
	public void vervangBlockchain(String blockchain) {
		
		FileWriter writer = null;
		System.out.println("Probeer ontvangen blockchain naar bestand te schrijven: " + blockchain);
		try {
			writer = new FileWriter(BLOCKCHAIN_PATH);
			writer.write(blockchain);
			System.out.println("Klaar met blockchainbestand schrijven");
		} catch (IOException e) {
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
		
		//laad de blockchain opnieuw
		initBlockchain();
		
		//Door het laden van de nieuwe blockchain hebben sommige weesblokken 
		//misschien een ouder gekregen. Voeg deze blokken toe aan de blockchain.
		ArrayList<Blok> verwijder = new ArrayList<Blok>();
		for (Blok weesblok : weesblokken) {
			if (blockchainBevat(weesblok.getVorigeBlokHash())) {
				verwijder.add(weesblok);
			}
		}
		for (Blok weesblok : verwijder) {
			weesblokken.remove(weesblok);
			voegBlokToe(weesblok);
			System.out.println("Weesblok toegevoegd na laden van nieuwe blockchain");
		}
	}

	/**
	 * Bepaal het saldo van de gebruiker met de gegeven publieke sleutel.
	 * @param publiekeSleutel	Publieke sleutel van de gebruiker waarvan we het saldo willen weten.
	 * @return Saldo van de gebruiker met de gegeven publieke sleutel.
	 */
	public int bepaalSaldo(String publiekeSleutel) {
		int saldo = 0;
		ArrayList<Transactie> utxoTransacties = zoekUTXOs(publiekeSleutel);
		for (Transactie tx : utxoTransacties) {
			for (int i=0; i<tx.getAantalUitvoeren(); i++) {
				Uitvoer uitvoer = tx.getUitvoer(i);
				if (uitvoer.publiekeSleutel.equals(publiekeSleutel)) {
					saldo += uitvoer.bedrag;
					break;
				}
			}
		}
		
		return saldo;
	}
	
	public ArrayList<Transactie> getTransactiePool() {
		return transactiePool;
	}

}
