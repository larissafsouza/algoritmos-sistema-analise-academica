package dados.insere.inter;
import java.io.FileNotFoundException;
import java.text.ParseException;

public interface Insere {
	
	public void insere(String nomeArquivo, String Separador)throws FileNotFoundException, ParseException;
}
