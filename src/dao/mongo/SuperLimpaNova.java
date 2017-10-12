package dao.mongo;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import util.RemoveAcentos;
import dados.insere.inter.Limpeza;
public class SuperLimpaNova implements Limpeza {
	private String dados;
	
	public SuperLimpaNova(){
		dados="";
	}
	
	public String limpa(String nomeArquivo, String SEPARADOR) throws Exception {
		BufferedReader myBuffer = new BufferedReader(new InputStreamReader(new FileInputStream(nomeArquivo)));
		FileWriter writer;
		//String cabecalho = scanner.nextLine();
		String linhaDeDados;
		String linha = myBuffer.readLine();
		linha = myBuffer.readLine();

		//int i=0;
		//System.out.println("Cabecalho do arquivo: " + cabecalho);

		try{
			writer = new FileWriter("dadosLimposAtividadesAcademicaUFAM.txt");
			while (linha !=null) {
				linhaDeDados="";
				String[] atributos = linha.split(SEPARADOR);
				//System.out.println("flag inicio: " + flag);
				String id_aluno = atributos[1].trim().toLowerCase().replace("  ", " ");
				linhaDeDados= linhaDeDados + id_aluno + ";";
				String nome_curso = RemoveAcentos.removerAcentos(atributos[2].trim().toLowerCase().replace("  ", " "));
				System.out.println("nome curso: "+ nome_curso);
				//System.out.println("nome curso depois: "+ tempNome_curso);
				linhaDeDados= linhaDeDados + nome_curso + ";";
				String cod_curso = RemoveAcentos.removerAcentos(atributos[3].trim().toLowerCase().replace("  ", " "));
				System.out.println("cod_curso:" +cod_curso);
				linhaDeDados= linhaDeDados + cod_curso + ";";
				String versao_curso = RemoveAcentos.removerAcentos(atributos[4].trim().toLowerCase().replace("  ", " "));
				linhaDeDados= linhaDeDados + versao_curso + ";";
				String cod_ativ_curric = RemoveAcentos.removerAcentos(atributos[7].trim().toLowerCase().replace("  ", " "));
				linhaDeDados= linhaDeDados + cod_ativ_curric + ";";
				String nome_ativ_curric = RemoveAcentos.removerAcentos(atributos[8].trim().toLowerCase().replace("  ", " "));
				linhaDeDados= linhaDeDados + nome_ativ_curric + ";";
				String media_final = RemoveAcentos.removerAcentos(atributos[10].trim().toLowerCase().replace("  ", " "));
				linhaDeDados=linhaDeDados + media_final + ";";
				String descr_situacao = RemoveAcentos.removerAcentos(atributos[11].trim().toLowerCase().replace("  ", " "));
				linhaDeDados=linhaDeDados + descr_situacao + ";";
				String ano = RemoveAcentos.removerAcentos(atributos[5].trim().toLowerCase().replace("  ", " "));
				String periodo;
				//System.out.println(atributos[9].trim());
				if (atributos[6].trim().equals("1° Semestre")) {
					//System.out.println("entrou em 1A");
					periodo = "1A";
				} else if(atributos[6].trim().equals("2° Semestre")){
					//System.out.println("entrou em 2A");
					periodo= "2A";
				} else if (atributos[6].trim().equals("Férias 2° Semestre")){
					//System.out.println("entrou em 2F");

					periodo = "2F";
				} else{
					//System.out.println("entrou em 1F");
					periodo = "1F";
				}
				linhaDeDados= linhaDeDados + ano + ";";
				linhaDeDados= linhaDeDados + periodo + ";";
				//System.out.println("flag depois: " + flag);
				String creditos = RemoveAcentos.removerAcentos(atributos[9].trim().toLowerCase().replace("  ", " "));
				String carga_horariaTeorica = RemoveAcentos.removerAcentos(atributos[12].trim().toLowerCase().replace("  ", " "));
				String carga_horariaPratica = RemoveAcentos.removerAcentos(atributos[13].trim().toLowerCase().replace("  ", " "));
				linhaDeDados=linhaDeDados + creditos + ";";
				linhaDeDados=linhaDeDados + carga_horariaTeorica + ";";
				linhaDeDados=linhaDeDados + carga_horariaPratica + ";";

				String forma_ingresso = RemoveAcentos.removerAcentos(atributos[14].trim().toLowerCase().replace("  ", " "));
				String ano_ingresso = RemoveAcentos.removerAcentos(atributos[15].trim().toLowerCase().replace("  ", " "));
				linhaDeDados=linhaDeDados + forma_ingresso + ";";
				linhaDeDados=linhaDeDados + ano_ingresso + ";";	
				
				String forma_saida = RemoveAcentos.removerAcentos(atributos[16].trim().toLowerCase().replace("  ", " "));
				String ano_evasao = RemoveAcentos.removerAcentos(atributos[17].trim().toLowerCase().replace("  ", " "));
			
				linhaDeDados=linhaDeDados + forma_saida + ";";
				linhaDeDados=linhaDeDados + ano_evasao;
				//System.out.println("LinhadeDados: " + linhaDeDados);
				//System.out.println("Pronto");
				dados = dados + linhaDeDados + "\n";
				//i++;
				writer.write(linhaDeDados+"\n");
				linha= myBuffer.readLine();
				}
			myBuffer.close();
			writer.close();
			}catch(IOException ex){
				ex.printStackTrace();
			}
		//	System.out.println("i:" + i);
		return dados;
	}
	
}
