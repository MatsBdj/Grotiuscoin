package nl.apkbaadjou.grotiuscoin;

import java.io.File;
import java.util.ArrayList;

import javax.swing.SwingUtilities;
import org.json.JSONObject;


public class Main implements GuiHandler, NetworkListener {	
	
	/**
	 * Directory waarin bestanden van dit programma worden opgeslagen.
	 */
	public static final String DIRECTORY = System.getProperty("user.home") + File.separator + "grotiuscoin";
	
	public static final String COIN_NAAM = "Grotiuscoin";
	
	private Gui gui;
	private Network network;
	private BlockchainManager blockchainManager;
	private Wallet wallet;
	
	/**
	 * Geeft aan of de main thread door moet blijven gaan.
	 * Zodra te gebruiker het scherm probeert te sluiten, 
	 * wordt deze waarde op false gezet.
	 */
	private boolean doorgaan;
	
	public static void main(String[] args) {
		Main main = new Main();
		main.start();
	}
	
	public void start() {
		doorgaan = true;
		
		//initialiseer wallet
		wallet = new Wallet();
		wallet.initWallet();
		
		//initialiseer de blockchain
		blockchainManager = new BlockchainManager(wallet.getPubliekeSleutel());
		blockchainManager.initBlockchain();

		//luister naar peers
		network = new Network();
		network.voegListenerToe(this);
		network.start();
		
		//maak de UI (op de Event Dispatch Thread)
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				gui = new Gui(Main.this);
				gui.updateMijnAdres(wallet.getPubliekeSleutel());
				gui.updateSaldo(blockchainManager.bepaalSaldo(wallet.getPubliekeSleutel()));
			}
		});	
	
		//deze loop stopt wanneer de gebruiker het venster probeert te sluiten
		while (doorgaan) {
			
			//verwerk ontvangen berichten
			BerichtAfzenderPaar bap;
			while ((bap = network.haalBerichtOp()) != null) {
				String soort = bap.bericht.getString("soort");
				
				if (soort.equals("transactie")) {
					
					System.out.println("transactie ontvangen: " + bap.bericht.getString("transactiejson"));
					Transactie transactie = new Transactie(new JSONObject(bap.bericht.getString("transactiejson")));
					
					//voeg de transactie toe aan de transactiePool
					if (blockchainManager.voegTransactieToe(transactie)) {
						
						//transactie is geldig; stuur door naar alle peers
						JSONObject transactiebericht = BerichtUtil.maakTransactieBericht(transactie);
						for (int i=0; i<network.getAantalPeers(); i++) {	
							Peer p = network.getPeer(i);
							network.stuurBericht(transactiebericht, p.getIP());		
						}
					}	
				} 
				else if (soort.equals("blok")) {
					//controleer het ontvangen blok en voeg het toe aan de blockchain
					System.out.println("blok ontvangen: " + bap.bericht.getString("blokjson"));
					Blok blok = new Blok(new JSONObject(bap.bericht.getString("blokjson")));
					
					if (blockchainManager.voegBlokToe(blok)) {
						//door het nieuwe blok is het saldo van de gebruiker misschien veranderd; update gui
						gui.updateSaldo(blockchainManager.bepaalSaldo(wallet.getPubliekeSleutel()));
						
					}
				} else if (soort.equals("blokhoogte")) {
					//als de andere node een lagere blokhoogte heeft, stuur hem dan de blockchain 
					System.out.println("blokhoogte peer: " + bap.bericht.getInt("blokhoogte"));
					if (bap.bericht.getInt("blokhoogte") < blockchainManager.getBlokhoogte()) {
						bap.afzender.stuurBericht(BerichtUtil.maakBlockchainBericht(blockchainManager.blockchainNaarString()));
					}
				} else if (soort.equals("blockchain")) {
					//neem de ontvangen blockchain over
					System.out.println("blockchain ontvangen");
					blockchainManager.vervangBlockchain(bap.bericht.getString("blockchain"));
				}
			}
			
			//probeer een blok te genereren
			Blok nieuwBlok;
			if ((nieuwBlok = blockchainManager.mine()) != null) {
				
				//stuur blok door naar peers
				for (int i=0; i<network.getAantalPeers(); i++) {
					network.stuurBericht(BerichtUtil.maakBlokBericht(nieuwBlok), network.getPeer(i).getIP());
					System.out.println("blok verstuurd");
				}
				
				//het saldo van de gebruiker is misschien veranderd; update gui
				gui.updateSaldo(blockchainManager.bepaalSaldo(wallet.getPubliekeSleutel()));
			}
		}
		
		//sla de blockchain op
		blockchainManager.slaBlockchainOp();

	}

	/**
	 * Wordt aangeroepen door gui als de gebruiker geld wil versturen.
	 * Wordt uitgevoerd op Event Dispatch Thread
	 */
	@Override
	public void stuurCoins(final String adres, final int aantal) {

		//start nieuwe Thread zodat de Event Dispatch Thread niet hoeft te wachten
		new Thread(new Runnable() {
			@Override
			public void run() {
				
				//maak een nieuwe transactie
				Transactie nieuweTransactie = new Transactie();
				nieuweTransactie.voegUitvoerToe(new Uitvoer(aantal, adres));
				
				//zoek UTXO's in de blockchain
				ArrayList<Transactie> utxoTransacties = blockchainManager.zoekUTXOs(wallet.getPubliekeSleutel());
				
				//verwijder UTXO's die al gebruikt worden door een transactie in de transactiePool
				ArrayList<Transactie> verwijder = new ArrayList<Transactie>();
				for (Transactie utxoTx : utxoTransacties) {
					for (int i=0; i<utxoTx.getAantalUitvoeren(); i++) {
						Uitvoer uitvoer = utxoTx.getUitvoer(i);
						if (uitvoer.publiekeSleutel.equals(wallet.getPubliekeSleutel())) {
							
							for (Transactie poolTx : blockchainManager.getTransactiePool()) {
								for (int j=0; j<poolTx.getAantalInvoeren(); j++) {
									Invoer invoer = poolTx.getInvoer(j);
									if (invoer.uitvoerIndex == i) {
										if (invoer.hashVorigeTransactie.equals(utxoTx.getHash())) {
											//uitvoer wordt al uitgegeven door een transactie in de transactiepool
											if (!verwijder.contains(utxoTx)) {
												verwijder.add(utxoTx);
												break;
											}
										}
									}
								}
							}			
							break;
						}
					}
				}
				for (Transactie t : verwijder) {
					utxoTransacties.remove(t);
				}
				
				//bepaal hoeveel UTXO's nodig zijn om het gewenste bedrag te kunnen uitgeven
				int nogTeBetalen = aantal;
				while (nogTeBetalen > 0) {
					
					if (utxoTransacties.size() == 0) {
						//Geen UTXO's meer over: de gebruiker heeft niet genoeg geld voor de transactie of
						//er zijn tijdelijk geen transactie-invoeren beschikbaar.
						if (aantal > blockchainManager.bepaalSaldo(wallet.getPubliekeSleutel())) {
							gui.toonMelding("Er is niet genoeg geld beschikbaar.");
						} else {
							gui.toonMelding("Er is tijdelijk geen transactie-invoer beschikbaar. Wacht enkele minuten en probeer het dan opnieuw.");
						}
						return;
					}
					
					Transactie utxoTx = utxoTransacties.remove(utxoTransacties.size()-1);
					Uitvoer uit = null;
					int uitvoerIndex = 0;
					
					//zoek de juiste uitvoer
					for (int i=0; i<utxoTx.getAantalUitvoeren(); i++) {
						uit = utxoTx.getUitvoer(i);
						uitvoerIndex = i;
						if (utxoTx.getUitvoer(i).publiekeSleutel.equals(wallet.getPubliekeSleutel())) {
							break;
						}
					}
					
					nogTeBetalen -= uit.bedrag;
					nieuweTransactie.voegInvoerToe(new Invoer(utxoTx.getHash(), uitvoerIndex));	
				}
				
				//als er te veel betaald is, stuur dan wisselgeld terug naar de eigen publieke sleutel
				int wisselgeld = -nogTeBetalen;
				if (wisselgeld > 0) {
					nieuweTransactie.voegUitvoerToe(new Uitvoer(wisselgeld, wallet.getPubliekeSleutel()));
				}
				
				//onderteken de transactie
				nieuweTransactie = wallet.ondertekenTransactie(nieuweTransactie);
				
				//voeg de transactie toe aan de transactiePool
				if (blockchainManager.voegTransactieToe(nieuweTransactie)) {

					//transactie is geldig; stuur door naar alle peers
					JSONObject transactiebericht = BerichtUtil.maakTransactieBericht(nieuweTransactie);
					for (int i=0; i<network.getAantalPeers(); i++) {	
						Peer p = network.getPeer(i);
						network.stuurBericht(transactiebericht, p.getIP());		
					}
					
					gui.toonMelding("Transactie is verstuurd en zal over enkele minuten zijn verwerkt.");
				}
			}	
		}).start();

	}

	/**
	 * Wordt aangeroepen door gui als de gebruiker het venster probeert te sluiten.
	 * Wordt uitgevoerd op Event Dispatch Thread
	 */
	@Override
	public void sluitVenster() {
		//Verberg en verwijder de JFrame van de gui, zodat de EDT kan stoppen.
		//(zie https://docs.oracle.com/javase/8/docs/api/java/awt/doc-files/AWTThreadIssues.html#Autoshutdown)
		gui.dispose();

		//Laat alle niet-Daemon threads stoppen, zodat het programma stopt.
		network.stop();
		doorgaan = false;	//zorgt ervoor dat main thread stopt
	}

	/**
	 * Wordt aangeroepen door gui als de gebruiker met een peer wil verbinden.
	 * Wordt uitgevoerd op Event Dispatch Thread
	 */
	@Override
	public void verbindMetPeer(final int poort, final String ip) {

		
		//start nieuwe Thread zodat de Event Dispatch Thread niet hoeft te wachten
		new Thread(new Runnable() {
			@Override
			public void run() {
				network.verbindMetPeer(poort, ip);
			}	
		}).start();
	}

	/**
	 * Wordt aangeroepen door network als een nieuwe peer verbindt
	 */
	@Override
	public void nieuwePeer(Peer peer) {
		updatePeerLijst();
		
		//stuur de blokhoogte van deze node naar de nieuwe peer
		peer.stuurBericht(BerichtUtil.maakBlokhoogteBericht(blockchainManager.getBlokhoogte()));
		
		System.out.println("blokhoogte verstuurd");
	}

	/**
	 * Wordt aangeroepen door network als een peer de verbinding verbreekt.
	 */
	@Override
	public void verbindingVerbroken(Peer peer) {
		updatePeerLijst();
	}
	
	/**
	 * Update de lijst met peers die wordt weergegeven in de GUI.
	 */
	private void updatePeerLijst() {
		String[] peerInfo = new String[network.getAantalPeers()];
		for (int i=0; i<network.getAantalPeers(); i++) {
			Peer p = network.getPeer(i);
			peerInfo[i] = p.getIP();
		}
		gui.updatePeerLijst(peerInfo);
	}

}
