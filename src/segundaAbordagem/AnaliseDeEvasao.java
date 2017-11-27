package segundaAbordagem;

import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import dao.mongo.MongoConnection;

public class AnaliseDeEvasao {
	private DBCollection alunos;
	private String current_curso;
	private String id_aluno_representante;
	private List<String> list_excluidos_perfil;
	
	public AnaliseDeEvasao(String curso){
		current_curso = curso;
			alunos = null;
				try {
					alunos = MongoConnection.getInstance().getDB().getCollection("atividades_academica_"+curso.toLowerCase().trim().replace(" ", "_"));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}
	
	public List<String> filterByEvasao(double porcentagemConjunto){
		DBObject match = new BasicDBObject("$match", new BasicDBObject("forma_saida",new BasicDBObject("$ne", "formado")));
		DBObject match2 = new BasicDBObject("$match", new BasicDBObject("forma_saida",new BasicDBObject("$ne", "jubilado (crit. 02)")));
		DBObject match3 = new BasicDBObject("$match", new BasicDBObject("forma_saida",new BasicDBObject("$ne", "jubilado (crit. 01)")));
		DBObject match4 = new BasicDBObject("$match", new BasicDBObject("forma_saida",new BasicDBObject("$ne", "desistente")));
		DBObject match5 = new BasicDBObject("$match", new BasicDBObject("forma_saida",new BasicDBObject("$ne", "transferido")));
		DBObject match6 = new BasicDBObject("$match", new BasicDBObject("forma_saida",new BasicDBObject("$ne", "excluido")));

		DBObject id = new BasicDBObject();
		id.put("curso","$cod_curso");
		id.put("id_aluno", "$id_aluno");
		id.put("ano", "$ano");
		id.put("periodo", "$periodo");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
		
		DBObject sortFields = new BasicDBObject();
		sortFields.put("_id", 1);
		DBObject sort = new BasicDBObject("$sort", sortFields);
					
		List<DBObject> pipeline = Arrays.asList(match, match2, match3, match4, match5, match6, group, sort);
		AggregationOutput output = alunos.aggregate(pipeline);
								
		List<String> listaEvasaoGeral = new ArrayList<String>();
		List<String> listaAlunosConjunto = new ArrayList<String>();
		List<String> listaAlunosForaConjunto = new ArrayList<String>();
					
		Map<String, List<String>> listaAtividadesAlunos = new LinkedHashMap<String, List<String>>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " + dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String id_aluno = getId.get("id_aluno").toString();
			String ano = getId.get("ano").toString();
			String periodo = getId.get("periodo").toString();
			
			List<String> listaPeriodos = new ArrayList<String>();
			
			if (listaAtividadesAlunos.containsKey(id_aluno)) {
				listaPeriodos = listaAtividadesAlunos.get(id_aluno);
				listaPeriodos.add(ano+"/"+periodo);
				listaAtividadesAlunos.put(id_aluno, listaPeriodos);
			} else {
				listaPeriodos.add(ano+"/"+periodo);
				listaAtividadesAlunos.put(id_aluno, listaPeriodos);
			}
		}
		
		Set<String> setIdAlunos = listaAtividadesAlunos.keySet();
		Iterator<String> iterator = setIdAlunos.iterator();
					
		while (iterator.hasNext()) {
		    String id_aluno = iterator.next();
		    List<String> periodos = listaAtividadesAlunos.get(id_aluno);
		    String [] itemAtual = periodos.get(listaAtividadesAlunos.get(id_aluno).size()-1).split("/");
		    int ano = 0;
		    String periodo = "";
		    if (itemAtual[1].equals("1A") || itemAtual[1].equals("1F")) {
		    	ano = Integer.parseInt(itemAtual[0]) + 2;
		    	periodo = "2A";
		    } else if (itemAtual[1].equals("2A") || itemAtual[1].equals("2F")) {
		    	ano = Integer.parseInt(itemAtual[0]) + 3;
		    	periodo = "1A";
		    }
		    
		    if (ano <= 2015 && (periodo.equals("1A") || periodo.equals("2A"))) {
		    	listaEvasaoGeral.add(id_aluno);
		    }
		}
		
		int count = 1;
		BasicDBObject document = new BasicDBObject();
		DBCollection trainingSet = null;
		
		// training_set2 com um aumento de alunos do conjunto
		try {
			trainingSet = MongoConnection.getInstance().getDB().getCollection(
			"training_setevasao"+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("size");
		int size = (int)((double)listaEvasaoGeral.size() * porcentagemConjunto);
		System.out.println(size);
		
		for (int i = 0; i < listaEvasaoGeral.size(); i++) {
			String id_aluno = listaEvasaoGeral.get(i);
			
			if (count <= size) {
				listaAlunosConjunto.add(id_aluno);
				
				List<List<String>> result = new ArrayList<List<String>>();
				result = getDetailsAluno(id_aluno);
				
				for (int item = 0; item < result.size(); item++ ) {
					document = new BasicDBObject();
					List<String> activity = result.get(item);
					
					document.put("id_aluno", id_aluno);
					document.put("nome_curso", activity.get(0));
					document.put("cod_curso", activity.get(1));
					document.put("versao_curso", activity.get(2));
					document.put("cod_ativ_curricular", activity.get(3));
					document.put("nome_ativ_curricular", activity.get(4));
					
					if (activity.get(5) != " ") {
						document.put("media_final", Double.parseDouble(activity.get(5)));
					} else {
						document.put("media_final", 0);
					}
					
					document.put("descricao_situacao", activity.get(6));
					document.put("ano", Integer.parseInt(activity.get(7)));
					document.put("periodo", activity.get(8));
					document.put("creditos", Integer.parseInt(activity.get(9)));
					
					if (activity.get(10) != " ") {
						document.put("carga_horaria_teorica", Integer.parseInt(activity.get(10)));
					} else {
						document.put("carga_horaria_teorica", 0);
					}
					
					if (activity.get(11) != " ") {
						document.put("carga_horaria_pratica", Integer.parseInt(activity.get(11)));
					} else {
						document.put("carga_horaria_pratica", 0);
					}
					
					document.put("forma_ingresso", activity.get(12));
					document.put("ano_ingresso", Integer.parseInt(activity.get(13)));
					
//					trainingSet.save(document);	
				}
			} else {
				listaAlunosForaConjunto.add(id_aluno);
			}
			count ++;
		}
		
		list_excluidos_perfil = listaAlunosForaConjunto;
		
		System.out.println("LISTA DE EXCLUIDOS DO PERFIL");
		System.out.println(listaAlunosForaConjunto);
		System.out.println("LISTA DE ALUNOS DO CONJUNTO");
		System.out.println(listaAlunosConjunto);
		
		return listaAlunosConjunto;

	}
	
	public List<List<String>> getDetailsAluno(String id_aluno) {
		DBObject match = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));

		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("nome_curso", "$nome_curso");
		id.put("cod_curso", "$cod_curso");
		id.put("versao_curso", "$versao_curso");
		id.put("cod_ativ_curricular", "$cod_ativ_curricular");
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
		id.put("media_final", "$media_final");
		id.put("descricao_situacao", "$descricao_situacao");
		id.put("ano", "$ano");
		id.put("periodo", "$periodo");
		id.put("creditos", "$creditos");
		id.put("carga_horaria_teorica", "$carga_horaria_teorica");
		id.put("carga_horaria_pratica", "$carga_horaria_pratica");
		id.put("forma_ingresso", "$forma_ingresso");
		id.put("ano_ingresso", "$ano_ingresso");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(match, group);
		AggregationOutput output = alunos.aggregate(pipeline);
								
		List<List<String>> list = new ArrayList<List<String>>();
		
		for (DBObject dbo : output.results()) {
			DBObject getId = (DBObject) dbo.get("_id");
			String nome_curso = getId.get("nome_curso").toString();
			String cod_curso = getId.get("cod_curso").toString();
			String versao_curso = getId.get("versao_curso").toString();
			String cod_ativ_curricular = getId.get("cod_ativ_curricular").toString();
			String nome_ativ_curricular = getId.get("nome_ativ_curricular").toString();
			
			String media_final = " ";
			if (getId.get("media_final") != null) {
				media_final = getId.get("media_final").toString();
			}
			
			String descricao_situacao = getId.get("descricao_situacao").toString();
			String ano = getId.get("ano").toString();
			String periodo = getId.get("periodo").toString();
			String creditos = getId.get("creditos").toString();
			
			String carga_horaria_teorica = " ";
			if (getId.get("carga_horaria_teorica") != null) {
				carga_horaria_teorica = getId.get("carga_horaria_teorica").toString();
			}
			
			String carga_horaria_pratica = " ";
			if (getId.get("carga_horaria_pratica") != null) {
				carga_horaria_pratica = getId.get("carga_horaria_pratica").toString();
			}
			
			String forma_ingresso = getId.get("forma_ingresso").toString();
			String ano_ingresso = getId.get("ano_ingresso").toString();
			
			List<String> listInside = new ArrayList<String>();

			listInside.add(nome_curso);
			listInside.add(cod_curso);
			listInside.add(versao_curso);
			listInside.add(cod_ativ_curricular);
			listInside.add(nome_ativ_curricular);
			listInside.add(media_final);
			listInside.add(descricao_situacao);
			listInside.add(ano);
			listInside.add(periodo);
			listInside.add(creditos);
			listInside.add(carga_horaria_teorica);
			listInside.add(carga_horaria_pratica);
			listInside.add(forma_ingresso);
			listInside.add(ano_ingresso);
			
			list.add(listInside);
			
		}
		return list;
	}
	
//	public List<String> getDisciplinesDistintics(String forma_saida){
//		DBObject id = new BasicDBObject();
//		id.put("cod_ativ_curricular", "$cod_ativ_curricular");
//		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
//		
//		DBObject groupFields = new BasicDBObject();
//		groupFields.put("_id", id);
//		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
//		DBObject group = new BasicDBObject("$group", groupFields);
//		
//		DBCollection trainingSet = null;
//		
//		try {
//			trainingSet = MongoConnection.getInstance().getDB().getCollection(
//			"training_set"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
//		} catch (UnknownHostException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//					
//		List<DBObject> pipeline = Arrays.asList(group);
//		AggregationOutput output = trainingSet.aggregate(pipeline);
//								
//		List<String> list = new ArrayList<String>();
//		
//		for (DBObject dbo : output.results()) {
////			System.out.println("dbo: " +dbo);
//			DBObject getId = (DBObject) dbo.get("_id");
//			String cod_ativ_curricular = getId.get("cod_ativ_curricular").toString();
//			list.add(cod_ativ_curricular);
//		}
//		return list;
//	}
	
	public List<String> getDisciplinesDistintics(String forma_saida){
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("cod_ativ_curricular", "$cod_ativ_curricular");
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
		
		DBObject id2 = new BasicDBObject();
//		id2.put("cod_ativ_curricular", "$_id.cod_ativ_curricular");
		id2.put("nome_ativ_curricular", "$_id.nome_ativ_curricular");

		
		DBObject groupFields2 = new BasicDBObject();
		groupFields2.put("_id", id2);
		groupFields2.put("quantidade", new BasicDBObject("$sum", 1));
		
		DBObject group2 = new BasicDBObject("$group", groupFields2);
		
		DBCollection trainingSet = null;
		
		try {
			trainingSet = MongoConnection.getInstance().getDB().getCollection(
			"training_set"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
					
		List<DBObject> pipeline = Arrays.asList(group, group2);
		AggregationOutput output = trainingSet.aggregate(pipeline);
										
		List<String> listDisc = new ArrayList<String>();
		
		int size = (int)((double)getAlunos(forma_saida).size() * 0.5);

		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");

			int quantidade = Integer.parseInt(dbo.get("quantidade").toString());
			
			
			if (quantidade >= size) {
				String cod_ativ_curricular = getId.get("nome_ativ_curricular").toString();
				listDisc.add(cod_ativ_curricular);
			}
		}
		
//		System.out.println("size");
//		System.out.println(getAlunos(forma_saida).size());
		
		return listDisc;
	}
	
	public List<String> getAlunos(String forma_saida){
		DBCollection trainingSet = null;
		try {
			trainingSet = MongoConnection.getInstance().getDB().getCollection(
			"training_set"+forma_saida+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(group);
		AggregationOutput output = trainingSet.aggregate(pipeline);
								
		List<String> list = new ArrayList<String>();
		
		for (DBObject dbo : output.results()) {
			DBObject getId = (DBObject) dbo.get("_id");
			String id_aluno = getId.get("id_aluno").toString();
			list.add(id_aluno);
		}
		return list;
	}
	
	public double calculaMediaFinal(List<Double> notas) {
		double media = 0.0;
		double mediaTemp = 0.0;
		for (int i = 0; i < notas.size(); i++) {
			 mediaTemp = mediaTemp + notas.get(i);
		}
		media = mediaTemp / notas.size();
		
		return media;
	}
	
	public List<String> getAverage(String forma_saida, String id_aluno, String nome_ativ_curricular) {
		DBCollection trainingSet = null;
		try {
			trainingSet = MongoConnection.getInstance().getDB().getCollection(
			"training_set"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DBObject matchAluno = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		DBObject matchDisciplina = new BasicDBObject("$match", new BasicDBObject("nome_ativ_curricular", nome_ativ_curricular));

		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
		id.put("media_final", "$media_final");
		id.put("ano", "$ano");
		id.put("periodo", "$periodo");
		id.put("ano_ingresso", "$ano_ingresso");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(matchAluno, matchDisciplina, group);
		AggregationOutput output = trainingSet.aggregate(pipeline);
								
		String media_final = "0.0";
		String ano = "0";
		String periodo = "";
		String ano_ingresso = "0";
		
		List<String> discipline = new ArrayList<String>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			ano = getId.get("ano").toString();
			periodo = getId.get("periodo").toString();
			media_final = getId.get("media_final").toString();
			ano_ingresso = getId.get("ano_ingresso").toString();
		}
		
//		System.out.println("ano: " + ano);
//		System.out.println("periodo: " + periodo);
//		System.out.println("media final: " +  media_final);
		
		discipline.add(ano_ingresso);
		discipline.add(ano);
		discipline.add(periodo);
		discipline.add(media_final);
		
		return discipline;
	}
	
	public List<String> getAverageGeral(String forma_saida, String id_aluno, String nome_ativ_curricular) {
		DBObject matchAluno = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		DBObject matchDisciplina = new BasicDBObject("$match", new BasicDBObject("nome_ativ_curricular", nome_ativ_curricular));

		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
		id.put("media_final", "$media_final");
		id.put("ano", "$ano");
		id.put("periodo", "$periodo");
		id.put("ano_ingresso", "$ano_ingresso");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(matchAluno, matchDisciplina, group);
		AggregationOutput output = alunos.aggregate(pipeline);
								
		String media_final = "0.0";
		String ano = "0";
		String periodo = "";
		String ano_ingresso = "0";
		
		List<String> discipline = new ArrayList<String>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			ano = getId.get("ano").toString();
			periodo = getId.get("periodo").toString();
			if (getId.get("media_final") != null) {
				media_final = getId.get("media_final").toString();
			}
			ano_ingresso = getId.get("ano_ingresso").toString();
		}
		
//		System.out.println("ano: " + ano);
//		System.out.println("periodo: " + periodo);
//		System.out.println("media final: " +  media_final);
		
		discipline.add(ano_ingresso);
		discipline.add(ano);
		discipline.add(periodo);
		discipline.add(media_final);
		
		return discipline;
	}
	
	public void getAverageDisciplines(String forma_saida) {
		List<String> listAlunos = getAlunos(forma_saida);
		List<String> listDisciplinas = getDisciplinesDistintics(forma_saida);
		List<List<String>> listMedias = new ArrayList<List<String>>();
		
		BasicDBObject document = new BasicDBObject();
		DBCollection vectorCourses = null;
		
		System.out.println("lista de alunos");
		System.out.println(listAlunos.size());
		System.out.println("lista de disciplinas");
		System.out.println(listDisciplinas.size());
		
		try {
			vectorCourses = MongoConnection.getInstance().getDB().getCollection(
			"segundo_vector_courses_training_set"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0; i < listAlunos.size(); i++) {
			listMedias = new ArrayList<List<String>>();
			for (int j = 0; j < listDisciplinas.size(); j++) {
//				System.out.println("aluno: " + listAlunos.get(i));
				listMedias.add(getAverage(forma_saida, listAlunos.get(i), listDisciplinas.get(j)));
			}
			
//			System.out.println("id_aluno");
//			System.out.println(listAlunos.get(i));
//			System.out.println("list medias");
//			System.out.println(listMedias);
//			System.out.println(" ");
			
			document = new BasicDBObject();
			document.put("id_aluno", listAlunos.get(i));
			document.put("vector_courses", listMedias);
			
//			vectorCourses.save(document);

		}
	}
	
	public List<Double> getVectorCourses(String forma_saida, String id_aluno){
		DBCollection vectorCourses = null;

		try {
			vectorCourses = MongoConnection.getInstance().getDB().getCollection(
			"vector_courses_training_set"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DBObject match = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("vector_courses", "$vector_courses");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(match, group);
		AggregationOutput output = vectorCourses.aggregate(pipeline);
										
		List<Double> result = new ArrayList<Double>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String temp = getId.get("vector_courses").toString();
			temp = temp.replace("[", "").replace("]", "").replace(" ", "");
			String [] notas = temp.split(",");
			for (int i = 0; i < notas.length; i++) {
				result.add(Double.parseDouble(notas[i]));
			}
		}
		
		return result;
	}
	
	public List<List<String>> getDisciplines(String forma_saida, String id_aluno) {
		List<String> listDisciplinas = getDisciplinesDistintics(forma_saida);
//		System.out.println("lista de disciplinas");
//		System.out.println(listDisciplinas.size());
		List<List<String>> listMedias = new ArrayList<List<String>>();

		for (int j = 0; j < listDisciplinas.size(); j++) {
//			System.out.println("aluno: " + listAlunos.get(i));
			listMedias.add(getAverage(forma_saida, id_aluno, listDisciplinas.get(j)));
		}

//		System.out.println("id_aluno");
//		System.out.println(id_aluno);
//		System.out.println("list medias");
//		System.out.println(listMedias);
//		System.out.println(" ");
		
		return listMedias;
	}
	
	public List<List<String>> getDisciplinesGeral(String forma_saida, String id_aluno) {
		List<String> listDisciplinas = getDisciplinesDistintics(forma_saida);
		List<List<String>> listMedias = new ArrayList<List<String>>();

		for (int j = 0; j < listDisciplinas.size(); j++) {
//			System.out.println("aluno: " + listAlunos.get(i));
			listMedias.add(getAverageGeral(forma_saida, id_aluno, listDisciplinas.get(j)));
		}

//		System.out.println("id_aluno");
//		System.out.println(id_aluno);
//		System.out.println("list medias");
//		System.out.println(listMedias);
//		System.out.println(" ");
		
		return listMedias;
	}
	
	public String getRepresentante(String forma_saida) {
		List<String> listAlunos = getAlunos(forma_saida);
		
		System.out.println("alunos: " + listAlunos.size());
		
		List<List<String>> list = new ArrayList<List<String>>();

		double menorDistancia = 2;
		String id_aluno_menor_distancia = "";
		
//		System.out.println("list de alunos");
//		System.out.println(listAlunos.size());
		
//		System.out.println("teste:");
//		List<List<String>> teste = getDisciplines(forma_saida, "f84525ce69a13442a3cc024af8f0afd0");
//		System.out.println(teste);
		
		for (int i = 0; i < listAlunos.size(); i++) {
			double total_media = 0.0;
			List<String> listInside = new ArrayList<String>();
			List<List<String>> vectorDisciplinesAlunoA = getDisciplines(forma_saida, listAlunos.get(i));
			for (int j = 0; j < listAlunos.size(); j++) {
				List<List<String>> vectorDisciplinesAlunoB = getDisciplines(forma_saida, listAlunos.get(j));
				
				double similaridade = 0.0;
				if (!listAlunos.get(i).toString().equals(listAlunos.get(j).toString())) {
					for (int k = 0; k < vectorDisciplinesAlunoA.size(); k++) {
						double nota_final = 0.0;
						int ano_ingressoA = Integer.parseInt(vectorDisciplinesAlunoA.get(k).get(0));
						int anoA = Integer.parseInt(vectorDisciplinesAlunoA.get(k).get(1));
						String periodoA = vectorDisciplinesAlunoA.get(k).get(2);
						double notaA = Double.parseDouble(vectorDisciplinesAlunoA.get(k).get(3));
						int ano_periodoA = anoA - ano_ingressoA;
						
						int ano_ingressoB = Integer.parseInt(vectorDisciplinesAlunoB.get(k).get(0));
						int anoB = Integer.parseInt(vectorDisciplinesAlunoB.get(k).get(1));
						String periodoB = vectorDisciplinesAlunoB.get(k).get(2);
						double notaB = Double.parseDouble(vectorDisciplinesAlunoB.get(k).get(3));
						int ano_periodoB = anoB - ano_ingressoB;
						
						if (ano_periodoA == ano_periodoB && periodoA.equals(periodoB) && !periodoA.equals("") && !periodoB.equals("")) {
							nota_final = (Math.abs(notaA - notaB))/10;
						}

						similaridade = similaridade + nota_final;
					}
				}
				similaridade = similaridade/vectorDisciplinesAlunoA.size();
				total_media = total_media + (1 - similaridade);
			}
			total_media = total_media / listAlunos.size();
			listInside.add(listAlunos.get(i).toString());
			listInside.add(String.valueOf(total_media));
			
			if (total_media < menorDistancia && total_media > 0.0) {
				menorDistancia = total_media;
				id_aluno_menor_distancia = listAlunos.get(i).toString();
			}
			list.add(listInside);
		}
		id_aluno_representante = id_aluno_menor_distancia;
		System.out.println("aluno representante");
		System.out.println(id_aluno_representante);
		System.out.println("menor distancia");
		System.out.println(menorDistancia);
		System.out.println("list");
		System.out.println(list);
		System.out.println(" ");
		return id_aluno_representante;
	}
	
	public List<String> getAlunosGeral(String forma_saida){
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(group);
		AggregationOutput output = alunos.aggregate(pipeline);
								
		List<String> list = new ArrayList<String>();
		
		List<String> listAlunos = getAlunos(forma_saida);
//		System.out.println("list Alunosss");
//		System.out.println(listAlunos.size());
		int cont = 1;
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " + dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String id_aluno = getId.get("id_aluno").toString();
			if (listAlunos.contains(id_aluno) == false) {
				list.add(id_aluno);
			}
			cont++;
		}
//		System.out.println("cont");
//		System.out.println(cont);
		return list;
	}
	
	public double getAverageGeralOld(String id_aluno, String cod_ativ_curricular) {
		DBObject matchAluno = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		DBObject matchDisciplina = new BasicDBObject("$match", new BasicDBObject("cod_ativ_curricular", cod_ativ_curricular));

		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("cod_ativ_curricular", "$cod_ativ_curricular");
		id.put("media_final", "$media_final");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(matchAluno, matchDisciplina, group);
		AggregationOutput output = alunos.aggregate(pipeline);
								
		double media_final = 0.0;
		
		List<Double> notas = new ArrayList<Double>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			if (getId.get("media_final") != null) {
				media_final = Double.parseDouble(getId.get("media_final").toString());
			}
			notas.add(media_final);
		}
		
		if (notas.size() > 0) {
			media_final = calculaMediaFinal(notas);
		}
		return media_final;
	}
	
	public void getAverageDisciplinesGeral(String forma_saida) {
		List<String> listAlunos = getAlunosGeral(forma_saida);
		List<String> listDisciplinas = getDisciplinesDistintics(forma_saida);
//		System.out.println("lista de alunos geral");
//		System.out.println(listAlunos.size());
//		System.out.println("discplinas");
//		System.out.println(listDisciplinas.size());
		List<Double> listMedias = new ArrayList<Double>();
		
		BasicDBObject document = new BasicDBObject();
		DBCollection vectorCourses = null;
		
		try {
			vectorCourses = MongoConnection.getInstance().getDB().getCollection(
			"vector_courses_geral"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0; i < listAlunos.size(); i++) {
			listMedias = new ArrayList<Double>();
			for (int j = 0; j < listDisciplinas.size(); j++) {
				listMedias.add(getAverageGeralOld(listAlunos.get(i), listDisciplinas.get(j)));
			}
			
//			System.out.println("id_aluno");
//			System.out.println(listAlunos.get(i));
//			System.out.println("list medias");
//			System.out.println(listMedias);
//			System.out.println(" ");
			System.out.println(i);
			
			document = new BasicDBObject();
			document.put("id_aluno", listAlunos.get(i));
			document.put("vector_courses", listMedias);
			
//			vectorCourses.save(document);

		}
	}
	
	public List<Double> getVectorCoursesGeral(String forma_saida, String id_aluno){
		DBCollection vectorCourses = null;

		try {
			vectorCourses = MongoConnection.getInstance().getDB().getCollection(
			"vector_courses_geral"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DBObject match = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("vector_courses", "$vector_courses");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(match, group);
		AggregationOutput output = vectorCourses.aggregate(pipeline);
										
		List<Double> result = new ArrayList<Double>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String temp = getId.get("vector_courses").toString();
			temp = temp.replace("[", "").replace("]", "").replace(" ", "");
			String [] notas = temp.split(",");
			for (int i = 0; i < notas.length; i++) {
				result.add(Double.parseDouble(notas[i]));
			}
		}
		
		return result;
	}
	
	
	public String getDetailsAlunoGeral(String id_aluno, String forma_saida){
		DBObject match = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("nome_curso", "$nome_curso");
		id.put("cod_curso", "$cod_curso");
		id.put("versao_curso", "$versao_curso");
		id.put("cod_ativ_curricular", "$cod_ativ_curricular");
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
		id.put("media_final", "$media_final");
		id.put("descricao_situacao", "$descricao_situacao");
		id.put("ano", "$ano");
		id.put("periodo", "$periodo");
		id.put("creditos", "$creditos");
		id.put("carga_horaria_teorica", "$carga_horaria_teorica");
		id.put("carga_horaria_pratica", "$carga_horaria_pratica");
		id.put("forma_ingresso", "$forma_ingresso");
		id.put("ano_ingresso", "$ano_ingresso");
		id.put("forma_saida", "$forma_saida");
		id.put("ano_saida", "$ano_saida");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
		
		DBObject sortFields = new BasicDBObject();
		sortFields.put("_id.ano", 1);
		DBObject sort = new BasicDBObject("$sort", sortFields);
					
		List<DBObject> pipeline = Arrays.asList(match, group, sort);
		AggregationOutput output = alunos.aggregate(pipeline);
										
		String info = "";
		
		List<String> listDisciplinas = getDisciplinesDistintics(forma_saida);
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String cod_ativ_curricular = getId.get("cod_ativ_curricular").toString();
//			if (listDisciplinas.contains(cod_ativ_curricular)) {
				info = info + dbo + "\n\n";
//			}
		}
		
		return info;
	}
	
	public double getDistanciaMax(String forma_saida, String id_representante) {
		List<List<String>> listDistance = new ArrayList<List<String>>();
			List<String> listAlunos = getAlunos(forma_saida);
			
			double menorDistancia = 2;
			String id_aluno_menor_distancia = "";
			
			for (int i = 0; i < listAlunos.size(); i++) {
					List<String> listInfo = new ArrayList<String>();
					List<Double> vectorCoursesAlunoRepresentante = getVectorCourses(forma_saida, id_representante);
					List<Double> vectorCoursesAluno = getVectorCourses(forma_saida, listAlunos.get(i));
					
					double media = 0.0;
					for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
						double notaA = vectorCoursesAlunoRepresentante.get(k);
						double notaB = vectorCoursesAluno.get(k);
						double nota_final = 1 - ((Math.abs(notaA - notaB))/10);
						media = media + nota_final;
					}
					media = media/vectorCoursesAlunoRepresentante.size();
						listInfo.add(listAlunos.get(i).toString());
						listInfo.add(String.valueOf(media));
						
						listDistance.add(listInfo);
						
						if (media < menorDistancia) {
							menorDistancia = media;
							id_aluno_menor_distancia = listAlunos.get(i).toString();
						}
			}
		System.out.println(" ");
		System.out.println("aluno");
		System.out.println(id_aluno_menor_distancia);
		System.out.println("dist max");
		System.out.println(menorDistancia);
		return menorDistancia;
	}
	
	public double getDistanciaMediana(String forma_saida, String id_representante) {
		List<List<String>> listDistance = new ArrayList<List<String>>();
		List<String> listAlunos = getAlunos(forma_saida);
		List<Double> listAllDistance = new ArrayList<Double>();

			
			double medianaDistancia;
			
			for (int i = 0; i < listAlunos.size(); i++) {
					List<String> listInfo = new ArrayList<String>();
					List<List<String>> vectorCoursesAlunoRepresentante = getDisciplines(forma_saida, id_representante);
					List<List<String>> vectorCoursesAluno = getDisciplines(forma_saida, listAlunos.get(i));

					double similaridade = 0.0;
					if (!id_representante.equals(listAlunos.get(i).toString())) {
						for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
							double nota_final = 0.0;
							int ano_ingressoA = Integer.parseInt(vectorCoursesAlunoRepresentante.get(k).get(0));
							int anoA = Integer.parseInt(vectorCoursesAlunoRepresentante.get(k).get(1));
							String periodoA = vectorCoursesAlunoRepresentante.get(k).get(2);
							double notaA = Double.parseDouble(vectorCoursesAlunoRepresentante.get(k).get(3));
							int ano_periodoA = anoA - ano_ingressoA;
							
							int ano_ingressoB = Integer.parseInt(vectorCoursesAluno.get(k).get(0));
							int anoB = Integer.parseInt(vectorCoursesAluno.get(k).get(1));
							String periodoB = vectorCoursesAluno.get(k).get(2);
							double notaB = Double.parseDouble(vectorCoursesAluno.get(k).get(3));
							int ano_periodoB = anoB - ano_ingressoB;
							
							if (ano_periodoA == ano_periodoB && periodoA.equals(periodoB) && !periodoA.equals("") && !periodoB.equals("")) {
								nota_final =  (Math.abs(notaA - notaB))/10;
							}

							similaridade = similaridade + nota_final;
						}
					}
					similaridade = similaridade/vectorCoursesAlunoRepresentante.size();
					double distance = 1 - similaridade;
					listAllDistance.add(distance);
					listInfo.add(listAlunos.get(i).toString());
					listInfo.add(String.valueOf(distance));
					listDistance.add(listInfo);
			}
		
		Collections.sort(listAllDistance);
		System.out.println("lista de distancias");
		System.out.println(listAllDistance);
		
		medianaDistancia = listAllDistance.get(listAllDistance.size()/2);
//		medianaDistancia = listAllDistance.get(listAllDistance.size()-1);
		
		System.out.println(" ");
		System.out.println("distancia mediana ");
		System.out.println(medianaDistancia);
		
		return medianaDistancia;
	}
	
	public List<List<String>> getResultadosExperimentosDistanceDoPerfil(String forma_saida, String id_representante, double distMax) {
		List<List<String>> listDistancesAprovados = new ArrayList<List<String>>();
		List<List<String>> listDistancesReprovados = new ArrayList<List<String>>();
		List<String> listAlunos = list_excluidos_perfil;
		
		List <Double> listaDistanciasGeral = new ArrayList<Double>();
					
		for (int i = 0; i < listAlunos.size(); i++) {
				List<String> listInfo = new ArrayList<String>();
				List<List<String>> vectorCoursesAlunoRepresentante = getDisciplines(forma_saida, id_representante);
				List<List<String>> vectorCoursesAluno = getDisciplinesGeral(forma_saida, listAlunos.get(i));

				double similaridade = 0.0;
				if (!id_representante.equals(listAlunos.get(i).toString())) {
					for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
						double nota_final = 0.0;
						int ano_ingressoA = Integer.parseInt(vectorCoursesAlunoRepresentante.get(k).get(0));
						int anoA = Integer.parseInt(vectorCoursesAlunoRepresentante.get(k).get(1));
						String periodoA = vectorCoursesAlunoRepresentante.get(k).get(2);
						double notaA = Double.parseDouble(vectorCoursesAlunoRepresentante.get(k).get(3));
						int ano_periodoA = anoA - ano_ingressoA;
						
						int ano_ingressoB = Integer.parseInt(vectorCoursesAluno.get(k).get(0));
						int anoB = Integer.parseInt(vectorCoursesAluno.get(k).get(1));
						String periodoB = vectorCoursesAluno.get(k).get(2);
						double notaB = Double.parseDouble(vectorCoursesAluno.get(k).get(3));
						int ano_periodoB = anoB - ano_ingressoB;
						
						if (ano_periodoA == ano_periodoB && periodoA.equals(periodoB) && !periodoA.equals("") && !periodoB.equals("")) {
							nota_final =  (Math.abs(notaA - notaB))/10;
						}

						similaridade = similaridade + nota_final;
					}
				}
				similaridade = similaridade/vectorCoursesAlunoRepresentante.size();
				double distance = 1 - similaridade;
				listInfo.add(listAlunos.get(i).toString());
				listInfo.add(String.valueOf(distance));
				
				listaDistanciasGeral.add(distance);
				
				if (distance < distMax) {
					listDistancesAprovados.add(listInfo);
				} else {
					listDistancesReprovados.add(listInfo);
				}
		}
//			for (int i = 0; i < listAlunos.size(); i++) {
//					List<String> listInfo = new ArrayList<String>();
//					List<Double> vectorCoursesAlunoRepresentante = getVectorCourses(forma_saida, id_representante);
//					List<Double> vectorCoursesAluno = getVectorCoursesGeral(forma_saida, listAlunos.get(i));
//					
//					double media = 0.0;
//					for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
//						double notaA = vectorCoursesAlunoRepresentante.get(k);
//						double notaB = vectorCoursesAluno.get(k);
//						double nota_final = 1 - ((Math.abs(notaA - notaB))/10);
//						media = media + nota_final;
//					}
//					media = media/vectorCoursesAlunoRepresentante.size();
//					listInfo.add(listAlunos.get(i).toString());
//					listInfo.add(String.valueOf(media));
//						if (media >= distMax) {
//							listDistancesAprovados.add(listInfo);
//						} else {
//							listDistancesReprovados.add(listInfo);
//						}
//			}
		
		Collections.sort(listaDistanciasGeral);
		System.out.println("tamanho da lista dos alunos excluídos do conjunto e classificados como pertences ao perfil");
		System.out.println(listDistancesAprovados.size());
		System.out.println("tamanho da lista dos alunos excluídos do conjunto e classificados como não pertences ao perfil");
		System.out.println(listDistancesReprovados.size());
		System.out.println(" ");
		System.out.println("lista geral");
		System.out.println(listaDistanciasGeral);
		return listDistancesAprovados;
	}
	
	public List<List<String>> getResultadosExperimentosDistanceGeral(String forma_saida, String id_representante, double distMax) {
		List<List<String>> listDistancesAprovados = new ArrayList<List<String>>();
		List<List<String>> listDistancesReprovados = new ArrayList<List<String>>();
		List<String> listAlunos = getAlunosGeral(forma_saida);
		
		List <Double> listaDistanciasGeral = new ArrayList<Double>();
		
		for (int i = 0; i < listAlunos.size(); i++) {
			List<String> listInfo = new ArrayList<String>();
			List<List<String>> vectorCoursesAlunoRepresentante = getDisciplines(forma_saida, id_representante);
			List<List<String>> vectorCoursesAluno = getDisciplinesGeral(forma_saida, listAlunos.get(i));

			double similaridade = 0.0;
			if (!id_representante.equals(listAlunos.get(i).toString())) {
				for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
					double nota_final = 0.0;
					int ano_ingressoA = Integer.parseInt(vectorCoursesAlunoRepresentante.get(k).get(0));
					int anoA = Integer.parseInt(vectorCoursesAlunoRepresentante.get(k).get(1));
					String periodoA = vectorCoursesAlunoRepresentante.get(k).get(2);
					double notaA = Double.parseDouble(vectorCoursesAlunoRepresentante.get(k).get(3));
					int ano_periodoA = anoA - ano_ingressoA;
					
					int ano_ingressoB = Integer.parseInt(vectorCoursesAluno.get(k).get(0));
					int anoB = Integer.parseInt(vectorCoursesAluno.get(k).get(1));
					String periodoB = vectorCoursesAluno.get(k).get(2);
					double notaB = Double.parseDouble(vectorCoursesAluno.get(k).get(3));
					int ano_periodoB = anoB - ano_ingressoB;
					
					if (ano_periodoA == ano_periodoB && periodoA.equals(periodoB) && !periodoA.equals("") && !periodoB.equals("")) {
						nota_final =  (Math.abs(notaA - notaB))/10;
					}

					similaridade = similaridade + nota_final;
				}
			}
			similaridade = similaridade/vectorCoursesAlunoRepresentante.size();
			double distance = 1 - similaridade;
			listInfo.add(listAlunos.get(i).toString());
			listInfo.add(String.valueOf(distance));
			
			listaDistanciasGeral.add(distance);
			
			if (distance < distMax) {
				listDistancesAprovados.add(listInfo);
			} else {
				listDistancesReprovados.add(listInfo);
			}
	}
		System.out.println("tamanho da lista dos alunos em geral classificados como pertences ao perfil");
		System.out.println(listDistancesAprovados.size());
		System.out.println("lista de info");
		System.out.println(listDistancesAprovados);
		System.out.println(" ");
		System.out.println("tamanho da lista dos alunos em geral classificados como não pertences ao perfil");
		System.out.println(listDistancesReprovados.size());
		System.out.println(" ");
		System.out.println("lista geral");
		System.out.println(listaDistanciasGeral);
		return listDistancesAprovados;
	}
	
	public static void main(String args[]) {
		AnaliseDeEvasao ativ= new AnaliseDeEvasao("ciencia da computacao");
		
		// FORMA O CONJUNTO DE ALUNOS QUE ESTARÃO NO CONJUNTO DO PERFIL SELECIONADO DE ACORDO COM A PORCENTAGEM REQUERIDA
		ativ.filterByEvasao(1);
		
		// CALCULA A MEDIA DE NOTAS DOS ALUNOS DO CONJUNTO EM CADA DISCIPLINA DO VETOR DE DISCPLINAS QUE FORAM 
		// SELECIONADAS DENTRO DO CONJUNTO DE DISCIPLINAS FEITAS PELOS ALUNOS DENTRO DO CONJUNTO
//		ativ.getAverageDisciplines("formado");
		
		// ENCONTRA O REPRESENTANTE DO CONJUNTO FORMADO DENTRO DAQUELE TIPO DE EVASAO REQUERIDO, ATRAVÉS
		// DE UM CALCULO DE MÉDIA DAS DISTÂNCIAS DE CADA ALUNO DENTRO DO CONJUNTO PARA O RESTANTE DOS ALUNOS DO CONJUNTO
		String representante = ativ.getRepresentante("evasao");
		
		// CALCULA A MEDIA DE NOTAS DOS ALUNOS ("RESTO DO MUNDO") EM CADA DISCIPLINA DO VETOR DE DISCPLINAS QUE FORAM 
		// SELECIONADAS DENTRO DO CONJUNTO DE DISCIPLINAS FEITAS PELOS ALUNOS DENTRO DO CONJUNTO
//		ativ.getAverageDisciplinesGeral("formado");
		
		// CALCULA A DISTANCIA MAXIMA DENTRO DO CONJUNTO, ATRAVÉS DE UM CALCULO PARA DESCOBRIR QUAL O VALOR DA DISTANCIA
		// DO ALUNO MAIS DISTANTE DO REPRESENTANTE DE DENTRO DO CONJUNTO
//		double distMax = ativ.getDistanciaMax("formado", representante);
		double distMax = ativ.getDistanciaMediana("evasao", representante);
		
		// VERIFICA QUANTIDADE DE ALUNOS DE DENTRO DO PERFIL PORÉM QUE NÃO ENTRARAM NO CONJUNTO E QUE FORAM CLASSIFICADOS
		// PELO ALGORITMO COMO PERTENCENTES AO PERFIL 
//		ativ.getResultadosExperimentosDistanceDoPerfil("evasao", representante, distMax);
		
		// VERIFICA QUANTIDADE DE ALUNOS FORA DO PERFIL (RESTO DO MUNDO) QUE FORAM CLASSIFICADOS
		// PELO ALGORITMO COMO PERTENCENTES AO PERFIL
		ativ.getResultadosExperimentosDistanceGeral("evasao", representante, distMax);
		
		System.out.println("disciplinas");
		System.out.println(ativ.getDisciplinesDistintics("evasao"));
	}
}
