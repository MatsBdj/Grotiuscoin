package nl.apkbaadjou.grotiuscoin;

/**
 * Implementaties van GuiHandler handelen UI-events af.
 *
 */
public interface GuiHandler {
	
	/**
	 * Stuur coins naar een bepaald adres.
	 * @param adres		Het adres waar de coins naartoe gestuurd moeten worden.
	 * @param aantal 	Het aantal coins dat gestuurd moet worden.
	 */
	public void stuurCoins(String adres, int aantal);
	
	public void sluitVenster();
	
	/**
	 * Verbind met een andere node in het netwerk.
	 * @param poort 	Poortnummer waarop de node luistert.
	 * @param ip		IP-adres van de node.
	 */
	public void verbindMetPeer(int poort, String ip);

}
