package nl.apkbaadjou.grotiuscoin;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Een Transactie bevat een reeks invoeren en uitvoeren.
 *
 */
public class Transactie {
	
	private ArrayList<Invoer> invoeren;
	private ArrayList<Uitvoer> uitvoeren;
	
	public Transactie() {
		invoeren = new ArrayList<Invoer>();
		uitvoeren = new ArrayList<Uitvoer>();
	}
	
	public Transactie(JSONObject txObj) {
		invoeren = new ArrayList<Invoer>();
		uitvoeren = new ArrayList<Uitvoer>();
		
		JSONArray inArray = txObj.getJSONArray("invoeren");
		JSONArray uitArray = txObj.getJSONArray("uitvoeren");

		for (int i=0; i<inArray.length(); i++) {
			JSONObject inObj = inArray.getJSONObject(i);
			invoeren.add(new Invoer(inObj));
		}
		
		for (int i=0; i<uitArray.length(); i++) {
			JSONObject uitObj = uitArray.getJSONObject(i);	
			uitvoeren.add(new Uitvoer(uitObj));
		}
	}
	
	public void voegInvoerToe(Invoer invoer) {
		invoeren.add(invoer);
	}
	
	public void voegUitvoerToe(Uitvoer uitvoer) {
		uitvoeren.add(uitvoer);
	}
	
	public Invoer getInvoer(int index) {
		return invoeren.get(index);
	}
	
	public Uitvoer getUitvoer(int index) {
		return uitvoeren.get(index);
	}
	
	public int getAantalInvoeren() {
		return invoeren.size();
	}
	
	public int getAantalUitvoeren() {
		return uitvoeren.size();
	}
	
	/**
	 * @return De hashwaarde van deze transactie.
	 */
	public String getHash() {

		StringBuilder hashString = new StringBuilder();
		for (Invoer invoer : invoeren) {
			hashString.append(invoer.hashVorigeTransactie);
			hashString.append(invoer.uitvoerIndex);
			//laat de handtekening weg
		}
		for (Uitvoer uitvoer : uitvoeren) {
			hashString.append(uitvoer.bedrag);
			hashString.append(uitvoer.publiekeSleutel);
		}
		return Util.getSha256Hash(hashString.toString());
	}
	
	/**
	 * @return De data van deze transactie in JSON-formaat.
	 */
	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();
		
		JSONArray inArray = new JSONArray();
		for (Invoer invoer : invoeren) {
			inArray.put(invoer.toJSON());
		}
		
		JSONArray uitArray = new JSONArray();
		for (Uitvoer uitvoer : uitvoeren) {
			uitArray.put(uitvoer.toJSON());
		}
		
		obj.put("invoeren", inArray);
		obj.put("uitvoeren", uitArray);
		return obj;
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (!(o instanceof Transactie)){
			return false;
		}
		
		Transactie t = (Transactie) o;
		
		return (this.getHash().equals(t.getHash()));
	}
}
