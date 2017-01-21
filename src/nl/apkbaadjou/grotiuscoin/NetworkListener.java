package nl.apkbaadjou.grotiuscoin;


/**
 * Implementaties van NetworkListener krijgen melding van belangrijke gebeurtenissen in de Network-klasse
 *
 */
public interface NetworkListener {
	
	/**
	 * Wordt aangeroepen als een nieuwe Peer verbindt.
	 * @param peer	De nieuwe peer.
	 */
	public void nieuwePeer(Peer peer);
	
	/**
	 * Wordt aangeroepen als een Peer de verbinding heeft verbroken.
	 * @param peer	De peer die de verbinding heeft verbroken.
	 */
	public void verbindingVerbroken(Peer peer);
}
