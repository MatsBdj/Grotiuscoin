package nl.apkbaadjou.grotiuscoin;
import org.json.JSONObject;


/**
 * Een uitvoer is een deel van een transactie dat aangeeft hoeveel geld wordt
 * gestuurd en naar wie.
 *
 */
public class Uitvoer {
	
	/**
	 * Het bedrag dat wordt verstuurd.
	 */
	public int bedrag;
	
	/**
	 * De publieke sleutel van de ontvanger van het geld.
	 */
	public String publiekeSleutel;
	
	public Uitvoer(int bedrag, String publiekeSleutel) {
		this.bedrag = bedrag;
		this.publiekeSleutel = publiekeSleutel;
	}
	
	public Uitvoer(JSONObject obj) {
		bedrag = obj.getInt("bedrag");
		publiekeSleutel = obj.getString("publiekeSleutel");
	}
	
	/**
	 * @return De data van deze uitvoer in JSON-formaat.
	 */
	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		obj.put("bedrag", bedrag);
		obj.put("publiekeSleutel", publiekeSleutel);
		return obj;
	}
}
