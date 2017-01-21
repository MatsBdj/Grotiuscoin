package nl.apkbaadjou.grotiuscoin;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

import org.json.JSONObject;

/**
 * De Network-klasse is verantwoordelijk voor:
 * 		-het ontvangen van berichten van peers
 * 		-het sturen van berichten naar peers
 * 		-het wachten op nieuwe peers
 *
 */
public class Network {
	
	/**
	 * Het poortnummer waarop de node luistert.
	 */
	public static final int POORT = 39114; 
	
	/**
	 * In deze lijst staan objecten die melding moeten krijgen van belangrijke gebeurtenissen 
	 * in deze klasse. 
	 * Objecten kunnen zich toevoegen aan/verwijderen van deze lijst met de voegListenerToe()-
	 * en verwijderListener()-methoden
	 * Deze lijst kan door meerdere threads worden aangepast, dus veranderingen dienen
	 * altijd in een synchronized-blok te staan, met 'listeners' als de lock.
	 */
	private ArrayList<NetworkListener> listeners;
	
	/**
	 * Ontvangen berichten worden toegevoegd aan deze queue.
	 * Deze queue kan door meerdere threads worden aangepast, dus
	 * veranderingen dienen altijd in een synchronized-blok te 
	 * staan, met 'ontvangenBerichten' als de lock.
	 */
	private Queue<BerichtAfzenderPaar> ontvangenBerichten;
	
	/**
	 * Peers zijn de nodes in het netwerk waarmee deze node
	 * verbonden is. 
	 * Deze lijst met peers kan door meerdere
	 * threads worden aangepast, dus veranderingen dienen
	 * altijd in een synchronized-blok te staan, met 
	 * 'peers' als lock.
	 */
	private ArrayList<Peer> peers;
	
	/**
	 * Wordt gebruikt om binnenkomende verbindingen te accepteren.
	 */
	private ServerSocket serverSocket;
	
	public Network() {
		listeners = new ArrayList<NetworkListener>();
		ontvangenBerichten = new LinkedList<BerichtAfzenderPaar>();
		peers = new ArrayList<Peer>();
		serverSocket = null;
	}
	
	public void start() {
			
		//deze thread wacht op binnenkomende verbindingen
		Thread binnenkomendeVerbindingThread = new Thread(new Runnable() {
			@Override
			public void run() {
				
				//wacht op nieuwe verbinding		
				try {
					serverSocket = new ServerSocket(POORT);
					System.out.println("Aan het luisteren op poort " + POORT);
			
					//Deze loop eindigt zodra serverSocket.accept() een SocketException 
					//werpt (doordat serverSocket.close() wordt aangeroepen).
					while (true) {
						
						Socket s = serverSocket.accept();
						Peer nieuwePeer = new Peer(s, Network.this);
						
						synchronized (peers) {
							peers.add(nieuwePeer);
						}
						
						synchronized (listeners) {
							for (NetworkListener listener : listeners) {
								listener.nieuwePeer(nieuwePeer);
							}
						}
					}
					
					
				} catch (SocketException se) { 
					//wordt geworpen als ServerSocket.close() wordt aangeroepen
				} catch (IOException e) {
					e.printStackTrace();
				} 
				
			}
		});
		binnenkomendeVerbindingThread.setName("binnenkomendeVerbindingThread");
		binnenkomendeVerbindingThread.start();
	}
	
	/**
	 * Stop alle verbindingen. 
	 */
	public void stop() {
		
		//hier worden alle sockets, streams, etc. gesloten 
		try {
			//ServerSocket.close() zorgt ervoor dat ServerSocket.accept() niet langer 
			//blokkeert en een SocketException werpt.
			//Als close() wordt aangeroepen voor accept(), dan wordt een SocketException
			//geworpen zodra accept() aangeroepen wordt.
			//(Merk op dat het serverSocket-object nu door twee threads wordt 
			// aangepast: de EDT in stop() en door binnenkomendeVerbindingThread in start(). 
			// Synchronisatie is echter niet nodig, aangezien het de bedoeling is dat
			// close() wordt aangeroepen wanneer accept() blokkeert)
			if (serverSocket != null) {
				serverSocket.close();
			}
			
			//stop alle luisterthreads van de peers
			synchronized (peers) {
				for (Peer peer : peers) {
					peer.stopLuisterThread();
				}
			}
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Stuur een bericht naar een Peer.
	 * @param bericht	Bericht dat verstuurd wordt (in de vorm van een JSON-object)
	 * @param ip		IP-adres van de geadresseerde
	 */
	public void stuurBericht(JSONObject bericht, String ip) {
		synchronized (peers) {
			for (Peer peer : peers) {
				if (peer.getIP().equals(ip)) {
					peer.stuurBericht(bericht);
					break;
				} 
			}
		}
	}
	
	/**
	 * Ga een verbinding aan met een andere node.
	 * @param poort		Poortnummer waarop verbonden moet worden.
	 * @param ip		Het IP-adres van de andere node.
	 */
	public void verbindMetPeer(final int poort, final String ip) {
		//probeer met peer te verbinden
		System.out.println("Probeer te verbinden met " + ip + " op poort " + poort);
		Socket socket = null;
		try {
			socket = new Socket(InetAddress.getByName(ip), poort);
			System.out.println("Verbonden met " + socket.getInetAddress());
		
			//voeg de peer toe aan de lijst met peers
			Peer nieuwePeer = new Peer(socket, Network.this);
			
			synchronized (peers) {
				peers.add(nieuwePeer);
			}
			
			synchronized (listeners) {
				for (NetworkListener listener : listeners) {
					listener.nieuwePeer(nieuwePeer);
				}
			}
			
		} catch (UnknownHostException uhe) {
			uhe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} 
	}

	/**
	 * Wordt aangeroepen door een Peer als de verbinding verbroken is.
	 * Wordt aangeroepen op de luisterThread van de Peer.
	 * @param peer	Reference naar de Peer waarvan de verbinding is verbroken.
	 */
	public void verbindingVerbroken(Peer peer) {
		synchronized (peers) {
			peers.remove(peer);
		}
	
		synchronized (listeners) {
			for (NetworkListener listener : listeners) {
				listener.verbindingVerbroken(peer);
			}
		}
	}
	
	/**
	 * Wordt aangeroepen door een Peer als deze een bericht gestuurd heeft.
	 * Wordt aangeroepen op de luisterThread van de Peer.
	 * @param bericht	Het bericht dat de Peer gestuurd heeft.
	 */
	public void ontvangBericht(JSONObject bericht, Peer afzender) {
		synchronized (ontvangenBerichten) {
			ontvangenBerichten.add(new BerichtAfzenderPaar(bericht, afzender));
		}
	}
	
	/**
	 * Voeg een NetworkListener toe aan de lijst met listeners, zodat deze melding krijgt
	 * van belangrijke gebeurtenissen in de Network-klasse.
	 * Let erop dat de listener ook weer verwijderd moet worden van de lijst als hij niet
	 * meer gebruikt wordt; anders kan de garbage collector hem niet opruimen.
	 * @param listener	Een object dat moet 'luisteren' naar gebeurtenissen in de Network-klasse.
	 */
	public void voegListenerToe(NetworkListener listener) {
		if (listener != null) {
			synchronized (listeners) {
				listeners.add(listener);
			}
		} else {
			throw new NullPointerException();
		}
	}
	
	/**
	 * Haal het oudste bericht op (en verwijder het uit de lijst met berichten).
	 * @return	Het oudste bericht in de lijst met ontvangen berichten.
	 */
	public BerichtAfzenderPaar haalBerichtOp() {
		synchronized (ontvangenBerichten) {
			return ontvangenBerichten.poll();
		}
	}
	
	/**
	 * Verwijder een NetworkListener van de lijst met listeners. De NetworkListener zal daarna geen
	 * melding meer krijgen van gebeurtenissen in de Network-klasse.
	 * @param listener	Een object dat niet langer hoeft te 'luisteren' naar gebeurtenissen in de Network-klasse.
	 */
	public void verwijderListener(NetworkListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	public Peer getPeer(int index) {
		synchronized (peers) {
			return peers.get(index);
		}
	}
	
	public int getAantalPeers() {
		synchronized (peers) {
			return peers.size();
		}
	}

}
