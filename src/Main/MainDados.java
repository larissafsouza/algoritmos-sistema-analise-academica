package Main;
import java.io.FileNotFoundException;
import java.text.ParseException;

import dao.mongo.InsereDados;
import dao.mongo.SuperInsereNova;
import dao.mongo.SuperLimpaNova;

public class MainDados {
	public static void main (String[] args)throws FileNotFoundException, ParseException, Exception {
		InsereDados insere = new InsereDados(new SuperLimpaNova(), new SuperInsereNova());
		//insere.limpaDados("C:\\Users\\Larissa\\workspaceProjeto\\SistemaDeAnaliseAcademica\\src\\main\\java\\listagem info alunos ICOMP.csv");
		insere.insereDados("");

	}
}
