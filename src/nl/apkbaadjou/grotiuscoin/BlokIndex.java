package nl.apkbaadjou.grotiuscoin;

/**
 * Een BlokIndex vormt een knoop in de boomstructuur van de blockchain.
 * Elk BlokIndex bevat een verwijzing naar het voorgaande en het volgende blok.
 * De verwijzing naar het volgende blok wijst altijd naar de langste keten.
 *
 */
public class BlokIndex {
	
	private BlokIndex vorigeBlokIndex;
	private BlokIndex volgendeBlokIndex;	//is null als er geen volgend blok is
											//bij blokken in de zijketen is er geen garantie dat de volgendeBlokIndex klopt
	private Blok blok;
	
	public BlokIndex(Blok blok, BlokIndex vorigeBlokIndex, BlokIndex volgendeBlokIndex) {
		this.blok = blok;
		this.vorigeBlokIndex = vorigeBlokIndex;
		this.volgendeBlokIndex = volgendeBlokIndex;
	}
	
	public BlokIndex getVorigeBlokIndex() {
		return vorigeBlokIndex;
	}
	
	public BlokIndex getVolgendeBlokIndex() {
		return volgendeBlokIndex;
	}
	
	public Blok getBlok() {
		return blok;
	}
	
	public void setVorigeBlokIndex(BlokIndex vorigeBlokIndex) {
		this.vorigeBlokIndex = vorigeBlokIndex;
	}
	
	public void setVolgendeBlokIndex(BlokIndex volgendeBlokIndex) {
		this.volgendeBlokIndex = volgendeBlokIndex;
	}

}
