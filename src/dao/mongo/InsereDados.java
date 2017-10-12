package dao.mongo;
import java.io.FileNotFoundException;
import java.text.ParseException;

import dados.insere.inter.Insere;
import dados.insere.inter.Limpeza;


public class InsereDados {
	private static final String SEPARADOR = ";";
	private Limpeza limpezaDados;
	private Insere insereDados;
	
	public InsereDados(Limpeza novoLimpezaDados, Insere novoInsereDados){
		
		limpezaDados= novoLimpezaDados;
		insereDados = novoInsereDados;
	}
	
	public String limpaDados(String nomeArquivo) throws Exception{
		return limpezaDados.limpa(nomeArquivo, SEPARADOR);
		
	}
	
	public void insereDados(String novo)throws FileNotFoundException, ParseException{
		insereDados.insere(novo, SEPARADOR);
		//System.out.println("pronto");
		//System.out.println(novo);
	}
}
