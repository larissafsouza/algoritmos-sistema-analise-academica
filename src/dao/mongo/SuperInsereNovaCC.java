package dao.mongo;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Scanner;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import dados.insere.inter.Insere;

public class SuperInsereNovaCC implements Insere {
	private DBCollection alunos;
	private String[] campos= {"cod_curso","nome_curso", "matr_aluno", "nome_pessoa", "cod_ativ_curric",
			"nome_ativ_curric", "media_final", "descr_situacao ", "ano", "periodo", "creditos",
			"total_carga_horaria", "forma_ingresso", "ano_ingresso", "forma_saida", "ano_saida"};
	
	public SuperInsereNovaCC(){	
		alunos = null;
		try {
			alunos = MongoConnection.getInstance().getDB().getCollection("atividadesAcademicaCC");
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void insere(String nomeS, String Separador)throws FileNotFoundException, ParseException{
		Scanner scanner = new Scanner(nomeS);
		Scanner codigos = new Scanner(new FileReader("C:\\Users\\Larissa\\Documents\\SistemaAlunoUFAM\\src\\main\\java\\codigos.txt"));
		HashSet<String> set1 = new HashSet<String> ();
		HashSet <String> set2 = new HashSet<String> ();
		HashSet <String> set3 = new HashSet<String> ();
		HashSet <String> set4 = new HashSet<String> ();
		HashSet <String> set5 = new HashSet<String> ();
		
			String[] atributos1 = codigos.nextLine().split(",");
			String[] atributos2 = codigos.nextLine().split(",");
			String[] atributos3 = codigos.nextLine().split(",");
			String[] atributos4 = codigos.nextLine().split(",");
			String[] atributos5 = codigos.nextLine().split(",");
			for(int i=0; i<atributos1.length; i++){
				set1.add(atributos1[i]);
			}
			for(int i=0; i<atributos2.length; i++){
				set2.add(atributos2[i]);
			}
			for(int i=0; i<atributos3.length; i++){
				set3.add(atributos3[i]);
			}
			for(int i=0; i<atributos4.length; i++){
				set4.add(atributos4[i]);
			}
			for(int i=0; i<atributos5.length; i++){
				set5.add(atributos5[i]);
			}
		
		codigos.close();	

		while (scanner.hasNext()) {
			String[] atributos = scanner.nextLine().split(Separador);
			BasicDBObject document = new BasicDBObject();
			//System.out.println("flag inicio: " + flag);
			String cod_curso = atributos[0].trim();
			document.put(campos[0], cod_curso);
			String nome_curso = atributos[1].trim();
			document.put(campos[1], nome_curso);
			Integer matr_aluno = Integer.parseInt(atributos[2].trim().toLowerCase());
			document.put(campos[2], matr_aluno);
			String nome_pessoa = atributos[3].trim();
			document.put(campos[3], nome_pessoa);
			String cod_ativ_curric = atributos[4].trim();
			document.put(campos[4], cod_ativ_curric);
			String nome_ativ_curric = atributos[5].trim();
			document.put(campos[5], nome_ativ_curric);
			String forma_ingresso = atributos[12].trim();
			Integer ano_ingresso = Integer.parseInt(atributos[13].trim());
			if (!set1.contains(cod_ativ_curric)){
				Float media_final = Float.parseFloat(atributos[6].trim());
				document.put(campos[6], media_final);
			} 
			if(!set2.contains(cod_ativ_curric)){
				String descr_situacao = atributos[7].trim();
				document.put(campos[7], descr_situacao);
			}
			
			Integer ano = Integer.parseInt(atributos[8].trim());
			String periodo = atributos[9].trim();
			document.put(campos[8], ano);
			document.put(campos[9], periodo);
			if(set3.contains(cod_ativ_curric)){
				
			} else if(!set4.contains(cod_ativ_curric)){
				Integer creditos = Integer.parseInt(atributos[10].trim());
				Integer total_carga_horaria = Integer.parseInt(atributos[11].trim());
				document.put(campos[10], creditos);
				document.put(campos[11], total_carga_horaria);
				
			}
			document.put(campos[12], forma_ingresso);
			document.put(campos[13], ano_ingresso);
			String forma_saida = atributos[14].trim();
			Integer ano_evasao = Integer.parseInt(atributos[15].trim());
			if(!forma_saida.equals("sem evasao")){
				document.put(campos[14], forma_saida);
				document.put(campos[15], ano_evasao);
			}
			
			
			alunos.save(document);	
				
			}
		scanner.close();	

	}
}
