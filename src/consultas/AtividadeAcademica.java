package consultas;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;


import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import dao.mongo.MongoConnection;
public class AtividadeAcademica {
	private DBCollection alunos;
	//private DBCollection alunosCC;
	
	public AtividadeAcademica(String curso){
		
			alunos = null;
			
				try {
					alunos = MongoConnection.getInstance().getDB().getCollection("atividades_academica_"+curso.toLowerCase().trim().replace(" ", ""));
					//alunosCC = MongoConnection.getInstance().getDB().getCollection("atividadesAcademicaCC");
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
		
	}	
	
	
		public List<String> aprovadosDisciplina(String disciplina, double notaInicial, double notaFinal){
		
			DBObject match = new BasicDBObject("$match", new BasicDBObject("nome_ativ_curric", disciplina));
			DBObject match1 = new BasicDBObject("$match", new BasicDBObject("media_final",  new BasicDBObject("$gt", notaInicial).append("$lt", notaFinal)));			
			DBObject id = new BasicDBObject();
			//id.put("ano_ingresso", new BasicDBObject("$year", "$ano_ingresso"));
			//id.put("nome_ativ_curric", "$nome_ativ_curric");
			id.put("curso","$cod_curso");
			
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			
			List<DBObject> pipeline = Arrays.asList(match,match1,group);
			System.out.println(pipeline);
			AggregationOutput output = alunos.aggregate(pipeline);

			List<String> list = new ArrayList<String>();

			for (DBObject dbo : output.results()) {
				System.out.println("dbo : " + dbo);
				String motivo_evasao = dbo.get("quantidade").toString();
				list.add(motivo_evasao);
			}
			return list;
	}
		public String mediaDeAprovadosDisciplina(double notaInicial, double notaFinal){
			String linha="";
			//DBObject match = new BasicDBObject("$match", new BasicDBObject("cod_ativ_curric", disciplina));
			DBObject match1 = new BasicDBObject("$match", new BasicDBObject("media_final",  new BasicDBObject("$gt", notaInicial).append("$lt", notaFinal)));			
			DBObject id = new BasicDBObject();
			//id.put("ano", "$ano");
			id.put("nome_ativ_curricular", "$nome_ativ_curricular");
			//id.put("periodo", "$periodo");
			id.put("curso","$cod_curso");
			//id.put("media_final",new BasicDBObject("$gt", notaInicial));
			
			DBObject id2 = new BasicDBObject();
			id2.put("media_final", "$media_final");
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			//groupFields.put("soma notas", new BasicDBObject("$sum", id2.get("media_final")));
			groupFields.put("media das notas", new BasicDBObject("$avg", id2.get("media_final")));

			//groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			//List<String> list = new ArrayList<>();
			
			List<DBObject> pipeline = Arrays.asList(match1, group, sort);
			System.out.println("pipeline " + pipeline);
			
			AggregationOutput output = alunos.aggregate(pipeline);
						
			for (DBObject dbo : output.results()) {
				System.out.println("dbo: " +dbo);
//				DBObject Getid = (DBObject) dbo.get("_id");
//				//String ano = Getid.get("ano").toString();
//				String codAtiv = Getid.get("nome_ativ_curric").toString();
//				String media = dbo.get("media das notas").toString();
//				linha = linha +codAtiv+";"+media+"\n";
				
				//list.add(ano);
				//list.add(periodo);
				//list.add(media);
				//System.out.println(list.get(0));
			} 
			
			return linha;
		}
		
		public String mediaDeAprovadosEmCadaDisciplina(String disciplina, double notaInicial, double notaFinal){
			String linha="";
			DBObject match = new BasicDBObject("$match", new BasicDBObject("cod_ativ_curric", disciplina));
			DBObject match1 = new BasicDBObject("$match", new BasicDBObject("media_final",  new BasicDBObject("$gt", notaInicial).append("$lt", notaFinal)));			
			DBObject id = new BasicDBObject();
			id.put("ano", "$ano");
			id.put("periodo", "$periodo");
			id.put("curso","$cod_curso");
			//id.put("media_final",new BasicDBObject("$gt", notaInicial));
			
			DBObject id2 = new BasicDBObject();
			id2.put("media_final", "$media_final");
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			//groupFields.put("soma notas", new BasicDBObject("$sum", id2.get("media_final")));
			groupFields.put("media das notas", new BasicDBObject("$avg", id2.get("media_final")));

			//groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			
			List<DBObject> pipeline = Arrays.asList(match, match1, group, sort);
			System.out.println("pipeline " + pipeline);
			
			AggregationOutput output = alunos.aggregate(pipeline);
						
			for (DBObject dbo : output.results()) {
				System.out.println("dbo: " +dbo);
				DBObject Getid = (DBObject) dbo.get("_id");
				String ano = Getid.get("ano").toString();
				String periodo = Getid.get("periodo").toString();
				String media = dbo.get("media das notas").toString();
				linha = linha +ano+";"+periodo+";"+media+"\n";
				
				//list.add(ano);
				//list.add(periodo);
				//list.add(media);
				//System.out.println(list.get(0));
			} 
			
			return linha;
		}
		
		public String SituacoesEmDisciplina(String disciplina){
			String linha="";
			DBObject match = new BasicDBObject("$match", new BasicDBObject("cod_ativ_curric", disciplina));
			DBObject id = new BasicDBObject();
			id.put("descr_situacao ", "$descr_situacao ");
			//id.put("curso","$cod_curso");
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			
			List<DBObject> pipeline = Arrays.asList(match,group, sort);
			//System.out.println("pipeline " + pipeline);
			
			AggregationOutput output = alunos.aggregate(pipeline);
						
			for (DBObject dbo : output.results()) {
				System.out.println("dbo: " +dbo);
				DBObject Getid = (DBObject) dbo.get("_id");
				String ano = Getid.get("descr_situacao ").toString();
				String media = dbo.get("quantidade").toString();
				linha = linha +ano+";"+media+"\n";
				
				//list.add(ano);
				//list.add(periodo);
				//list.add(media);
				//System.out.println(list.get(0));
			} 
			
 
			
			return linha;
		}
		
		public String anoMaisRigoroso(int ano_ingresso){
			String linha="";
			DBObject match = new BasicDBObject("$match", new BasicDBObject("ano_ingresso", ano_ingresso));
			DBObject match1 = new BasicDBObject("$match", new BasicDBObject("media_final",  new BasicDBObject("$gt", 0).append("$lt", 5)));			
			DBObject id = new BasicDBObject();
			id.put("ano", "$ano");
			id.put("curso","$cod_curso");
			//id.put("media_final",new BasicDBObject("$gt", notaInicial));
			
			
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			
			List<DBObject> pipeline = Arrays.asList(match, match1, group, sort);
			//System.out.println("pipeline " + pipeline);
			
			AggregationOutput output = alunos.aggregate(pipeline);
						
			for (DBObject dbo : output.results()) {
				//System.out.println("dbo: " +dbo);
				DBObject Getid = (DBObject) dbo.get("_id");
				String ano = Getid.get("ano").toString();
				String media = dbo.get("quantidade").toString();
				linha = linha +ano+";"+media+"\n";
				
				//list.add(ano);
				//list.add(periodo);
				//list.add(media);
				//System.out.println(list.get(0));
			} 
			
			return linha;
		}
		
		
		
		public String formaDeEvaso(String forma_evasao){
			String linha="";
		    DBObject match2 = new BasicDBObject("$match", new BasicDBObject("forma_saida",forma_evasao));

			DBObject id = new BasicDBObject();
			//id.put("ano_ingresso", new BasicDBObject("$year", "$ano_ingresso"));
			//id.put("curso","$cod_curso");
			//id.put("ano", "$ano");
			//id.put("ano_saida", "$ano_saida");
			id.put("forma_saida", "$forma_saida");
			id.put("id_aluno", "$id_aluno");
			  
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			
			
			DBObject group = new BasicDBObject("$group", groupFields);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			
			List<DBObject> pipeline = Arrays.asList(match2, group,sort);
			System.out.println(pipeline);
			AggregationOutput output = alunos.aggregate(pipeline);


			for (DBObject dbo : output.results()) {
				System.out.println(dbo);
				//DBObject Getid = (DBObject) dbo.get("_id");
				//String ano = Getid.get("ano_saida").toString();
				//String periodo = Getid.get("periodo").toString();
				//String quantidade = dbo.get("quantidade").toString();

				//linha = linha +ano+";"+quantidade+"\n";
			}		
			return linha;
		}
		
		public String formaEvasaoPorTurma(int ano_ingresso, String forma_evasao){
			String linha="";
		    DBObject match = new BasicDBObject("$match", new BasicDBObject("ano_ingresso",ano_ingresso));
		    DBObject match2 = new BasicDBObject("$match", new BasicDBObject("forma_saida",forma_evasao));

			DBObject id = new BasicDBObject();
			id.put("curso","$cod_curso");
			id.put("ano_saida", "$ano_saida");
			id.put("id_aluno", "$id_aluno");
			  
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			
			
			DBObject group = new BasicDBObject("$group", groupFields);
			DBObject id2 = new BasicDBObject();
			id2.put("ano_saida", "$_id.ano_saida");
			
			DBObject groupFields2 = new BasicDBObject();
			groupFields2.put("_id", id2);
			groupFields2.put("quantidade", new BasicDBObject("$sum", 1));
			//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
			DBObject group2 = new BasicDBObject("$group", groupFields2);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			
			List<DBObject> pipeline = Arrays.asList(match, match2, group, group2, sort);
			System.out.println(pipeline);
			AggregationOutput output = alunos.aggregate(pipeline);


			for (DBObject dbo : output.results()) {
				System.out.println(dbo);
				DBObject Getid = (DBObject) dbo.get("_id");
				String ano = Getid.get("ano_saida").toString();
				//String periodo = Getid.get("periodo").toString();
				String quantidade = dbo.get("quantidade").toString();

				linha = linha +ano+";"+quantidade+"\n";
			}		
			return linha;
		}
		
		public List<String> buscaDistintosDescricao(){
			List<String> list = new ArrayList<String>();
			list.add("aprovado");
			list.add("reprovado por nota");
			list.add("reprovado por frequencia");
			return list;

		}
		// ok
			public List<String> buscaDistintosAnoIngresso(){
			    String linha="";
				
				DBObject id = new BasicDBObject();
				id.put("ano_ingresso", "$ano_ingresso");
				//id.put("curso","$cod_curso");
				
				
				DBObject groupFields = new BasicDBObject();
				groupFields.put("_id", id);
				groupFields.put("quantidade", new BasicDBObject("$sum", 1));
				//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
				DBObject group = new BasicDBObject("$group", groupFields);
				
				DBObject sortFields = new BasicDBObject();
				sortFields.put("_id", 1);
				DBObject sort = new BasicDBObject("$sort", sortFields);
				//List<DBObject> pipeline = Arrays.asList(match,group,sort);
				List<DBObject> pipeline = Arrays.asList(group, sort);
				//System.out.println(pipeline);
				//List<DBObject> pipeline2 = Arrays.asList( pipeline);
				//System.out.println("pipeline 2 "+pipeline2);
				
				List<String> list = new ArrayList<String>();
				AggregationOutput output = alunos.aggregate(pipeline);

				for (DBObject dbo : output.results()) {
					//System.out.println("dbo: " +dbo);
					DBObject Getid = (DBObject) dbo.get("_id");
					String ano_ingresso = Getid.get("ano_ingresso").toString();

					list.add(ano_ingresso);
				}
				
				//System.out.println("String: " + linha);
				return list;
			
			}
			
			public List<String> buscaDistintosAnos(int ano_ingresso){
			    DBObject match = new BasicDBObject("$match", new BasicDBObject("ano_ingresso",ano_ingresso));

				
				DBObject id = new BasicDBObject();
				id.put("ano", "$ano");
				//id.put("curso","$cod_curso");
				
				
				DBObject groupFields = new BasicDBObject();
				groupFields.put("_id", id);
				groupFields.put("quantidade", new BasicDBObject("$sum", 1));
				//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
				DBObject group = new BasicDBObject("$group", groupFields);
				
				DBObject sortFields = new BasicDBObject();
				sortFields.put("_id", 1);
				DBObject sort = new BasicDBObject("$sort", sortFields);
				//List<DBObject> pipeline = Arrays.asList(match,group,sort);
				List<DBObject> pipeline = Arrays.asList(match, group, sort);
				//System.out.println(pipeline);
				//List<DBObject> pipeline2 = Arrays.asList( pipeline);
				//System.out.println("pipeline 2 "+pipeline2);
				
				List<String> list = new ArrayList<String>();
				AggregationOutput output = alunos.aggregate(pipeline);

				for (DBObject dbo : output.results()) {
					//System.out.println("dbo: " +dbo);
					DBObject Getid = (DBObject) dbo.get("_id");
					String ano = Getid.get("ano").toString();

					list.add(ano);
				}
				
				//System.out.println("String: " + linha);
				return list;
			
			}
			//ok
			public List<String> buscaDistintosFormasIngresso(){
			    DBObject match = new BasicDBObject("$match", new BasicDBObject("forma_ingresso",new BasicDBObject("$ne", null)));

				DBObject id = new BasicDBObject();
				id.put("forma_ingresso", "$forma_ingresso");
				//id.put("curso","$cod_curso");
				
				
				DBObject groupFields = new BasicDBObject();
				groupFields.put("_id", id);
				groupFields.put("quantidade", new BasicDBObject("$sum", 1));
				//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
				DBObject group = new BasicDBObject("$group", groupFields);
				
				DBObject sortFields = new BasicDBObject();
				sortFields.put("_id", 1);
				DBObject sort = new BasicDBObject("$sort", sortFields);
				//List<DBObject> pipeline = Arrays.asList(match,group,sort);
				List<DBObject> pipeline = Arrays.asList(match,group, sort);
				//System.out.println(pipeline);
				//List<DBObject> pipeline2 = Arrays.asList( pipeline);
				//System.out.println("pipeline 2 "+pipeline2);
				
				List<String> list= new ArrayList<String>();
				AggregationOutput output = alunos.aggregate(pipeline);
				for (DBObject dbo : output.results()) {
					System.out.println("dbo: " +dbo);
					DBObject Getid = (DBObject) dbo.get("_id");
					String forma_saida = Getid.get("forma_ingresso").toString();
					//Getid.get("forma_saida");
					
					list.add(forma_saida);
				}
				return list;
			
			}
			public List<String> buscaDistintosFormasEvasao(int ano_ingresso){
			    DBObject match = new BasicDBObject("$match", new BasicDBObject("forma_saida",new BasicDBObject("$ne", null)));
			    DBObject match2 = new BasicDBObject("$match", new BasicDBObject("ano_ingresso",ano_ingresso));

				DBObject id = new BasicDBObject();
				id.put("forma_saida", "$forma_saida");
				//id.put("curso","$cod_curso");
				
				
				DBObject groupFields = new BasicDBObject();
				groupFields.put("_id", id);
				groupFields.put("quantidade", new BasicDBObject("$sum", 1));
				//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
				DBObject group = new BasicDBObject("$group", groupFields);
				
				DBObject sortFields = new BasicDBObject();
				sortFields.put("_id", 1);
				DBObject sort = new BasicDBObject("$sort", sortFields);
				//List<DBObject> pipeline = Arrays.asList(match,group,sort);
				List<DBObject> pipeline = Arrays.asList(match,match2,group, sort);
				//System.out.println(pipeline);
				//List<DBObject> pipeline2 = Arrays.asList( pipeline);
				//System.out.println("pipeline 2 "+pipeline2);
				
				List<String> list= new ArrayList<String>();
				AggregationOutput output = alunos.aggregate(pipeline);
				for (DBObject dbo : output.results()) {
					System.out.println("dbo: " +dbo);
					DBObject Getid = (DBObject) dbo.get("_id");
					String forma_saida = Getid.get("forma_saida").toString();
					//Getid.get("forma_saida");
					
					list.add(forma_saida);
				}
				return list;
			
			}
			
			public List<String> buscaDistintosPeriodos(int ano_ingresso){
			    DBObject match = new BasicDBObject("$match", new BasicDBObject("ano_ingresso",ano_ingresso));

				DBObject id = new BasicDBObject();
				id.put("periodo", "$periodo");				
				
				DBObject groupFields = new BasicDBObject();
				groupFields.put("_id", id);
				groupFields.put("quantidade", new BasicDBObject("$sum", 1));
				DBObject group = new BasicDBObject("$group", groupFields);
				
				DBObject sortFields = new BasicDBObject();
				sortFields.put("_id", 1);
				DBObject sort = new BasicDBObject("$sort", sortFields);
				List<DBObject> pipeline = Arrays.asList(match,group, sort);
				
				List<String> list= new ArrayList<String>();
				AggregationOutput output = alunos.aggregate(pipeline);
				for (DBObject dbo : output.results()) {
					System.out.println("dbo: " +dbo);
					DBObject Getid = (DBObject) dbo.get("_id");
					String periodo = Getid.get("periodo").toString();					
					list.add(periodo);
				}
				
				return list;
			}
		
		public String evasaoDeAlunosPorAnoIngressoComEvasao(int ano_ingresso){
		    String linha="";
			DBObject match = new BasicDBObject("$match", new BasicDBObject("ano_ingresso",ano_ingresso));
			DBObject match2 = new BasicDBObject("$match", new BasicDBObject("forma_saida", new BasicDBObject("$ne",null)));

			DBObject id = new BasicDBObject();
			//id.put("ano_ingresso", "$ano_ingresso");
			//id.put("forma_saida", "$forma_saida");
			
			id.put("ano_saida", "$ano_saida");
			id.put("curso","$cod_curso");
			id.put("id_aluno", "$id_aluno");
			
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			
			DBObject id2 = new BasicDBObject();
			id2.put("ano_saida", "$_id.ano_saida");
			//id2.put("forma_saida", "$_id.forma_saida");

			//id2.put("ano", "$_id.ano");
			//id2.put("periodo", "$_id.periodo");
			
			DBObject groupFields2 = new BasicDBObject();
			groupFields2.put("_id", id2);
			groupFields2.put("quantidade", new BasicDBObject("$sum", 1));
			//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
			DBObject group2 = new BasicDBObject("$group", groupFields2);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			//List<DBObject> pipeline = Arrays.asList(match,group,sort);
			List<DBObject> pipeline = Arrays.asList(match, match2, group, group2,sort);
			//System.out.println(pipeline);
			//List<DBObject> pipeline2 = Arrays.asList( pipeline);
			//System.out.println("pipeline 2 "+pipeline2);
			
			
			AggregationOutput output = alunos.aggregate(pipeline);

			for (DBObject dbo : output.results()) {
				System.out.println("dbo: " +dbo);
//				DBObject Getid = (DBObject) dbo.get("_id");
//				String ano = Getid.get("ano_saida").toString();
//
//				String periodo = " ";
//				String qntAlunos = dbo.get("quantidade").toString();
//				linha = linha + periodo + ";" + ano + ";"+qntAlunos+"\n";
			}
			
			//
			//System.out.println("String: " + linha);
			return linha;
		
		}
		public String evasaoDeAlunosPorAnoIngressoComEvasoes(int ano_ingresso){
		    String linha="";
			DBObject match = new BasicDBObject("$match", new BasicDBObject("ano_ingresso",ano_ingresso));
			DBObject match2 = new BasicDBObject("$match", new BasicDBObject("forma_saida", new BasicDBObject("$ne",null)));

			DBObject id = new BasicDBObject();
			//id.put("ano_ingresso", "$ano_ingresso");
			//id.put("forma_saida", "$forma_saida");
			
			id.put("ano_saida", "$ano_saida");
			id.put("curso","$cod_curso");
			id.put("id_aluno", "$id_aluno");
			id.put("forma_saida", "$forma_saida");
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			
			DBObject id2 = new BasicDBObject();
			id2.put("ano_saida", "$_id.ano_saida");
			id2.put("forma_saida", "$_id.forma_saida");

			//id2.put("ano", "$_id.ano");
			//id2.put("periodo", "$_id.periodo");
			
			DBObject groupFields2 = new BasicDBObject();
			groupFields2.put("_id", id2);
			groupFields2.put("quantidade", new BasicDBObject("$sum", 1));
			//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
			DBObject group2 = new BasicDBObject("$group", groupFields2);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			//List<DBObject> pipeline = Arrays.asList(match,group,sort);
			List<DBObject> pipeline = Arrays.asList(match, match2, group, group2,sort);
			//System.out.println(pipeline);
			//List<DBObject> pipeline2 = Arrays.asList( pipeline);
			//System.out.println("pipeline 2 "+pipeline2);
			
			
			AggregationOutput output = alunos.aggregate(pipeline);

			for (DBObject dbo : output.results()) {
				System.out.println("dbo: " +dbo);
//				DBObject Getid = (DBObject) dbo.get("_id");
//				String ano = Getid.get("ano_saida").toString();
//
//				String periodo = Getid.get("forma_saida").toString();;
//				String qntAlunos = dbo.get("quantidade").toString();
//				linha = linha + periodo + ";" + ano + ";"+qntAlunos+"\n";
			}
			
			//
			//System.out.println("String: " + linha);
			return linha;
		
		}
		public String evasaoDeAlunosPorAnoIngresso(int ano_ingresso){
		    String linha="";
			DBObject match = new BasicDBObject("$match", new BasicDBObject("ano_ingresso", ano_ingresso));
			//DBObject match2 = new BasicDBObject("$match", new BasicDBObject("forma_saida", null));

			DBObject id = new BasicDBObject();
			//id.put("ano_ingresso", "$ano_ingresso");
			id.put("ano", "$ano");
			id.put("periodo", "$periodo");
			id.put("curso","$cod_curso");
			id.put("id_aluno", "$id_aluno");
			
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			
			DBObject id2 = new BasicDBObject();
			id2.put("ano_ingresso", "$_id.ano_ingresso");
			id2.put("ano", "$_id.ano");
			id2.put("periodo", "$_id.periodo");
			
			DBObject groupFields2 = new BasicDBObject();
			groupFields2.put("_id", id2);
			groupFields2.put("quantidade", new BasicDBObject("$sum", 1));
			//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
			DBObject group2 = new BasicDBObject("$group", groupFields2);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			//List<DBObject> pipeline = Arrays.asList(match,group,sort);
			List<DBObject> pipeline = Arrays.asList(match,group, group2, sort);
			//System.out.println(pipeline);
			//List<DBObject> pipeline2 = Arrays.asList( pipeline);
			//System.out.println("pipeline 2 "+pipeline2);
			
			
			AggregationOutput output = alunos.aggregate(pipeline);

			for (DBObject dbo : output.results()) {
				System.out.println("dbo: " +dbo);
//				DBObject Getid = (DBObject) dbo.get("_id");
//				String ano = Getid.get("ano").toString();
//				String periodo = Getid.get("periodo").toString();
//				String qntAlunos = dbo.get("quantidade").toString();
//				linha = linha +ano+";"+periodo+";"+qntAlunos+"\n";
			}
			
			//System.out.println("String: " + linha);
			return linha;
		
		}
		
		public String evasaoDeAlunosPorAnoIngresso2(){
		    String linha="";

			DBObject match = new BasicDBObject("$match", new BasicDBObject("ano_ingresso",2010));
			
			DBObject id = new BasicDBObject();
			//id.put("ano_ingresso", "$ano_ingresso");
			id.put("ano", "$ano");
			id.put("periodo", "$periodo");
			id.put("curso","$cod_curso");
			id.put("id_aluno", "$id_aluno");
			
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade disciplina", new BasicDBObject("$sum", 1));
			//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			DBObject fields = new BasicDBObject();
			fields.put("_id", "ano"); // comment out for id option

			DBObject id2 = new BasicDBObject();
			id2.put("ano", "$_id.ano");
			//id2.put("periodo", "$_id.periodo");
			id2.put("quantidade disciplina", "$quantidade disciplina");
			
			DBObject groupFields2 = new BasicDBObject();
			groupFields2.put("_id", id2);
			groupFields2.put("quantidade 2", new BasicDBObject("$addToSet", "$_id.ano"));
			//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
			DBObject group2 = new BasicDBObject("$group", groupFields2);
			
			DBObject id3 = new BasicDBObject();
			//id2.put("ano", "$_id.ano");
			//id2.put("periodo", "$_id.periodo");
			id3.put("quantidade disciplina", "$_id.quantidade disciplina");
			
			DBObject groupFields3 = new BasicDBObject();
			groupFields3.put("_id", id3);
			groupFields3.put("anos", new BasicDBObject("$addToSet", "$_id.ano"));
			//groupFields.put("quantidade2", new BasicDBObject("$addToSet", 1));
			DBObject group3 = new BasicDBObject("$group", groupFields3);

			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			//List<DBObject> pipeline = Arrays.asList(match,group,sort);
			List<DBObject> pipeline = Arrays.asList(match, group, group2, group3, sort);
			//System.out.println(pipeline);
			//List<DBObject> pipeline2 = Arrays.asList( pipeline);
			//System.out.println("pipeline 2 "+pipeline2);
			
			
			AggregationOutput output = alunos.aggregate(pipeline);

			for (DBObject dbo : output.results()) {
				System.out.println("dbo: " +dbo);
				//DBObject Getid = (DBObject) dbo.get("_id");
				String anos = dbo.get("anos").toString();
				DBObject Getid = (DBObject) dbo.get("_id");
				String qntdisciplinas = Getid.get("quantidade disciplina").toString();
				linha = linha +anos+"//"+qntdisciplinas+"\n";
			}		
			return linha;
		
		}
		
		public List<String> mediaDeFormadosPorAnoIngresso(){
			
		    //DBObject match = new BasicDBObject("$match", new BasicDBObject("forma_evasao", "formado"));
		
			DBObject id = new BasicDBObject();
			id.put("curso","$cod_curso");

			id.put("ano_ingresso", "$ano_ingresso");
			//id.put("forma_evasao","$forma_evasao");
			
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
					
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			
			//List<DBObject> pipeline = Arrays.asList(match,group,sort);
			List<DBObject> pipeline = Arrays.asList(group, sort);
			System.out.println(pipeline);
			AggregationOutput output = alunos.aggregate(pipeline);

			List<String> list = new ArrayList<String>();

			for (DBObject dbo : output.results()) {
				System.out.println("dbo: " +dbo);
				String motivo_evasao = dbo.get("quantidade").toString();
				list.add(motivo_evasao);
			}		
			return list;
		}
		public String disciplinaNavalhaPorSemestre(int anoIngresso, int ano, String periodo){
			   String linha = ""; 
				
				DBObject match = new BasicDBObject("$match", new BasicDBObject("ano_ingresso",anoIngresso));
			    DBObject match2 = new BasicDBObject("$match", new BasicDBObject("ano", ano));
			    DBObject match3 = new BasicDBObject("$match", new BasicDBObject("periodo", periodo));
			    DBObject match4 = new BasicDBObject("$match", new BasicDBObject("descricao_situacao", new BasicDBObject("$ne", null)));
			    
			    DBObject id = new BasicDBObject();
				id.put("curso","$cod_curso");
				//id.put("ano_ingresso", "$ano_ingresso");
				//id.put("ano", "$ano");
				//id.put("periodo", "$periodo");
			    id.put("nome_ativ_curricular", "$nome_ativ_curricular");
			    //id.put("descr_situacao ", "$descr_situacao ");

				DBObject groupFields = new BasicDBObject();
				groupFields.put("_id", id);
				groupFields.put("quantidade", new BasicDBObject("$sum", 1));
				DBObject group = new BasicDBObject("$group", groupFields);
				
				DBObject sortFields = new BasicDBObject();
				sortFields.put("_id", 1);
				DBObject sort = new BasicDBObject("$sort", sortFields);
				
				List<DBObject> pipeline = Arrays.asList(match, match2,match3,match4,group,sort);
				System.out.println(pipeline);
				AggregationOutput output = alunos.aggregate(pipeline); 


				for (DBObject dbo : output.results()) {
					System.out.println(dbo);
//					DBObject Getid = (DBObject) dbo.get("_id");
//					String atividade = Getid.get("nome_ativ_curric").toString();
//					String qntSituacao = dbo.get("quantidade").toString();
//					linha = linha +atividade+";"+qntSituacao+"\n";
				}
				
				
				return linha;
			}
		public String demonstracaoDeSituacaoEmCadaDisciplina(String disciplina, int anoIngresso, int ano, String periodo){
			
		    DBObject match1 = new BasicDBObject("$match", new BasicDBObject("nome_ativ_curricular", disciplina));
		
		    String linha = ""; 
			
			DBObject match2 = new BasicDBObject("$match", new BasicDBObject("ano_ingresso",anoIngresso));
		    DBObject match3 = new BasicDBObject("$match", new BasicDBObject("ano", ano));
		    DBObject match4 = new BasicDBObject("$match", new BasicDBObject("periodo", periodo));
		    //DBObject match5 = new BasicDBObject("$match", new BasicDBObject("descr_situacao ", descr_situacao));
		    
		    DBObject id = new BasicDBObject();
			id.put("curso","$cod_curso");
			//id.put("ano_ingresso", "$ano_ingresso");
			//id.put("ano", "$ano");
			//id.put("periodo", "$periodo");
		    //id.put("nome_ativ_curric", "$nome_ativ_curric");
		    id.put("descricao_situacao", "$descricao_situacao");

			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			
			List<DBObject> pipeline = Arrays.asList(match1, match2,match3, match4, group,sort);
			System.out.println(pipeline);
			AggregationOutput output = alunos.aggregate(pipeline); 


			for (DBObject dbo : output.results()) {
				System.out.println(dbo);
//				DBObject Getid = (DBObject) dbo.get("_id");
//				String atividade = Getid.get("descr_situacao ").toString();
//				String qntSituacao = dbo.get("quantidade").toString();
//				linha = linha +atividade+";"+qntSituacao+"\n";
			}
			
			
			return linha;
		}
		
		public String demonstracaoDeSituacaoEmTodasDisciplinas(int anoIngresso, int ano, String periodo){
			
		
		    String linha = ""; 
			
			DBObject match2 = new BasicDBObject("$match", new BasicDBObject("ano_ingresso",anoIngresso));
		    DBObject match3 = new BasicDBObject("$match", new BasicDBObject("ano", ano));
		    DBObject match4 = new BasicDBObject("$match", new BasicDBObject("periodo", periodo));
		    DBObject match5 = new BasicDBObject("$match", new BasicDBObject("descricao_situacao", new BasicDBObject("$ne", null )));
		    
		    DBObject id = new BasicDBObject();
			id.put("curso","$cod_curso");
		    id.put("nome_ativ_curricular", "$nome_ativ_curricular");
			//id.put("ano_ingresso", "$ano_ingresso");
			//id.put("ano", "$ano");
			//id.put("periodo", "$periodo");
		    //id.put("nome_ativ_curric", "$nome_ativ_curric");
		    id.put("descricao_situacao ", "$descricao_situacao");

			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);
			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			
			List<DBObject> pipeline = Arrays.asList(match2,match3, match4, match5, group,sort);
			System.out.println(pipeline);
			AggregationOutput output = alunos.aggregate(pipeline); 


			for (DBObject dbo : output.results()) {
				System.out.println(dbo);
//				DBObject Getid = (DBObject) dbo.get("_id");
//				String disciplina = Getid.get("nome_ativ_curric").toString();
//				String atividade = Getid.get("descr_situacao ").toString();
//				String qntSituacao = dbo.get("quantidade").toString();
//				linha = linha +disciplina+";"+atividade+";"+qntSituacao+"\n";
			}
			
			
			return linha;
		}
		//Filtro para achar todos os alunos que estão no perfil de evasao
		public String perfilEvasao(){
			String linha="";
			DBObject id = new BasicDBObject();
			id.put("ano", "$ano");
			id.put("periodo", "$periodo");
			id.put("curso","$cod_curso");
			id.put("id_aluno", "$id_aluno");
			//id.put("media_final",new BasicDBObject("$gt", notaInicial));
			
			DBObject groupFields = new BasicDBObject();
			groupFields.put("_id", id);

			groupFields.put("quantidade", new BasicDBObject("$sum", 1));
			DBObject group = new BasicDBObject("$group", groupFields);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("_id", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
			
			DBObject sortFields2 = new BasicDBObject();
			sortFields2.put("_id.id_aluno", 1);
			DBObject sort2 = new BasicDBObject("$sort", sortFields2);
			
			List<DBObject> pipeline = Arrays.asList(group, sort, sort2);
			System.out.println("pipeline " + pipeline);
			
			
			AggregationOutput output = alunos.aggregate(pipeline);
						
			for (DBObject dbo : output.results()) {
				System.out.println("dbo: " +dbo);
//				DBObject Getid = (DBObject) dbo.get("_id");
//				String ano = Getid.get("ano").toString();
//				String periodo = Getid.get("periodo").toString();
//				String id_aluno = Getid.get("id_aluno").toString();
//				linha = linha +ano+";"+periodo+";"+id+"\n";
				
				//list.add(ano);
				//list.add(periodo);
				//list.add(media);
				//System.out.println(list.get(0));
			} 
			
			return linha;
		}
		//Filtro para achar todos os alunos que estão no perfil de evasao
				public String perfilJubiladoTeste(){
				    DBObject match = new BasicDBObject("$match", new BasicDBObject("forma_saida", "jubilado (crit. 01)"));

					String linha="";
					DBObject id = new BasicDBObject();
					id.put("curso","$cod_curso");

					id.put("id_aluno", "$id_aluno");
					//id.put("media_final",new BasicDBObject("$gt", notaInicial));
					
					DBObject groupFields = new BasicDBObject();
					groupFields.put("_id", id);

					groupFields.put("quantidade", new BasicDBObject("$sum", 1));
					DBObject group = new BasicDBObject("$group", groupFields);
					
					DBObject sortFields = new BasicDBObject();
					sortFields.put("_id", 1);
					DBObject sort = new BasicDBObject("$sort", sortFields);
					
					
					
					List<DBObject> pipeline = Arrays.asList(match,group, sort);
					System.out.println("pipeline " + pipeline);
					
					
					AggregationOutput output = alunos.aggregate(pipeline);
					int soma=0;
					for (DBObject dbo : output.results()) {
						System.out.println("dbo: " +dbo);
						soma =  soma + Integer.parseInt(dbo.get("quantidade").toString());
						
//						String periodo = Getid.get("periodo").toString();
//						String id_aluno = Getid.get("id_aluno").toString();
//						linha = linha +ano+";"+periodo+";"+id+"\n";
						
						//list.add(ano);
						//list.add(periodo);
						//list.add(media);
						//System.out.println(list.get(0));
					} 
					System.out.println(soma);
					return linha;
				}
		//Filtro para achar todos os alunos que estão no perfil de jubilado
				public String perfilJubilado(){
					String linha="";
				    DBObject match = new BasicDBObject("$match", new BasicDBObject("forma_saida", "jubilado (crit. 01)"));
					DBObject id = new BasicDBObject();
					id.put("curso","$cod_curso");
					id.put("id_aluno", "$id_aluno");
					//id.put("media_final",new BasicDBObject("$gt", notaInicial));
					
					DBObject groupFields = new BasicDBObject();
					groupFields.put("_id", id);

					groupFields.put("quantidade", new BasicDBObject("$sum", 1));
					DBObject group = new BasicDBObject("$group", groupFields);
					
					DBObject sortFields = new BasicDBObject();
					sortFields.put("_id", 1);
					DBObject sort = new BasicDBObject("$sort", sortFields);
					
					DBObject sortFields2 = new BasicDBObject();
					sortFields2.put("_id.id_aluno", 1);
					DBObject sort2 = new BasicDBObject("$sort", sortFields2);
					
					List<DBObject> pipeline = Arrays.asList(match, group, sort, sort2);
					System.out.println("pipeline " + pipeline);
					
					
					AggregationOutput output = alunos.aggregate(pipeline);
					FileWriter writer;
					String linhaDeDados;
					int soma = 0;
					try{
						writer = new FileWriter("consultaJubiladoCC.txt");
						for (DBObject dbo : output.results()) {
								System.out.println("dbo: " +dbo);
								soma =  soma + Integer.parseInt(dbo.get("quantidade").toString());

								DBObject Getid = (DBObject) dbo.get("_id");
								String id_aluno = Getid.get("id_aluno").toString();
							    DBObject matchid = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
							    DBObject id_idAluno = new BasicDBObject();
							    id_idAluno.put("curso","$cod_curso");

							    id_idAluno.put("nome_curso","$nome_curso");
							    id_idAluno.put("versao_curso","$versao_curso");

							    id_idAluno.put("cod_ativ_curricular","$cod_ativ_curric");
							    id_idAluno.put("nome_ativ_curricular","$nome_ativ_curric");
							    id_idAluno.put("media_final","$media_final");
							    id_idAluno.put("descr_situacao","$descr_situacao ");
							    id_idAluno.put("ano", "$ano");
							    id_idAluno.put("periodo", "$periodo");
							    id_idAluno.put("creditos", "$creditos");
							    id_idAluno.put("carga_horariaTeorica","$carga_horariaTeorica");
							    id_idAluno.put("carga_horariaPratica","$carga_horariaPratica");
							    id_idAluno.put("forma_ingresso","$forma_ingresso");
							    id_idAluno.put("ano_ingresso","$ano_ingresso");
							    id_idAluno.put("forma_saida","$forma_saida");
							    id_idAluno.put("ano_saida","$ano_saida");
		
								
								DBObject groupFieldsIdAluno = new BasicDBObject();
								groupFieldsIdAluno.put("_id", id_idAluno);
		
								groupFieldsIdAluno.put("quantidade", new BasicDBObject("$sum", 1));
								DBObject groupidAluno = new BasicDBObject("$group", groupFieldsIdAluno);
								
								DBObject sortFieldsidAluno = new BasicDBObject();
								sortFieldsidAluno.put("_id", 1);
								DBObject sortaluno = new BasicDBObject("$sort", sortFieldsidAluno);
								
								
								
								List<DBObject> pipelinealuno = Arrays.asList(matchid, groupidAluno, sortaluno);
								System.out.println("pipeline " + pipelinealuno);
								
								
								AggregationOutput outputaluno = alunos.aggregate(pipelinealuno);
								
								for (DBObject dboaluno : outputaluno.results()) {
									linhaDeDados="";
									linhaDeDados= linhaDeDados + id_aluno + ";";
									System.out.println("dboaluno: " +dboaluno);
									DBObject Getidaluno = (DBObject) dboaluno.get("_id");
									String curso = Getidaluno.get("curso").toString();
									String nome_curso = Getidaluno.get("nome_curso").toString();
									String versao_curso = Getidaluno.get("versao_curso").toString();
									String cod_ativ_curricular = Getidaluno.get("cod_ativ_curricular").toString();
									String nome_ativ_curricular = Getidaluno.get("nome_ativ_curricular").toString();
									String media_final="0";
									if(Getidaluno.get("media_final")!=null){
										media_final = Getidaluno.get("media_final").toString();

									}
									String descr_situacao = Getidaluno.get("descr_situacao").toString();

									String ano = Getidaluno.get("ano").toString();
									String periodo = Getidaluno.get("periodo").toString();
									String creditos = Getidaluno.get("creditos").toString();
									String cargaHorariaPratica = "0";
									if(Getidaluno.get("carga_horariaPratica")!=null){
										cargaHorariaPratica = Getidaluno.get("carga_horariaPratica").toString();

									}
									String cargaHorariaTeorica = "0";
									if(Getidaluno.get("carga_horariaTeorica")!=null){
										cargaHorariaPratica = Getidaluno.get("carga_horariaTeorica").toString();

									}									
									String forma_ingresso = Getidaluno.get("forma_ingresso").toString();
									String ano_ingresso = Getidaluno.get("ano_ingresso").toString();
									String forma_saida = Getidaluno.get("forma_saida").toString();
									String ano_saida = Getidaluno.get("ano_saida").toString();
									linhaDeDados= linhaDeDados + curso + ";";
									linhaDeDados= linhaDeDados + nome_curso + ";";
									linhaDeDados= linhaDeDados + versao_curso + ";";
									linhaDeDados= linhaDeDados + cod_ativ_curricular + ";";
									linhaDeDados= linhaDeDados + nome_ativ_curricular + ";";

									linhaDeDados= linhaDeDados + media_final + ";";
									linhaDeDados= linhaDeDados + ano + ";";
									linhaDeDados= linhaDeDados + periodo + ";";
									linhaDeDados= linhaDeDados + creditos + ";";
									linhaDeDados= linhaDeDados + cargaHorariaPratica + ";";
									linhaDeDados= linhaDeDados + cargaHorariaTeorica + ";";
									linhaDeDados= linhaDeDados + forma_ingresso + ";";
									linhaDeDados= linhaDeDados + ano_ingresso + ";";
									linhaDeDados= linhaDeDados + forma_saida + ";";
									linhaDeDados= linhaDeDados + ano_saida + ";";
									
									writer.write(linhaDeDados+"\n");
								}
								
							}
							writer.close();
						}catch(IOException ex){
							ex.printStackTrace();
						}
						
						System.out.println(soma);
						
//						linha = linha +ano+";"+periodo+";"+id+"\n";
						
						//list.add(ano);
						//list.add(periodo);
						//list.add(media);
						//System.out.println(list.get(0));
					return linha;

					} 
					
		public static void main(String args[]){
			AtividadeAcademica ativ= new AtividadeAcademica("sistemas de informacao");
			/*int num = Integer.parseInt(ativ.aprovadosDisciplina().get(0));
			System.out.println("int: " +  num);
			*/
//			System.out.println(ativ.aprovadosDisciplina("algoritmos e estruturas de dados  i", 0, 2.5));
//			System.out.println(ativ.aprovadosDisciplina("algoritmos e estruturas de dados  i", 2.5, 5));
//			System.out.println(ativ.aprovadosDisciplina("algoritmos e estruturas de dados  i", 5, 7.5));
//			System.out.println(ativ.aprovadosDisciplina("algoritmos e estruturas de dados  i", 7.5, 10));
			
			//System.out.println("media de formados por ano de ingresso");
			//System.out.println(ativ.mediaDeFormadosPorAnoIngresso());
			//System.out.println("media de aprovados em cada disciplina");
			//System.out.println(ativ.mediaDeAprovadosEmCadaDisciplina("faa011",5,10));
			System.out.println("disciplina navalha");
			
			System.out.println(ativ.disciplinaNavalhaPorSemestre(2014, 2014, "1A"));
			
			System.out.println("situações em todas as disciplinas");
			System.out.println(ativ.demonstracaoDeSituacaoEmTodasDisciplinas(2014, 2014, "1A"));
	//System.out.println("");
	
//			System.out.println("media de aprovados por Disciplina");
//			System.out.println(ativ.mediaDeAprovadosDisciplina(5,10));
			//System.out.println("disciplina navalha");
			//System.out.println(ativ.buscaDistintosFormasIngresso());
//			System.out.println("distribuicaoo de saida por ano de ingresso");
			//System.out.println(ativ.formaEvasaoPorTurma());
//			System.out.println("teste1");
//			System.out.println(ativ.disciplinaNavalhaPorSemestre(2010, 2010, "1A"));
//			System.out.println("Teste 1");
		//System.out.println(ativ.evasaoDeAlunosPorAnoIngressoComEvasoes(2009));
//			System.out.println("");
//			System.out.println("Teste 2");
			//System.out.println(ativ.evasaoDeAlunosPorAnoIngressoComEvasoes(2008));
//			System.out.println("teste");
//			System.out.println(ativ.demonstracaoDeSituacaoEmTodasDisciplinas(2010, 2010, "1A"));
//	
			
//			System.out.println(ativ.buscaDistintosAnoIngresso());
//			System.out.println(ativ.buscaDistintosAnos(2008));
//
			//System.out.println(ativ.buscaDistintosAnoIngresso());
//			System.out.println(ativ.perfilJubilado());
		}
}
