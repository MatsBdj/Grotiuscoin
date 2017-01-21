package nl.apkbaadjou.grotiuscoin;
import org.json.JSONObject;


/**
 * Een invoer verwijst naar de uitvoer van een voorgaande transactie.
 * De invoeren bepalen hoeveel geld de transactie kan besteden.
 *
 */
public class Invoer {
	
	/**
	 * De hashwaarde van de transactie die de uitvoer bevat waar deze invoer naar verwijst.
	 */
	public String hashVorigeTransactie;
	
	/**
	 * De index van de uitvoer waar deze invoer naar verwijst (een transactie kan meerdere uitvoeren hebben).
	 */
	public int uitvoerIndex;
	
	/**
	 * Een digitale handtekening die bevestigt dat de eigenaar van het geld de transactie heeft goedgekeurd.
	 */
	public String handtekening;
	
	public Invoer(String hashVorigeTransactie, int uitvoerIndex) {
		this.hashVorigeTransactie = hashVorigeTransactie;
		this.uitvoerIndex = uitvoerIndex;
		handtekening = null;	//handtekening wordt pas later toegevoegd
	}
	
	public Invoer(JSONObject obj) {
		hashVorigeTransactie = obj.getString("hashVorigeTransactie");
		uitvoerIndex = obj.getInt("uitvoerIndex");
		handtekening = obj.getString("handtekening");
	}
	
	/**
	 * @return De data van deze invoer in JSON-formaat.
	 */
	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		obj.put("hashVorigeTransactie", hashVorigeTransactie);
		obj.put("uitvoerIndex", uitvoerIndex);
		obj.put("handtekening", handtekening);
		return obj;
	}
}
