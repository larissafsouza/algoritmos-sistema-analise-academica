package dao.mongo;
import java.io.FileWriter;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import util.RemoveAcentos;
import dados.insere.inter.Limpeza;
public class SuperLimpaNovaCC implements Limpeza {
	private String dados;
	
	public SuperLimpaNovaCC(){
		dados="";
	}
	
	public String limpa(String nomeArquivo, String SEPARADOR) throws Exception {
		BufferedReader myBuffer = new BufferedReader(new InputStreamReader(new FileInputStream(nomeArquivo)));
		FileWriter writer;
		String linhaDeDados;
		String linha = myBuffer.readLine();
		try{
			writer = new FileWriter("dadosLimposAtividadesAcademicaCC.txt");
			linha = myBuffer.readLine();

			while (linha !=null) {
				
				linhaDeDados="";
				String[] atributos = linha.split(SEPARADOR);
				String cod_curso = RemoveAcentos.removerAcentos(atributos[6].trim().toLowerCase().replace("  ", " "));
				//System.out.println(cod_curso);
				linhaDeDados= cod_curso + ",";
				String nome_curso = RemoveAcentos.removerAcentos(atributos[5].trim().toLowerCase().replace("  ", " "));
				System.out.println("nome curso: "+ nome_curso);
				
				linhaDeDados= linhaDeDados + nome_curso + ",";
				Integer matr_aluno = Integer.parseInt(atributos[3].trim().toLowerCase().replace("  ", " "));
				linhaDeDados= linhaDeDados + matr_aluno + ",";
				String nome_pessoa = RemoveAcentos.removerAcentos(atributos[1].trim().toLowerCase().replace("  ", " "));
				linhaDeDados= linhaDeDados + nome_pessoa + ",";
				String cod_ativ_curric = RemoveAcentos.removerAcentos(atributos[9].trim().toLowerCase().replace("  ", " "));
				linhaDeDados= linhaDeDados + cod_ativ_curric + ",";
				String nome_ativ_curric = RemoveAcentos.removerAcentos(atributos[10].trim().toLowerCase().replace("  ", " "));
				linhaDeDados= linhaDeDados + nome_ativ_curric + ",";
				String media_final = RemoveAcentos.removerAcentos(atributos[12].trim().toLowerCase().replace("  ", " "));
				linhaDeDados=linhaDeDados + media_final + ",";
				String descr_situacao = RemoveAcentos.removerAcentos(atributos[13].trim().toLowerCase().replace("  ", " "));
				linhaDeDados=linhaDeDados + descr_situacao + ",";
				String ano = RemoveAcentos.removerAcentos(atributos[8].trim().toLowerCase().replace("  ", " "));
				String periodo;
				if (atributos[14].trim().equals("1° Semestre")) {
					periodo = "1A";
				} else if(atributos[14].trim().equals("2° Semestre")){
					periodo= "2A";
				} else if (atributos[14].trim().equals("Férias 2° Semestre")){
					periodo = "2F";
				} else{
					periodo = "1F";
				}
				linhaDeDados= linhaDeDados + ano + ",";
				linhaDeDados= linhaDeDados + periodo + ",";
				//System.out.println("flag depois: " + flag);
				String creditos = RemoveAcentos.removerAcentos(atributos[11].trim().toLowerCase().replace("  ", " "));
				Integer total_carga_horaria = Integer.parseInt(RemoveAcentos.removerAcentos(atributos[19].trim().toLowerCase().replace("  ", " ")));
				linhaDeDados=linhaDeDados + creditos + ",";
				linhaDeDados=linhaDeDados + total_carga_horaria + ",";
				String forma_ingresso = RemoveAcentos.removerAcentos(atributos[20].trim().toLowerCase().replace("  ", " "));
				String ano_ingresso = RemoveAcentos.removerAcentos(atributos[21].trim().toLowerCase().replace("  ", " "));
				linhaDeDados=linhaDeDados + forma_ingresso + ",";
				linhaDeDados=linhaDeDados + ano_ingresso + ",";	
				
				String forma_saida = RemoveAcentos.removerAcentos(atributos[22].trim().toLowerCase().replace("  ", " "));
				String ano_evasao = RemoveAcentos.removerAcentos(atributos[23].trim().toLowerCase().replace("  ", " "));
			
				linhaDeDados=linhaDeDados + forma_saida + ",";
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
