package nl.apkbaadjou.grotiuscoin;
import org.json.JSONObject;


/**
 * Deze klasse bevat statische methoden die het makkelijker maken om 
 * berichten te maken en te lezen.
 * 
 * Elk bericht heeft een 'soort'-attribuut dat aangeeft wat voor soort
 * bericht het is (bv. een transactie, een blok, etc.). De rest van de
 * inhoud van het bericht is afhankelijk van het soort bericht.
 *
 */
public class BerichtUtil {
	
	//wordt gebruikt om transacties te sturen
	public static JSONObject maakTransactieBericht(Transactie transactie) {
		JSONObject bericht = new JSONObject();
		bericht.put("soort", "transactie");
		bericht.put("transactiejson", transactie.toJSON().toString());
		return bericht;
	}
	
	//dit type bericht wordt gebruikt om blokken te sturen
	public static JSONObject maakBlokBericht(Blok blok) {
		JSONObject bericht = new JSONObject();
		bericht.put("soort", "blok");
		bericht.put("blokjson", blok.toJSON().toString());
		return bericht;
	}
	
	//Een blokhoogtebericht bevat de blokhoogte van de blockchain van een peer.
	//Blokhoogteberichten worden aan het begin van de verbinding verstuurd zodat
	//peers van elkaar weten wie van de twee de meeste blokken heeft. 
	public static JSONObject maakBlokhoogteBericht(int blokhoogte) {
		JSONObject bericht = new JSONObject();
		bericht.put("soort", "blokhoogte");
		bericht.put("blokhoogte", blokhoogte);
		return bericht;
	}
	
	//Een blockchainbericht bevat de hele blockchain, en wordt gestuurd naar
	//nodes die achter liggen op de rest van het netwerk.
	public static JSONObject maakBlockchainBericht(String blockchain) {
		JSONObject bericht = new JSONObject();
		bericht.put("soort", "blockchain");
		bericht.put("blockchain", blockchain);
		return bericht;
	}
	

}
