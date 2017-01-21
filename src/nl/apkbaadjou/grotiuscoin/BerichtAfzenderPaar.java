package nl.apkbaadjou.grotiuscoin;
import org.json.JSONObject;


/**
 * Bevat een bericht en de afzender van dat bericht.
 *
 */
public class BerichtAfzenderPaar {
	public JSONObject bericht;
	public Peer afzender;
	
	public BerichtAfzenderPaar(JSONObject bericht, Peer peer) {
		this.bericht = bericht;
		this.afzender = peer;
	}

}
