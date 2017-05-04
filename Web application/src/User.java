
public class User {

	private String ime;
	private String prezime;
	private String eMail;
    private String grad;
    private int kredit;

	public User(String ime, String prezime, String eMail, String grad, int kredit) {
		super();
		this.ime = ime;
		this.prezime = prezime;
		this.eMail = eMail;
		this.grad = grad;
		this.kredit = kredit;
	}

	public String getIme() {
		return ime;
	}
	public void setIme(String ime) {
		this.ime = ime;
	}
	public String getPrezime() {
		return prezime;
	}
	public void setPrezime(String prezime) {
		this.prezime = prezime;
	}
	public String geteMail() {
		return eMail;
	}
	public void seteMail(String eMail) {
		this.eMail = eMail;
	}
	public String getGrad() {
		return grad;
	}
	public void setGrad(String grad) {
		this.grad = grad;
	}
	public int getKredit() {
		return kredit;
	}
	public void setKredit(int kredit) {
		this.kredit = kredit;
	}
    
}
