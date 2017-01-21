package nl.apkbaadjou.grotiuscoin;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

/**
 * Een Peer is een node die verbonden is met deze node.
 *
 */
public class Peer {
	
	/**
	 * Het IP-adres van deze Peer.
	 */
	private String ip;
	
	/**
	 * Het poortnummer dat deze Peer gebruikt.
	 */
	private int poort;
	

	/**
	 * De Socket waarmee deze Peer verbonden is.
	 */
	private Socket socket;
	
	/**
	 * Deze PrintWriter wordt gebruikt om berichten te sturen naar deze Peer.
	 * Meerdere threads kunnen toegang krijgen tot deze writer, dus elk stuk code
	 * dat er gebruik van maakt, dient in een synchronized-blok te staan met 
	 * 'writer' als de lock.
	 */
	private PrintWriter writer;
	
	/**
	 * Reference naar de instantie van Network waarbij deze Peer hoort.
	 */
	private Network network;
	
	/**
	 * Op deze thread worden berichten ontvangen van deze Peer.
	 */
	private Thread luisterThread;
	
	
	/**
	 * Wordt aangeroepen op nieuweVerbindingThread.
	 * @param socket				De Socket die verbonden is aan deze peer.
	 * @param ontvangenBerichten	Reference naar de berichtenqueue waar ontvangen berichten in opgeslagen moeten worden.
	 */
	public Peer(Socket socket, Network network) {
		this.socket = socket;
		this.network = network;

		ip = socket.getInetAddress().getHostName();
		poort = socket.getPort();
		
		try {
			writer = new PrintWriter(socket.getOutputStream(), true);
			
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		
		luisterThread = new Thread(new Runnable() {
			@Override
			public void run() {
				
				try {
					
					//ontvang berichten van deze Peer
					BufferedReader reader = new BufferedReader(new InputStreamReader(
												Peer.this.socket.getInputStream(), StandardCharsets.UTF_8));

					String regel;
					while ((regel = reader.readLine()) != null) {
						Peer.this.network.ontvangBericht(new JSONObject(regel), Peer.this);
					}

				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						if (Peer.this.socket != null) {
							Peer.this.socket.close();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				 
					//laat network weten dat deze peer niet meer verbonden is
					Peer.this.network.verbindingVerbroken(Peer.this);
				}
				
			}
		});
		luisterThread.setName("luisterThread " + ip);
		luisterThread.start();
	}
	
	/**
	 * Stuur een bericht naar deze Peer.
	 * @param json	JSONObject dat de informatie van dit bericht bevat (gebruik de BerichtUtil-klasse om berichten te maken)
	 */
	public void stuurBericht(JSONObject json) {
		synchronized (writer) {
			writer.println(json.toString());
			writer.flush();
		}
	}
	
	/**
	 * Stop de luisterThread
	 */
	public void stopLuisterThread() {
		
		try {
			//sluit socket
			//(Merk op dat we de inputstream niet hoeven te sluiten, dat
			// gebeurt automatisch als de socket wordt gesloten)
			if (socket != null) {
				socket.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public String getIP() {
		return ip;
	}
	
	public int getPoort() {
		return poort;
	}
}
