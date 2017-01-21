package nl.apkbaadjou.grotiuscoin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


/**
 * Deze klasse is verantwoordelijk voor de grafische interface.
 * BELANGRIJK: -Alle code die een Swing-component aanpast of afhankelijk
 * 			    is van de toestand van een Swing-component, mag alleen op
 * 			    de Event Dispatch Thread (EDT) worden uitgevoerd. 
 * 
 * 			   -Om code uit te voeren op te EDT gebruik je 
 * 			    SwingUtilities.invokeLater() of SwingUtilities.invokeAndWait().
 * 
 * 			   -Op de EDT mogen alleen korte taken uitgevoerd worden, anders
 * 				hangt de UI vast. Langere taken dienen op een andere thread
 * 				uitgevoerd te worden.
 * 
 * 			   -Als de code van een event listener een Swing-component aanpast, 
 * 				moet deze code uitgevoerd worden via 
 * 				SwingUtilities.invokeLater(). Het is namelijk mogelijk dat er
 * 				andere event listeners zijn, die afhankelijk zijn van de 
 * 				toestand van deze component. Door invokeLater() te gebruiken,
 * 				zorg je ervoor dat de toestand van de component pas verandert
 * 				nadat alle event listeners zijn afgehandeld.
 * 				(zie http://www.pushing-pixels.org/2007/12/06/unwritten-rule-of-working-with-swings-edt.html)
 *
 */
public class Gui extends JFrame {
	
	public static final int BREEDTE = 700;
	public static final int HOOGTE = 480;
	
	/**
	 * De GuiHandler handelt de UI-events af.
	 */
	private GuiHandler guiHandler;
	
	private JLabel mijnPortemonneeLabel;
	private JLabel mijnAdresTekstLabel;
	private JTextArea mijnAdresTextArea;
	private JLabel saldoTekstLabel;
	private JLabel saldoLabel;
	
	private JLabel stuurNaarAdresLabel;
	private JLabel adresTekstLabel;
	private JTextField adresTextField;
	private JLabel aantalLabel;
	private JTextField aantalTextField;
	
	private JButton verstuurButton;
	
	private JLabel verbindIpLabel;
	private JTextField verbindIpTextField;
	private JButton verbindMetPeerButton;
	
	private JLabel peersLabel;
	private JTextArea peerLijst;
	private JScrollPane scrollPane;
	
	public Gui(GuiHandler guiHandler) {
		this.guiHandler = guiHandler;
		
		JPanel paneel = new JPanel();
		paneel.setLayout(null);
		
		mijnPortemonneeLabel = new JLabel("Mijn Portemonnee");
		mijnPortemonneeLabel.setSize(200,20);
		mijnPortemonneeLabel.setLocation(50,40);
		
		mijnAdresTekstLabel = new JLabel("Mijn adres: ");
		mijnAdresTekstLabel.setSize(200, 20);
		mijnAdresTekstLabel.setLocation(70, 70);

		
		mijnAdresTextArea = new JTextArea();
		mijnAdresTextArea.setSize(480, 50);
		mijnAdresTextArea.setLocation(170, 70);
		mijnAdresTextArea.setEditable(false);
		mijnAdresTextArea.setLineWrap(true);
		mijnAdresTextArea.setOpaque(false);
		
		saldoTekstLabel = new JLabel("Saldo: ");
		saldoTekstLabel.setSize(200,20);
		saldoTekstLabel.setLocation(70,120);
		
		saldoLabel = new JLabel("0 " + Main.COIN_NAAM + "s");
		saldoLabel.setSize(200,20);
		saldoLabel.setLocation(170,120);

		stuurNaarAdresLabel = new JLabel("Stuur " + Main.COIN_NAAM + "s naar adres");
		stuurNaarAdresLabel.setSize(250,20);
		stuurNaarAdresLabel.setLocation(50,180);
		
		adresTekstLabel = new JLabel("Adres: ");
		adresTekstLabel.setSize(200,20);
		adresTekstLabel.setLocation(70,210);
		
		adresTextField = new JTextField();
		adresTextField.setSize(250,20);
		adresTextField.setLocation(170,210);
		
		aantalLabel = new JLabel("Bedrag:");
		aantalLabel.setSize(200,20);
		aantalLabel.setLocation(70,235);
		
		aantalTextField = new JTextField();
		aantalTextField.setSize(250,20);
		aantalTextField.setLocation(170,235);
		
		verstuurButton = new JButton("Verstuur");
		verstuurButton.setSize(100,25);
		verstuurButton.setLocation(320, 265);
		verstuurButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
			
				//controleer invoer
				String adres = adresTextField.getText();
				if (adres.equals("")) {
					JOptionPane.showMessageDialog(null, "Vul een adres in.");
					return;
				} 
				
				int aantal = 0;
				try {
					aantal = Integer.valueOf(aantalTextField.getText());
				} catch (NumberFormatException nfe) {}
				
				
				if (aantal <= 0) {
					JOptionPane.showMessageDialog(null, "Vul een geldig aantal " + Main.COIN_NAAM + "s in.");
					return;
				}
				
				//vraag om een bevestiging 
				int ret = JOptionPane.showConfirmDialog(null, "Weet u zeker dat u " + aantal + " " + Main.COIN_NAAM + "s wilt versturen naar adres " 
						+ adres + "?", "Transactie bevestigen", JOptionPane.OK_CANCEL_OPTION);
				
				if (ret == JOptionPane.OK_OPTION) {
	
					adresTextField.setText("");
					aantalTextField.setText("");
					
					//geef door aan GuiHandler
					Gui.this.guiHandler.stuurCoins(adres, aantal);
					
				}			
			}
		});
		
		verbindIpLabel = new JLabel("Verbind met IP-adres:");
		verbindIpLabel.setSize(270, 20);
		verbindIpLabel.setLocation(50, 325);
		
		verbindIpTextField = new JTextField("");
		verbindIpTextField.setSize(200, 20);
		verbindIpTextField.setLocation(220, 325);
		
		verbindMetPeerButton = new JButton("Verbind");
		verbindMetPeerButton.setSize(100,25);
		verbindMetPeerButton.setLocation(320, 350);
		verbindMetPeerButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				if (verbindIpTextField.getText().length() == 0) {
					toonMelding("Vul een IP-adres in.");
					return;
				}
				//geef door aan GuiHandler
				Gui.this.guiHandler.verbindMetPeer(Network.POORT, verbindIpTextField.getText());
			}
		});
		
		peersLabel = new JLabel("Peers");
		peersLabel.setSize(200, 20);
		peersLabel.setLocation(450, 180);
		
		peerLijst = new JTextArea();
		peerLijst.setEditable(false);
		scrollPane = new JScrollPane(peerLijst, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setBounds(450,210,220,200);
		
		paneel.add(mijnPortemonneeLabel);
		paneel.add(mijnAdresTekstLabel);
		paneel.add(mijnAdresTextArea);
		paneel.add(saldoTekstLabel);
		paneel.add(saldoLabel);
		paneel.add(stuurNaarAdresLabel);
		paneel.add(adresTekstLabel);
		paneel.add(adresTextField);
		paneel.add(aantalLabel);
		paneel.add(aantalTextField);
		paneel.add(verstuurButton);
		paneel.add(verbindIpLabel);
		paneel.add(verbindIpTextField);
		paneel.add(verbindMetPeerButton);
		paneel.add(peersLabel);
		paneel.add(scrollPane);

		setSize(BREEDTE, HOOGTE);
		setResizable(false);
		setLocationRelativeTo(null);
		setTitle("Grotiuscoin");
		setContentPane(paneel);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);	//GuiHandler is verantwoordelijk voor het sluiten van het venster
		setVisible(true);	
	}
	
	@Override
	protected void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID() == WindowEvent.WINDOW_CLOSING) {
			guiHandler.sluitVenster();
		}
	}
	
	/**
	 * Update de lijst met peers
	 * @param peerinfo		Informatie over peers (bv. IP-adres)
	 */
	public void updatePeerLijst(String[] peerinfo) {
		peerLijst.setText("");
		for (String info : peerinfo) {
			peerLijst.append(info + "\n");
		}
		
	}
	
	/**
	 * Update het weergegeven saldo.
	 * @param saldo		Het saldo dat moet worden weergegeven.
	 */
	public void updateSaldo(final int saldo) {
		SwingUtilities.invokeLater(new Runnable() {		
			public void run() {	
				saldoLabel.setText(String.valueOf(saldo) + " " + Main.COIN_NAAM + "s");
			}
		});
	}
	
	/**
	 * Update het weergegeven adres van de gebruiker.
	 * @param adres	Het adres dat moet worden weergegeven.
	 */
	public void updateMijnAdres(final String adres) {
		SwingUtilities.invokeLater(new Runnable() {		
			public void run() {	
				mijnAdresTextArea.setText(adres);
			}
		});
	}
	
	/**
	 * Open een schermpje dat een melding toont.
	 * @param tekst	De tekst die vermeld wordt.
	 */
	public void toonMelding(final String tekst) {
		SwingUtilities.invokeLater(new Runnable() {		
			public void run() {	
				JOptionPane.showMessageDialog(null, tekst);
			}
		});
	}

}
