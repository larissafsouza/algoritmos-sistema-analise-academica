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

public class SuperInsereNova implements Insere {
	private DBCollection alunos;
	private String[] campos= {"id_aluno", "nome_curso", "cod_curso", "versao_curso", "cod_ativ_curricular",
			"nome_ativ_curricular", "media_final", "descricao_situacao", "ano", "periodo", "creditos",
			"carga_horaria_teorica", "carga_horaria_pratica" ,"forma_ingresso", "ano_ingresso", "forma_saida", "ano_saida"};
	
	public SuperInsereNova(){	
		alunos = null;
		
	}
	
	public void insere(String nomeS, String Separador)throws FileNotFoundException, ParseException{
		Scanner scanner = new Scanner(new FileReader("/home/larissafabiola/workspace/SistemaAnaliseAcademica/dadosLimposAtividadesAcademicaUFAM.txt"));
		Scanner codigos = new Scanner(new FileReader("/home/larissafabiola/workspace/SistemaAnaliseAcademica/codigos.txt"));
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
			String[] atributos = scanner.nextLine().split(";");
			BasicDBObject document = new BasicDBObject();
			//System.out.println("flag inicio: " + flag);
			String id_aluno = atributos[0].trim();
			document.put(campos[0], id_aluno);
			String nome_curso = atributos[1].trim();
			document.put(campos[1], nome_curso);
			String cod_curso = atributos[2].trim();
			document.put(campos[2], cod_curso);
			
			
			String versao_curso = atributos[3].trim();
			document.put(campos[3], versao_curso);
			String cod_ativ_curric = atributos[4].trim();
			document.put(campos[4], cod_ativ_curric);
			String nome_ativ_curric = atributos[5].trim();
			document.put(campos[5], nome_ativ_curric);
			String forma_ingresso = atributos[13].trim();
			Integer ano_ingresso = Integer.parseInt(atributos[14].trim());
			if (!set1.contains(cod_ativ_curric)&& !atributos[6].trim().equals("")){
				Float media_final=null; 
				try {
					media_final = Float.parseFloat(atributos[6].trim());
					while(media_final>10){
						media_final = media_final/10;
						
					}
					System.out.println("media: " + atributos[6]);
					document.put(campos[6], media_final);

				} 
				catch (NumberFormatException e) {
//					System.out.println("media exception : " + atributos[6].replace(".", ";"));
					Scanner scanner2 = new Scanner(atributos[6].replace(".", ";"));
					String[] temp = scanner2.nextLine().split(";");
					
					double media = (Double.parseDouble(temp[0]))/10;
					//System.out.println("Deu exception : " + media);
					document.put(campos[6], media);

					//e.printStackTrace();
				}
				
				
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
				document.put(campos[10], creditos);

				if(!atributos[11].trim().equals("")){
					Integer carga_horariaTeorica = Integer.parseInt(atributos[11].trim());
					document.put(campos[11], carga_horariaTeorica);
				} if(!atributos[12].trim().equals("")){
					Integer carga_horariaPratica = Integer.parseInt(atributos[12].trim());
					document.put(campos[12], carga_horariaPratica);

				}

				

			}
			document.put(campos[13], forma_ingresso);
			document.put(campos[14], ano_ingresso);
			String forma_saida = atributos[15].trim();
			if(!forma_saida.equals("sem evasao")){
				document.put(campos[15], forma_saida);
				Integer ano_evasao = Integer.parseInt(atributos[16].trim());
				document.put(campos[16], ano_evasao);
				
			}
			
			try {
				alunos = MongoConnection.getInstance().getDB().getCollection("atividades_academica_"+nome_curso.replace("- ", "").replace(" ","_"));
			} catch (UnknownHostException e) {
//				 TODO Auto-generated catch block
				e.printStackTrace();
			}
			alunos.save(document);	
				
			}
		scanner.close();	

	}
}
