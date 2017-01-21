package nl.apkbaadjou.grotiuscoin;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;


/**
 * In een Blok worden transacties opgeslagen. Elk Blok bevat een
 * verwijzing naar het vorige Blok, waardoor de blokken een keten 
 * vormen.
 *
 */
public class Blok {
	
	/**
	 * Hash van het vorige blok.
	 */
	private String vorigeBlokHash;
	
	/**
	 * Tijdstip waarop dit blok is gemaakt (gemeten in milliseconden sinds Unix Epoch).
	 * Wordt gebruikt voor het bepalen van de difficulty target.
	 */
	private long timestamp;
	
	/**
	 * De 'nonce' is een willekeurig getal dat wordt toegevoegd aan het blok, 
	 * om ervoor te zorgen dat de hashwaarde van het blok kleiner is dan de target.
	 */
	private int nonce;
	
	/**
	 * Om geldig te zijn moet de hashwaarde van dit blok onder de target liggen.
	 * De target wordt bepaald door de hoeveelheid tijd die nodig is geweest om 
	 * de voorgaande blokken te produceren.
	 * De target mag niet hoger zijn dan BlockchainManager.MAX_TARGET.
	 */
	private String target;
	
	/**
	 * De transacties die dit blok bevat.
	 */
	private ArrayList<Transactie> transacties;
	
	public Blok(String vorigeBlokHash, long timestamp) {
		this.vorigeBlokHash = vorigeBlokHash;
		this.timestamp = timestamp;
		nonce = 0;
		target = BlockchainManager.MAX_TARGET;
		transacties = new ArrayList<Transactie>();
	}
	
	public Blok(JSONObject obj) {		
		vorigeBlokHash = obj.getString("vorigeBlokHash");
		timestamp = obj.getLong("timestamp");
		nonce = obj.getInt("nonce");
		target = obj.getString("target");
				
		transacties = new ArrayList<Transactie>();
		JSONArray txArray = obj.getJSONArray("transacties");
		
		for (int i=0; i<txArray.length(); i++) {		
			JSONObject txObj = txArray.getJSONObject(i);;	
			Transactie transactie = new Transactie(txObj);
			transacties.add(transactie);
		}
	}
	
	/**
	 * @return hash van de data van dit blok
	 */
	public String getHash() {
		StringBuilder hashString = new StringBuilder();
		hashString.append(vorigeBlokHash);
		hashString.append(timestamp);
		hashString.append(nonce);
		hashString.append(target);
		for (Transactie tx : transacties) {
			hashString.append(tx.getHash());
		}
		return Util.getSha256Hash(hashString.toString());
	}
	
	public String getVorigeBlokHash() {
		return vorigeBlokHash;
	}
	
	public long getTimestamp() {
		return timestamp;
	}
	
	public int getNonce() {
		return nonce;
	}
	
	public String getTarget() {
		return target;
	}
	
	/**
	 * Zoek in dit blok naar de transactie met de gegeven hash.
	 * @param hash	Hash van de gezochte transactie.
	 * @return De gezochte transactie (of null als de transactie niet in het blok zit).
	 */
	public Transactie getTransactie(String hash) {
		
		for (Transactie t : transacties) {
			if (t.getHash().equals(hash)) {
				return t;
			}
		}
		
		//niets gevonden
		return null;
	}
	
	public ArrayList<Transactie> getTransacties() {
		return transacties;
	}
	
	public void setVorigeBlokHash(String vorigeBlokHash) {
		this.vorigeBlokHash = vorigeBlokHash;
	}
	
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
	public void setNonce(int nonce) {
		this.nonce = nonce;
	}
	
	public void setTarget(String target) {
		this.target = target;
	}
	
	public void voegTransactieToe(Transactie transactie) {
		transacties.add(transactie);
	}
	
	@Override
	public boolean equals(Object o) {
		
		if (!(o instanceof Blok)){
			return false;
		}
		
		Blok b = (Blok) o;
		
		return (this.getHash().equals(b.getHash()));
	}
	
	
	
	/**
	 * @return De data van dit blok in JSON-formaat.
	 */
	public JSONObject toJSON() {
		
		JSONArray txArray = new JSONArray();
		for (Transactie tx : transacties) {
			txArray.put(tx.toJSON());
		}
		
		JSONObject obj = new JSONObject();
		obj.put("vorigeBlokHash", vorigeBlokHash);
		obj.put("timestamp", timestamp);
		obj.put("nonce", nonce);
		obj.put("target", target);
		obj.put("transacties", txArray);
		return obj;
	}
	
	

}
