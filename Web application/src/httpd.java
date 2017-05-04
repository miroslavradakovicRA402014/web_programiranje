
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Jednostavan web server
 */
public class httpd {

	private static List<User> users = new ArrayList<User>();
	
	public static void main(String args[]) throws IOException {
		int port = 80;
		
		@SuppressWarnings("resource")
		ServerSocket srvr = new ServerSocket(port);

		System.out.println("httpd running on port: " + port);
		System.out.println("document root is: "
				+ new File(".").getAbsolutePath() + "\n");

		Socket skt = null;

		while (true) {
			try {
				skt = srvr.accept();
				InetAddress addr = skt.getInetAddress();

				String resource = getResource(skt.getInputStream());
				// zastita od praznih zahteve od browsera 
				if (resource == null) {
					continue;
				}
				// localhost /pocetna
				if (resource.equals(""))
					resource = "index.html";

				System.out.println("Request from " + addr.getHostName() + ": "
						+ resource);

				sendResponse(resource, skt.getOutputStream());
				skt.close();
				skt = null;
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	static String getResource(InputStream is) throws IOException {
		BufferedReader dis = new BufferedReader(new InputStreamReader(is));
		String s = dis.readLine();
		System.out.println(s);
		// zastita od praznih zahteve od browsera 
		if (s == null) {
			return null;
		}

		String[] tokens = s.split(" ");

		// prva linija HTTP zahteva: METOD /resurs HTTP/verzija
		// obradjujemo samo GET metodu
		String method = tokens[0];
		if (!method.equals("GET")) {
			return null;
		}

		String rsrc = tokens[1];

		// izbacimo znak '/' sa pocetka
		rsrc = rsrc.substring(1);

		// ignorisemo ostatak zaglavlja
		String s1;
		while (!(s1 = dis.readLine()).equals(""))
			System.out.println(s1);

		return rsrc;
	}

	
	/***
	 * Funkcija koja parsira HTTP parametre
	 * Povratna vrednost je Map objekat u kom je kljuc naziv parametra, a vrednost pod tim kljucem, uneta vrednost sa forme
	 * 
	 * Primer upotrebe:
	 * Ako je resource="index.html?ime=Pera&prezime=Peric&operacija=dodaj
	 * Map<String, String> parametri = getParams(resource);
	 * String ime = parametri.get("ime");  // ovo ce biti "Pera"
	 * String prezime = parametri.get("prezime"); // ovo ce biti "Peric"
	 * 
	 * @param resource
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String, String> getParams(String resource) throws UnsupportedEncodingException {
		  final Map<String, String> queryPairs = new LinkedHashMap<String,String>();
		  // nadji ?
		  int queryStartIndex = resource.indexOf("?");
		  // ako nema ? , vrati praznu mapu
		  if(queryStartIndex == -1) {
			  return queryPairs;
		  }
		  
		  // u suprotnom parsiraj resource nakon ?
		  // odseci nakon ? ceo stalo
		  String query = resource.substring(queryStartIndex + 1);
		  
		  // cepaj po &
		  final String[] pairs = query.split("&");
		  
		  // napravi parove gde ce kljuc biti naziv parama, a vrednost pod tim kljucem ce biti vrednost parametra iz URL-a
		  for (String pair : pairs) {
		    final int idx = pair.indexOf("=");
		    final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
		    final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
		    queryPairs.put(key, value);
		  }
		  return queryPairs;
		}
	
	static void sendResponse(String resource, OutputStream os)
			throws IOException {
		PrintStream ps = new PrintStream(os);
		File file = null;
		// sadrzaj HTTP odgovora za klijenta
		String retVal = "";
		Map<String, String> parametri = getParams(resource);
		// DA LI VRACAMO FAJL ILI PARSIRAMO HTTP ZAHTEV SA FORME?
			if (resource.startsWith("index.html")) {
				retVal = "HTTP/1.1 200 OK\r\nContent-Type: text/html;charset=UTF-8\r\n\r\n";
				retVal += "<html><head><link href='style.css' rel='stylesheet' type='text/css'></head>\n";
				retVal += "<body>";
				/////////////////////////
				// PRAZNA STRANICA
				
				retVal += ispisiFormu(null);
				// koja je operacija itd?
		        retVal += odradiOperaciju(resource,parametri);
		        retVal += ispisiTabelu(users);
		        
		        
				retVal += "</body>";
				retVal += "</html>";
				// print u output stream da bi se poslalo...
				ps.print(retVal);
				
				
			} else if (resource.startsWith("filtriraj")) {
				retVal = "HTTP/1.1 200 OK\r\nContent-Type: text/html;charset=UTF-8\r\n\r\n";
				retVal += "<html><head><link href='style.css' rel='stylesheet' type='text/css'></head>\n";
				retVal += "<body>";
				
				retVal += ispisiFormu(null);
				List<User> filtrirano = new ArrayList<User>();
				
				String kriterijumFiltracije = parametri.get("kriterijum");

				switch (kriterijumFiltracije) {
				  case "iznad10000": {					  
					 for (User korisnici: users){
						if (korisnici.getKredit() > 10000) {
							filtrirano.add(korisnici);
						}
					 } 
					  
					 break; 
				  }
				  case "iznad100000": {					  
					 for (User korisnici: users){
						if (korisnici.getKredit() > 100000) {
							filtrirano.add(korisnici);
						}
					 } 	  
					 break; 
				  }								   
				  default:
					 filtrirano = users;
					 break;
				
				}
								
		        retVal += ispisiTabelu(filtrirano);		
		        
				retVal += "</body>";
				retVal += "</html>";
				// print u output stream da bi se poslalo...
				ps.print(retVal);				
				
				
			} else if (resource.startsWith("izmeniKorisnika")) {
				retVal = "HTTP/1.1 200 OK\r\nContent-Type: text/html;charset=UTF-8\r\n\r\n";
				retVal += "<html><head><link href='style.css' rel='stylesheet' type='text/css'></head>\n";
				retVal += "<body>";
				
				User izmena = null;
				
				for (User korisnik:users) {
				  if (korisnik.geteMail().equals(parametri.get("kogaMenjamo"))) {
					 izmena = korisnik;
					 break;
				  }											
				}
						
				retVal += ispisiFormu(izmena);
				retVal += ispisiTabelu(users);
				
				retVal += "</body>";
				retVal += "</html>";

				ps.print(retVal);
				
			} else if (resource.startsWith("izbrisiKorisnika")) {
				retVal = "HTTP/1.1 200 OK\r\nContent-Type: text/html;charset=UTF-8\r\n\r\n";
				retVal += "<html><head><link href='style.css' rel='stylesheet' type='text/css'></head>\n";
				retVal += "<body>";

				retVal += ispisiFormu(null);
				
				User korisnik;
								
				for (int i = 0; i < users.size(); i++) {
			      korisnik = users.get(i);		
				  if (korisnik.geteMail().equals(parametri.get("kogaBrisemo"))) {
					 users.remove(i);
					 break;
				  }											
				}
				
				retVal += ispisiTabelu(users);
								
				retVal += "</body>";
				retVal += "</html>";
				
			}
			
			// else if mozemo dodati ako je neki drugi URL, neka druga forma, neka druga stranica
			//else if()  {
			//	
			//}
				
			
			else {
				// NIJE SE POKLOPILO NI SA JEDNIM URLom za prihvatanje podataka sa forme
				// zamenimo web separator sistemskim separatorom
				resource = resource.replace('/', File.separatorChar);
				file = new File(resource);
				
				// AKO NIJE ZAHTEV SA FORME I NIJE FAJL , ONDA VRACAMO 404, NEMA NICEG NA TOM URL
				// POYYY
				if(!file.exists()) {
					// ako datoteka ne postoji, vratimo kod za gresku
					ps.print("HTTP/1.0 404 File not found\r\n"
							+ "Content-type: text/html; charset=UTF-8\r\n\r\n<b>404 Нисам нашао:"
							+ file.getName() + "</b>");
					// ps.flush();
					System.out.println("Could not find resource: " + file);
					return;
				}
				
				// ispisemo zaglavlje HTTP odgovora
				ps.print("HTTP/1.0 200 OK\r\n\r\n");

				// a, zatim datoteku
				FileInputStream fis = new FileInputStream(file);
				byte[] data = new byte[8192];
				int len;

				while ((len = fis.read(data)) != -1) {
					ps.write(data, 0, len);
				}
				fis.close();
			} 
		ps.flush();
		
	}
	
	@SuppressWarnings("deprecation")
	public static String odradiOperaciju(String resource,Map<String, String> parametri) {
		String retVal = "";
		
		if (resource.contains("operacija")) {
           String[] delovi = resource.split("\\?");			
		   String sviParametri = delovi[1];
		   String[] deoParametara = sviParametri.split("&");
		   
		   for (int i = 0; i < deoParametara.length; i++) {
			  String par = deoParametara[i];
			  String[] vrednosti = par.split("=");
			  parametri.put(vrednosti[0], URLDecoder.decode(vrednosti[1])); 
		   }
			
			
		   switch (parametri.get("operacija")) {
		     case "Dodaj": { 
		       User korisnik = new User(parametri.get("ime"),parametri.get("prezime"),parametri.get("email"),parametri.get("grad"),Integer.parseInt(parametri.get("kredit")));
		       users.add(korisnik);
		       break;	 		    	 
		     }  
		     case "Snimi": { 
		       for (User izmeni: users) {
		    	  if (izmeni.geteMail().equals(parametri.get("email"))) {
		    		  izmeni.setIme(parametri.get("ime"));
		    		  izmeni.setPrezime(parametri.get("prezime"));
		    		  izmeni.setGrad(parametri.get("grad"));
		    		  izmeni.setKredit(Integer.parseInt(parametri.get("kredit")));
		    		  break;
		    	  }		    	   
		       } 	 		    	 
		       break;	 
		     }
		     case "Obrisi": { 
			   for (int i = 0 ; i < users.size(); i++) {
				   User zaObrisati = users.get(i);
				   if (zaObrisati.geteMail().equals(parametri.get("email"))) {
                      users.remove(i);
                      break;
				   }		    	   
			   } 		    
			   break;
		     }
		     default: {
		       break;
		     }
		  		   
		   }
			
		}
				
		return retVal;
	}
	
	
	public static String ispisiFormu(User korisnik) {
		
		String retVal = "";
		
		if (korisnik == null) {
		   retVal += "<form action='/index.html' >";	
		   retVal += "<table border='1'>";
		   
		   retVal += "<tr>";	   
		   retVal += "<td> Ime :</td>";
		   retVal += "<td> <input type='text' name='ime' required> </td>";		   
		   retVal += "</tr>";
		   
		   retVal += "<tr>";		   
		   retVal += "<td> Prezime :</td>";
		   retVal += "<td> <input type='text' name='prezime' required> </td>";
		   retVal += "</tr>";
		   
		   retVal += "<tr>";	   
		   retVal += "<td> Email :</td>";
		   retVal += "<td> <input type='email' name='email' required> </td>";
		   retVal += "</tr>";
		   
		   retVal += "<tr>";
		   retVal += "<td> Grad :</td>";
		   retVal += "<td> <select name='grad' required>";
		   retVal += "<option value='Beograd'>Beograd</option>";
		   retVal += "<option value='Novi Sad'>Novi Sad</option>";
		   retVal += "<select></td>";
		   retVal += "</tr>";
		   
		   retVal += "<tr>";	   
		   retVal += "<td> Kredit :</td>";
		   retVal += "<td> <input type='number' name='kredit' required> </td>";		   
		   retVal += "</tr>";
		   
	       retVal += "<tr>";
	       retVal += "<td colspan='2'>";
	       retVal += "<input type='submit' name='operacija' value='Dodaj'>";
	       retVal += "<input type='submit' name='operacija' value='Snimi'>";	       
	       retVal += "<input type='submit' name='operacija' value='Obrisi'>";
	       retVal += "<a href='/index.html'> Odustani </a>";
	       retVal += "</td>";
	       retVal += "</tr>";
		   
		   retVal += "</table>";
		   retVal += "</form>";
		   
		} else {
		   retVal += "<form action='/index.html' >";	
		   retVal += "<table border='1'>";
			   
	       retVal += "<tr>";	   
		   retVal += "<td> Ime :</td>";
		   retVal += "<td> <input type='text' name='ime' required value='"+korisnik.getIme()+"'> </td>";		   
		   retVal += "</tr>";
			   
		   retVal += "<tr>";		   
		   retVal += "<td> Prezime :</td>";
		   retVal += "<td> <input type='text' name='prezime' required value='"+korisnik.getPrezime()+"'> </td>";
		   retVal += "</tr>";
			   
		   retVal += "<tr>";	   
		   retVal += "<td> Email :</td>";
		   retVal += "<td> <input type='email' name='email' required readonly value='"+korisnik.geteMail()+"'></td>";
		   retVal += "</tr>";	
		   
		   retVal += "<tr>";
		   retVal += "<td> Grad :</td>";
		   retVal += "<select name='grad' required>";
		   retVal += "<option value='Novi sad'>"+(korisnik.getGrad().equals("Novi Sad")?"selected":"")+" Novi sad</option>";
		   retVal += "<option value='Beograd'>"+(korisnik.getGrad().equals("Beograd")?"selected":"")+" Beograd</option>";
		   retVal += "</select></td>";
		   retVal += "</tr>";
		   
		   retVal += "<tr>";	   
		   retVal += "<td> Kredit :</td>";
		   retVal += "<td> <input type='number' name='kredit' required value='"+korisnik.getKredit()+"'></td>";
		   retVal += "</tr>";	
		   
	       retVal += "<tr>";
	       retVal += "<td colspan='2'>";
	       retVal += "<input type='submit' name='operacija' value='Dodaj'>";
	       retVal += "<input type='submit' name='operacija' value='Snimi'>";	       
	       retVal += "<input type='submit' name='operacija' value='Obrisi'>";
	       retVal += "<a href='/index.html'> Odustani </a>";
	       retVal += "</td>";
	       retVal += "</tr>";
		   
		   retVal += "</table>";
		   retVal += "</form>";		   
			
			
		}
			
		return retVal;
	}
	
	public static String ispisiTabelu(List<User> users)
			throws UnsupportedEncodingException {
		
		String retVal = "";
		
		retVal += "<h1>TABELA TRENUTNO UNETIH KORISNIKA </h1>";
		
		if (users.isEmpty()) {
		   retVal += "<h2>Trenutno nema unetih korisnika </h2>";
		} else {
		   retVal += "<table border='1'>";
		   retVal += "<tr>";
		   retVal += "<td>Ime</td>";
		   retVal += "<td>Prezime</td>";
		   retVal += "<td>Email</td>";
		   retVal += "<td>Grad</td>";
		   retVal += "<td>Kredit</td>";
		   retVal += "<td>Izmeni</td>";
		   retVal += "<td>Brisi</td>";
		   retVal += "</tr>";
		   
		   		   
		   for (User korisnik:users) {
		       retVal += "<tr>";	   
			   retVal += "<td>"+korisnik.getIme()+"</td>";
			   retVal += "<td>"+korisnik.getPrezime()+"</td>";	
			   retVal += "<td>"+korisnik.geteMail()+"</td>";			   
			   
			   switch (korisnik.getGrad()) {
			     case "Novi Sad": {
			    	retVal += "<td> 21000 Novi sad </td>"; 
			    	break; 
			     } 
			     case "Beograd": {
			    	retVal += "<td> 11000 Beograd </td>"; 
			    	break; 
			     }			   
			     default: {
			    	break; 
			     }
			   }
			
			   retVal += "<td>"+korisnik.getKredit()+"</td>";	
			   
			   retVal += "<td>";
			   retVal += "<a href='izmeniKorisnika?kogaMenjamo="+URLEncoder.encode(korisnik.geteMail(), "UTF-8")+"'>Izmeni</a>";		   
			   retVal += "</td>";
			   
			   retVal += "<td>";
			   retVal += "<a href='izbrisiKorisnika?kogaBrisemo="+URLEncoder.encode(korisnik.geteMail(), "UTF-8")+"'>Izbrisi</a>";		   
			   retVal += "</td>";
			   
			   retVal += "</tr>";
		   }
			
		
		   retVal +="<tr> <td colspan='7'> <form action='filtriraj'>";
		   retVal +="<select name='kriterijum'>";
		   retVal +="<option value='iznad10000'>Prikazi sve korisnike sa kreditom iznad 10000</option>";
		   retVal +="<option value='iznad100000'>Prikazi sve korisnike sa kreditom iznad 100000</option>";
		   retVal +="<option value='svi'>Prikazi sve korisnike</option>";
		   retVal +="</select>";
		   retVal +="<input type='submit' value='filtriranje'>";
		   retVal +="</form></td></tr>";
		   retVal +="</table>";
		}
		
		return retVal;
	}
}
