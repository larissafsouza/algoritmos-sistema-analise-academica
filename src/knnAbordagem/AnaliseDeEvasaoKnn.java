package knnAbordagem;

import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream.GetField;
import java.net.UnknownHostException;
import java.security.AllPermission;
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

public class AnaliseDeEvasaoKnn {
	private DBCollection alunos;
	private String current_curso;
	private String id_aluno_representante;
	private List<String> list_excluidos_perfil;
	
	public AnaliseDeEvasaoKnn(String curso){
		current_curso = curso;
			alunos = null;
				try {
					alunos = MongoConnection.getInstance().getDB().getCollection("atividades_academica_"+curso.toLowerCase().trim().replace(" ", "_"));
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	}
	
	public List<String> filterByEvasao(String forma_saida, double porcentagemConjunto){
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
			"training_set"+forma_saida.toLowerCase()+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
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
		
//		list_excluidos_perfil = listaAlunosForaConjunto;
//		
//		System.out.println("LISTA DE EXCLUIDOS DO PERFIL");
//		System.out.println(listaAlunosForaConjunto);
//		System.out.println("LISTA DE ALUNOS DO CONJUNTO");
//		System.out.println(listaAlunosConjunto);
//		
		return listaAlunosConjunto;
	}
	
	public boolean verificarPeriodos(String id_aluno) {
		DBObject match = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));

		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("ano", "$ano");
		id.put("periodo", "$periodo");
		id.put("ano_ingresso", "$ano_ingresso");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
		
		DBObject id2 = new BasicDBObject();
		id2.put("id_aluno", "$_id.id_aluno");
		
		DBObject groupFields2 = new BasicDBObject();
		groupFields2.put("_id", id2);
		groupFields2.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group2 = new BasicDBObject("$group", groupFields2);
		
		DBObject sortFields = new BasicDBObject();
		sortFields.put("_id", 1);
		DBObject sort = new BasicDBObject("$sort", sortFields);
					
		List<DBObject> pipeline = Arrays.asList(match, group, group2);
		AggregationOutput output = alunos.aggregate(pipeline);
								
		List<List<String>> list = new ArrayList<List<String>>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " + dbo);
			int quantidade = Integer.parseInt(dbo.get("quantidade").toString());
			if (quantidade >= 8) {
				return true;
			}
		}
		return false;
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
	
	public void setDisciplinesRestrito(String forma_saida){
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
		id.put("ano", "$ano");
		id.put("periodo", "$periodo");
		id.put("ano_ingresso", "$ano_ingresso");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
		
		DBObject id2 = new BasicDBObject();
		id2.put("nome_ativ_curricular", "$_id.nome_ativ_curricular");
		id2.put("ano", "$_id.ano");
		id2.put("periodo", "$_id.periodo");
		id2.put("ano_ingresso", "$_id.ano_ingresso");
//		id2.put("nome_ativ_curricular", "$_id.nome_ativ_curricular");

		
		DBObject groupFields2 = new BasicDBObject();
		groupFields2.put("_id", id2);
		groupFields2.put("quantidade", new BasicDBObject("$sum", 1));
		
		DBObject group2 = new BasicDBObject("$group", groupFields2);
		
		DBObject sortFields = new BasicDBObject();
		sortFields.put("_id.nome_ativ_curricular", 1);
		DBObject sort = new BasicDBObject("$sort", sortFields);
		
		DBCollection trainingSet = null;
		
		try {
			trainingSet = MongoConnection.getInstance().getDB().getCollection(
			"training_set"+forma_saida.toLowerCase().trim().replace(" ", "").replace("(crit.01)", "").replace("(crit.02)", "02")+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
					
		List<DBObject> pipeline = Arrays.asList(group, sort);
		AggregationOutput output = trainingSet.aggregate(pipeline);
										
		List<String> listDisc = new ArrayList<String>();
		
		
		List<List<String>> list_sub_periodos = new ArrayList<List<String>>();
		
		BasicDBObject document = new BasicDBObject();
		DBCollection disciplinesRestrito = null;
		
		try {
			disciplinesRestrito = MongoConnection.getInstance().getDB().getCollection(
			"disciplinas_restrito_"+forma_saida+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (DBObject dbo : output.results()) {
			System.out.println("dbo: " +dbo);
			document = new BasicDBObject();
			List<String> listInfo = new ArrayList<String>();
			
			DBObject getId = (DBObject) dbo.get("_id");
			String cod_ativ_curricular = getId.get("nome_ativ_curricular").toString();
			int ano = Integer.parseInt(getId.get("ano").toString());
			String periodo = getId.get("periodo").toString();
			int ano_ingresso = Integer.parseInt(getId.get("ano_ingresso").toString());
			
			String sub_periodo =  (ano - ano_ingresso) + "/" + periodo;
			
			listInfo.add(cod_ativ_curricular);
			listInfo.add(sub_periodo);
			
			list_sub_periodos.add(listInfo);
			document.put("nome_ativ_curricular", cod_ativ_curricular);
			document.put("periodo_padrao", sub_periodo);
			
//			disciplinesRestrito.save(document);	
		}
	}
	
	public List<String> getDisciplinesRestrito(String forma_saida){
		DBObject id = new BasicDBObject();
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
		id.put("periodo_padrao", "$periodo_padrao");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
		
		DBObject sortFields = new BasicDBObject();
		sortFields.put("_id.nome_ativ_curricular", 1);
		DBObject sort = new BasicDBObject("$sort", sortFields);
		
		DBCollection disciplinesRestrito = null;
		
		try {
			disciplinesRestrito = MongoConnection.getInstance().getDB().getCollection(
			"disciplinas_restrito_"+forma_saida+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
					
		List<DBObject> pipeline = Arrays.asList(group, sort);
		AggregationOutput output = disciplinesRestrito.aggregate(pipeline);
		
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
		return listDisc;
	}
	
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
//			System.out.println("quantidade:" + getAlunos(forma_saida).size()/2);
			
			if (quantidade >= size) {
				String cod_ativ_curricular = getId.get("nome_ativ_curricular").toString();
				listDisc.add(cod_ativ_curricular);
			}
		}
		
		return listDisc;
	}
	
	public List<String> getDisciplinesDistinticsToGroupKnn(String forma_saida, String abordagem_disciplinas, int k){
		DBCollection knn_info_group = null;
		
		try {
			knn_info_group = MongoConnection.getInstance().getDB().getCollection(
			"k"+k+"_info_group_"+abordagem_disciplinas+"_"+forma_saida+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
//		id2.put("id_aluno", "$id_aluno");
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
		AggregationOutput output = knn_info_group.aggregate(pipeline);
										
		List<String> listDisc = new ArrayList<String>();
		
		int size = (int)((double)(k+1) * 0.5);
//		System.out.println("size: " + size);
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");

			int quantidade = Integer.parseInt(dbo.get("quantidade").toString());
//			System.out.println("quantidade:" + getAlunos(forma_saida).size()/2);
			
			if (quantidade >= size) {
				String cod_ativ_curricular = getId.get("nome_ativ_curricular").toString();
				listDisc.add(cod_ativ_curricular);
			}
		}
		
		return listDisc;
	}
	
	public List<String> getAlunosFromGroupKnn(String forma_saida, String abordagem_disciplinas, int knn){
		BasicDBObject document = new BasicDBObject();
		DBCollection knn_info_group = null;
		
		try {
			knn_info_group = MongoConnection.getInstance().getDB().getCollection(
			"k"+knn+"_info_group_"+abordagem_disciplinas+"_"+forma_saida+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
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
		AggregationOutput output = knn_info_group.aggregate(pipeline);
								
		List<String> list = new ArrayList<String>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: "+ dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String id_aluno = getId.get("id_aluno").toString();
			if (!id_aluno.equals("representante"+knn)) {
				list.add(id_aluno);
			}
		}
		return list;
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
		
		discipline.add(ano_ingresso);
		discipline.add(ano);
		discipline.add(periodo);
		discipline.add(media_final);
		
		return discipline;
	}
	
	public List<List<String>> getAverageDisciplinesFromGroupKnn(String forma_saida, String nome_ativ_curricular, 
			String abordagem_disciplinas,int k) {
		DBCollection knn_info_group = null;
		
		try {
			knn_info_group = MongoConnection.getInstance().getDB().getCollection(
			"k"+k+"_info_group_"+abordagem_disciplinas+"_"+forma_saida+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DBObject matchDisciplina = new BasicDBObject("$match", new BasicDBObject("nome_ativ_curricular", nome_ativ_curricular));

		DBObject id = new BasicDBObject();
//		id.put("id_aluno", "$id_aluno");
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
//		id.put("ano", "$ano");
//		id.put("periodo", "$periodo");
//		id.put("ano_ingresso", "$ano_ingresso");
		
		DBObject id2 = new BasicDBObject();
		id2.put("media_final", "$media_final");
				
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("media das notas", new BasicDBObject("$avg", id2.get("media_final")));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(matchDisciplina, group);
		AggregationOutput output = knn_info_group.aggregate(pipeline);
								
		
		List<String> listInfo = new ArrayList<String>();
		
		List<List<String>> listGeral = new ArrayList<List<String>>();
 		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String ativ_curricular = getId.get("nome_ativ_curricular").toString();
			double media = Double.parseDouble(dbo.get("media das notas").toString());
			
			listInfo.add(ativ_curricular);
			listInfo.add(String.valueOf(media));
//			
			
			DBObject idInside = new BasicDBObject();
			idInside.put("id_aluno", "$id_aluno");
			idInside.put("nome_ativ_curricular", "$nome_ativ_curricular");
			idInside.put("ano", "$ano");
			idInside.put("periodo", "$periodo");
			idInside.put("ano_ingresso", "$ano_ingresso");
			
			DBObject id2Inside = new BasicDBObject();
			id2Inside.put("periodos", "$periodos");
			id2Inside.put("periodo", "$periodo");
			
			DBObject projectFields = new BasicDBObject();
			projectFields.put("_id", idInside);
			projectFields.put("periodos", new BasicDBObject("$subtract", new Object[] {"$ano", "$ano_ingresso" }));
			projectFields.put("periodo", "$periodo");
			DBObject project = new BasicDBObject("$project", projectFields);
			
					
			DBObject groupFieldsInside = new BasicDBObject();
			groupFieldsInside.put("_id", id2Inside);
			groupFieldsInside.put("quantidade", new BasicDBObject("$sum", 1));
			DBObject groupInside = new BasicDBObject("$group", groupFieldsInside);
			
			DBObject sortFields = new BasicDBObject();
			sortFields.put("quantidade", 1);
			DBObject sort = new BasicDBObject("$sort", sortFields);
						
			List<DBObject> pipelineInside = Arrays.asList(matchDisciplina, project, groupInside, sort);
			AggregationOutput outputInside = knn_info_group.aggregate(pipelineInside);
									
			String periodosInside = "";
			String periodoInside = "";
			
			for (DBObject dboInside : outputInside.results()) {
				DBObject getIdInside = (DBObject) dboInside.get("_id");
				periodosInside = getIdInside.get("periodos").toString();
				periodoInside = getIdInside.get("periodo").toString();				
			}
			
			listInfo.add(periodosInside);
			listInfo.add(periodoInside);
	
			listGeral.add(listInfo);
		}
		
		return listGeral;
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
		
		discipline.add(ano_ingresso);
		discipline.add(ano);
		discipline.add(periodo);
		discipline.add(media_final);
		
		return discipline;
	}
	
	public List<String> getAverageDisciplineFromGroupKnn(String forma_saida, String id_aluno, String nome_ativ_curricular, 
			String abordagem_disciplinas,int k) {
		DBCollection knn_info_group = null;
		
		try {
			knn_info_group = MongoConnection.getInstance().getDB().getCollection(
			"k"+k+"_info_group_"+abordagem_disciplinas+"_"+forma_saida+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DBObject matchAluno = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));
		DBObject matchDisciplina = new BasicDBObject("$match", new BasicDBObject("nome_ativ_curricular", nome_ativ_curricular));

		DBObject id = new BasicDBObject();
		if (!id_aluno.equals("representante"+k)) {
			id.put("id_aluno", "$id_aluno");
			id.put("nome_ativ_curricular", "$nome_ativ_curricular");
			id.put("media_final", "$media_final");
			id.put("ano", "$ano");
			id.put("periodo", "$periodo");
			id.put("ano_ingresso", "$ano_ingresso");
		} else {
			id.put("id_aluno", "$id_aluno");
			id.put("nome_ativ_curricular", "$nome_ativ_curricular");
			id.put("media_final", "$media_final");
			id.put("periodos", "$periodos");
			id.put("periodo", "$periodo");
		}
		
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(matchAluno, matchDisciplina, group);
		AggregationOutput output = knn_info_group.aggregate(pipeline);
			
		List<String> listInfo = new ArrayList<String>();
		 		
		String media_final = "0.0";
		String ano = "0";
		String periodo = "";
		String ano_ingresso = "0";
		
		String periodos = "";
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			periodo = getId.get("periodo").toString();
			if (id_aluno.equals("representante"+k)) {
				media_final = getId.get("media_final").toString();
				periodos = getId.get("periodos").toString();
			} else {
				media_final = getId.get("media_final").toString();
				ano = getId.get("ano").toString();
				ano_ingresso = getId.get("ano_ingresso").toString();
			}
			
			
		}
		
		listInfo.add(media_final);
		
		if (id_aluno.equals("representante"+k)) {
			listInfo.add(periodos);
		} else {
			listInfo.add(ano);
			listInfo.add(ano_ingresso);
		}
		
		listInfo.add(periodo);
		
		
		return listInfo;
	}
	
	public List<List<String>> getDisciplinesFromGroupKnn(String forma_saida, String id_aluno, String abordagem_disciplinas, int knn) {
		List<String> listDisciplinas = getDisciplinesDistinticsToGroupKnn(forma_saida, abordagem_disciplinas, knn);
		List<List<String>> listMedias = new ArrayList<List<String>>();

		for (int j = 0; j < listDisciplinas.size(); j++) {
			listMedias.add(getAverageDisciplineFromGroupKnn(forma_saida, id_aluno, listDisciplinas.get(j), abordagem_disciplinas, knn));
		}
		return listMedias;
	}
	
	public List<List<String>> getDisciplines(String forma_saida, String id_aluno, String abordagem_disciplinas) {
		List<String> listDisciplinas = new ArrayList<String>();
		
		if (abordagem_disciplinas.equals("flex")) {
			listDisciplinas = getDisciplinesDistintics(forma_saida);
//			System.out.println("qnt flex: " + listDisciplinas.size());
		} else {
			listDisciplinas = getDisciplinesRestrito (forma_saida);
//			System.out.println("qnt restrito: " + listDisciplinas.size());
		}
		List<List<String>> listMedias = new ArrayList<List<String>>();

		for (int j = 0; j < listDisciplinas.size(); j++) {
			listMedias.add(getAverage(forma_saida, id_aluno, listDisciplinas.get(j)));
		}
		return listMedias;
	}
	
	
	public List<List<String>> getDisciplinesGeral(String forma_saida, String id_aluno, String abordagem_disciplinas, int knn) {
		List<String> listDisciplinas = getDisciplinesDistinticsToGroupKnn(forma_saida, abordagem_disciplinas, knn);
		
		List<List<String>> listMedias = new ArrayList<List<String>>();

		for (int j = 0; j < listDisciplinas.size(); j++) {
			listMedias.add(getAverageGeral(forma_saida, id_aluno, listDisciplinas.get(j)));
		}
		
		return listMedias;
	}
	
	public List<String> getAlunoReguaGroup(String forma_saida, String id_aluno) {
		DBObject matchAluno = new BasicDBObject("$match", new BasicDBObject("id_aluno", id_aluno));

		DBObject id = new BasicDBObject();
		id.put("nome_ativ_curricular", "$nome_ativ_curricular");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
					
		List<DBObject> pipeline = Arrays.asList(matchAluno, group);
		AggregationOutput output = alunos.aggregate(pipeline);
		
		List<String> discipline = new ArrayList<String>();
		
		for (DBObject dbo : output.results()) {
//			System.out.println("dbo: " +dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String cod_ativ_curricular = getId.get("nome_ativ_curricular").toString();
			discipline.add(cod_ativ_curricular);
		}
		
		return discipline;
	}
	
	public double calculateJaccard (double sizeA, double sizeB, double sizeAB) {
//		System.out.println(sizeA + " - " + sizeB + " - " + sizeAB + ": " + sizeAB / ((sizeA + sizeB) - sizeAB));
		return sizeAB / ((sizeA + sizeB) - sizeAB);
	}
	
	public double getLimiarJaccardGroup(String forma_saida, String abordagem_disciplinas, 
										int knn) {
//		List<String> reguaGroup = getDisciplinesDistintics(forma_saida);
		List<String> reguaRepresentante = getDisciplinesDistinticsToGroupKnn(forma_saida, abordagem_disciplinas, knn);
//		List<String> reguaGroup = getDisciplinesRestrito(forma_saida);
		List<String> listaDeAlunos = getAlunosFromGroupKnn(forma_saida, abordagem_disciplinas, knn);
		List<Double> listValues = new ArrayList<Double>();
		
		
		for (int i = 0; i < listaDeAlunos.size(); i++) {
			List<String> alunoReguaGroup = getAlunoReguaGroup(forma_saida, listaDeAlunos.get(i));			
			int cont = 0;
			for (int j = 0; j < reguaRepresentante.size(); j++) {
				if (alunoReguaGroup.contains(reguaRepresentante.get(j))) {
					cont++;
				}
			}
			
			listValues.add(calculateJaccard(reguaRepresentante.size(), alunoReguaGroup.size(), cont));
		}
		
		Collections.sort(listValues);
		double limiar = listValues.get(0);
		
		System.out.println("limiar");
		System.out.println(limiar);
		System.out.println("list");
		System.out.println(listValues);
		
		return limiar;
	}
	
	public double getLimiarJaccardGeral(String forma_saida, String id_aluno, String abordagem_disciplinas, int knn) {
//		List<String> reguaGroup = getDisciplinesDistintics(forma_saida);
//		List<String> reguaGroup = getDisciplinesRestrito(forma_saida);
		List<String> reguaRepresentante = getDisciplinesDistinticsToGroupKnn(forma_saida, abordagem_disciplinas, knn);
		List<String> alunoReguaGroup = getAlunoReguaGroup(forma_saida, id_aluno);
		
		int cont = 0;
		for (int j = 0; j < reguaRepresentante.size(); j++) {
			if (alunoReguaGroup.contains(reguaRepresentante.get(j))) {
				cont++;
			}
		}

		double valueJaccard = calculateJaccard(reguaRepresentante.size(), alunoReguaGroup.size(), cont);
		return valueJaccard;
	}
	
	public List<String> setInfoKnn(String forma_saida, String abordagem_disciplinas, int k) {
		DBCollection knn_distances_geral = null;
		
		try {
			knn_distances_geral = MongoConnection.getInstance().getDB().getCollection(
			"knn_distances_geral_"+abordagem_disciplinas+"_"+forma_saida+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		DBObject id = new BasicDBObject();
		id.put("id_aluno", "$id_aluno");
		id.put("distance_to_representante", "$distance_to_representante");
		
		DBObject groupFields = new BasicDBObject();
		groupFields.put("_id", id);
		groupFields.put("quantidade", new BasicDBObject("$sum", 1));
		DBObject group = new BasicDBObject("$group", groupFields);
		
		DBObject sortFields = new BasicDBObject();
		sortFields.put("_id.distance_to_representante", - 1);
		DBObject sort = new BasicDBObject("$sort", sortFields);
					
		List<DBObject> pipeline = Arrays.asList(group, sort);
		AggregationOutput output = knn_distances_geral.aggregate(pipeline);
						
		List<String> listaKAlunosMaisProximos = new ArrayList<String>();		

		
		for (DBObject dbo : output.results()) {
			System.out.println("dbo: " + dbo);
			DBObject getId = (DBObject) dbo.get("_id");
			String id_aluno = getId.get("id_aluno").toString();
			if (listaKAlunosMaisProximos.size() <= k) {
				listaKAlunosMaisProximos.add(id_aluno);
			}
		}
		
		
		BasicDBObject document = new BasicDBObject();
		DBCollection knn_info_group = null;
		
		try {
			knn_info_group = MongoConnection.getInstance().getDB().getCollection(
			"k"+k+"_info_group_"+abordagem_disciplinas+"_"+forma_saida+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0; i < listaKAlunosMaisProximos.size(); i++) {
			String id_aluno = listaKAlunosMaisProximos.get(i);				
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
//					knn_info_group.save(document);	
				}
		}
		
//		list_excluidos_perfil = listaAlunosForaConjunto;
//		
//		System.out.println("LISTA DE EXCLUIDOS DO PERFIL");
//		System.out.println(listaAlunosForaConjunto);
//		System.out.println("LISTA DE ALUNOS DO CONJUNTO");
//		System.out.println(listaAlunosConjunto);
		
		return listaKAlunosMaisProximos;

	}
	
	public void calculateDistanceGroupToRepresentante(String forma_saida, String id_representante, String abordagem_disciplinas) {
		List<List<String>> listDistance = new ArrayList<List<String>>();
		List<String> listAlunos = getAlunos(forma_saida);
		List<Double> listAllDistance = new ArrayList<Double>();
		
		BasicDBObject document = new BasicDBObject();
		DBCollection knn_distances_geral = null;
		
//		System.out.println("alunosss");
//		System.out.println(listAlunos.size());
		
		try {
			knn_distances_geral = MongoConnection.getInstance().getDB().getCollection(
			"knn_distances_geral_"+abordagem_disciplinas+"_"+forma_saida+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
			for (int i = 0; i < listAlunos.size(); i++) {
					List<String> listInfo = new ArrayList<String>();
					List<List<String>> vectorCoursesAlunoRepresentante = getDisciplines(forma_saida, id_representante, abordagem_disciplinas);
					List<List<String>> vectorCoursesAluno = getDisciplines(forma_saida, listAlunos.get(i), abordagem_disciplinas);
					document = new BasicDBObject();
					
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
					
					document.put("id_aluno", listAlunos.get(i).toString());
					document.put("distance_to_representante", distance);
					
//					knn_distances_geral.save(document);	
			}
		
		Collections.sort(listAllDistance);
				
		System.out.println(" ");
		System.out.println("todas as distancias");
		System.out.println(listAllDistance);
	}
	
	public String getRepresentante(String forma_saida, String abordagem_disciplinas) {
		List<String> listAlunos = getAlunos(forma_saida);
		
		List<List<String>> list = new ArrayList<List<String>>();

		double menorDistancia = 2;
		String id_aluno_menor_distancia = "";
		
//		System.out.println("teste:");
//		List<List<String>> teste = getDisciplines(forma_saida, "0fb59b53b6701bb7e419ff0d5f634da0");
//		System.out.println(teste);
//		
		for (int i = 0; i < listAlunos.size(); i++) {
			double total_media = 0.0;
			List<String> listInside = new ArrayList<String>();
			List<List<String>> vectorDisciplinesAlunoA = getDisciplines(forma_saida, listAlunos.get(i), abordagem_disciplinas);
			
//			System.out.println("alunoA: " + vectorDisciplinesAlunoA);

			for (int j = 0; j < listAlunos.size(); j++) {
				List<List<String>> vectorDisciplinesAlunoB = getDisciplines(forma_saida, listAlunos.get(j), abordagem_disciplinas);
				
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
	
	
	public String setRepresentanteArtificial(String forma_saida, String abordagem_disciplinas, int knn) {
		BasicDBObject document = new BasicDBObject();
		DBCollection knn_info_group = null;
		
		try {
			knn_info_group = MongoConnection.getInstance().getDB().getCollection(
			"k"+knn+"_info_group_"+abordagem_disciplinas+"_"+forma_saida+"_"+current_curso.toLowerCase().trim().replace(" ", "_"));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		List<String> listDisciplines = getDisciplinesDistinticsToGroupKnn(forma_saida, abordagem_disciplinas, knn);
		List<List<String>> list = new ArrayList<List<String>>();
//		
		for (int i = 0; i < listDisciplines.size(); i++) {
			document = new BasicDBObject();
			double total_media = 0.0;
			List<List<String>> mediaDisciplinas = getAverageDisciplinesFromGroupKnn(forma_saida, listDisciplines.get(i), abordagem_disciplinas, knn);
//			List<String> listInfo = vectorDisciplinesAlunoA.get(0)
			System.out.println("listInfo");
			System.out.println(mediaDisciplinas);
			
			document.put("id_aluno", "representante"+knn);
			document.put("nome_ativ_curricular", mediaDisciplinas.get(0).get(0));
			document.put("media_final", mediaDisciplinas.get(0).get(1));
			document.put("periodos", mediaDisciplinas.get(0).get(2));
			document.put("periodo", mediaDisciplinas.get(0).get(3));

//			knn_info_group.save(document);	
		}
		return id_aluno_representante;
	}
	
	public List<String> getAlunosGeral(String forma_saida){
		DBObject match = new BasicDBObject("$match", new BasicDBObject("forma_saida",new BasicDBObject("$ne", "formado")));

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
		int cont = 1;
		for (DBObject dbo : output.results()) {
			DBObject getId = (DBObject) dbo.get("_id");
			String id_aluno = getId.get("id_aluno").toString();
			if (listAlunos.contains(id_aluno) == false) {
				list.add(id_aluno);
			}
			cont++;
		}
		return list;
	}	
	
	public double getDistanciaMediana(String forma_saida, String id_representante, String abordagem_disciplinas, int knn) {
		List<List<String>> listDistance = new ArrayList<List<String>>();
		List<String> listAlunos = getAlunosFromGroupKnn(forma_saida, abordagem_disciplinas, knn);
		List<Double> listAllDistance = new ArrayList<Double>();
		
//		System.out.println("list aluunos: " + listAlunos.size());

			double medianaDistancia;
			
			for (int i = 0; i < listAlunos.size(); i++) {
					List<String> listInfo = new ArrayList<String>();
					List<List<String>> vectorCoursesAlunoRepresentante = getDisciplinesFromGroupKnn(forma_saida, id_representante, abordagem_disciplinas, knn);
					List<List<String>> vectorCoursesAluno = getDisciplinesFromGroupKnn(forma_saida, listAlunos.get(i), abordagem_disciplinas, knn);
					
//					System.out.println("vectorCoursesAlunoRepresentante: " + vectorCoursesAlunoRepresentante);
//					System.out.println("vectorCoursesAluno: " + vectorCoursesAluno);
					double similaridade = 0.0;
					if (!id_representante.equals(listAlunos.get(i).toString())) {
						for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
							double nota_final = 0.0;
							
							double notaA = Double.parseDouble(vectorCoursesAlunoRepresentante.get(k).get(0));
							int ano_periodoA = Integer.parseInt(vectorCoursesAlunoRepresentante.get(k).get(1));
							String periodoA = vectorCoursesAlunoRepresentante.get(k).get(2);
							
							
							double notaB = Double.parseDouble(vectorCoursesAluno.get(k).get(0));
							int ano_ingressoB = Integer.parseInt(vectorCoursesAluno.get(k).get(2));
							int anoB = Integer.parseInt(vectorCoursesAluno.get(k).get(1));
							String periodoB = vectorCoursesAluno.get(k).get(3);
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
//		System.out.println("lista de distancias");
//		System.out.println(listAllDistance);
		
		medianaDistancia = listAllDistance.get(0);
//		medianaDistancia = listAllDistance.get(listAllDistance.size()-1);
		
		System.out.println(" ");
//		System.out.println(listAllDistance);
		System.out.println("distancia mediana ");
		System.out.println(medianaDistancia);
		
		return medianaDistancia;
	}
	
//	public List<List<String>> getResultadosExperimentosDistanceDoPerfil(String forma_saida, String id_representante, double distMax, double limiarJaccard, String abordagem_disciplinas) {
//		List<List<String>> listDistancesAprovados = new ArrayList<List<String>>();
//		List<List<String>> listDistancesReprovados = new ArrayList<List<String>>();
//		List<String> listAlunos = list_excluidos_perfil;
//		
//		List <Double> listaDistanciasGeral = new ArrayList<Double>();
//		
//		for (int i = 0; i < listAlunos.size(); i++) {
//			List<String> listInfo = new ArrayList<String>();
//			List<List<String>> vectorCoursesAlunoRepresentante = new ArrayList<List<String>>();
//			
//			vectorCoursesAlunoRepresentante = getDisciplines(forma_saida, id_representante, abordagem_disciplinas);
//			
//			double similaridade = 0.0;
//			double valueJaccard = getLimiarJaccardGeral(forma_saida, listAlunos.get(i), id_representante);
//			
//		
//			if (valueJaccard >= limiarJaccard) {				
////				List<List<String>> vectorCoursesAluno = getDisciplinesGeral(forma_saida, listAlunos.get(i), abordagem_disciplinas);
//				
////				System.out.println("vectorCoursesAlunoRepresentante: " + vectorCoursesAlunoRepresentante);
////				System.out.println(" ");
////				System.out.println("vectorCoursesAluno: " + vectorCoursesAluno);
////				System.out.println("--");
//
//				if (!id_representante.equals(listAlunos.get(i).toString())) {
//					for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
//						double nota_final = 0.0;
//						int ano_ingressoA = Integer.parseInt(vectorCoursesAlunoRepresentante.get(k).get(0));
//						int anoA = Integer.parseInt(vectorCoursesAlunoRepresentante.get(k).get(1));
//						String periodoA = vectorCoursesAlunoRepresentante.get(k).get(2);
//						double notaA = Double.parseDouble(vectorCoursesAlunoRepresentante.get(k).get(3));
//						int ano_periodoA = anoA - ano_ingressoA;
//						
//						int ano_ingressoB = Integer.parseInt(vectorCoursesAluno.get(k).get(0));
//						int anoB = Integer.parseInt(vectorCoursesAluno.get(k).get(1));
//						String periodoB = vectorCoursesAluno.get(k).get(2);
//						double notaB = Double.parseDouble(vectorCoursesAluno.get(k).get(3));
//						int ano_periodoB = anoB - ano_ingressoB;
//						
//						if (ano_periodoA == ano_periodoB && periodoA.equals(periodoB) && !periodoA.equals("") && !periodoB.equals("")) {
//							nota_final =  (Math.abs(notaA - notaB))/10;
//						}
//
//						similaridade = similaridade + nota_final;
//					}
//				}
//			}
//				similaridade = similaridade/vectorCoursesAlunoRepresentante.size();
//				double distance = 1 - similaridade;
//				listInfo.add(listAlunos.get(i).toString());
//				listInfo.add(String.valueOf(distance));
//				
//				listaDistanciasGeral.add(distance);
//				
////				System.out.println("distance: " + distance);
////				System.out.println(" ");
//				
//				if (distance < distMax) {
//					listDistancesAprovados.add(listInfo);
//				} else {
//					listDistancesReprovados.add(listInfo);
//				}
//		}
//		
//		Collections.sort(listaDistanciasGeral);
//		System.out.println("tamanho da lista dos alunos excluídos do conjunto e classificados como pertences ao perfil");
//		System.out.println(listDistancesAprovados.size());
//		System.out.println("tamanho da lista dos alunos excluídos do conjunto e classificados como não pertences ao perfil");
//		System.out.println(listDistancesReprovados.size());
//		System.out.println(" ");
//		System.out.println("lista geral");
//		System.out.println(listaDistanciasGeral);
//		System.out.println("");
//		return listDistancesAprovados;
//	}
	
	public List<List<String>> getResultadosExperimentosDistanceGeral(String forma_saida, String id_representante, 
							double distMax, double limiarJaccard, String abordagem_disciplinas, int knn) {
		List<List<String>> listDistancesAprovados = new ArrayList<List<String>>();
		List<List<String>> listDistancesReprovados = new ArrayList<List<String>>();
		List<String> listAlunos = getAlunosGeral(forma_saida);
		
		List <Double> listaDistanciasGeral = new ArrayList<Double>();
		int cont = 0;
		for (int i = 0; i < listAlunos.size(); i++) {
			List<String> listInfo = new ArrayList<String>();
			List<List<String>> vectorCoursesAlunoRepresentante = new ArrayList<List<String>>();
			
			double similaridade = 0.0;
			double valueJaccard = getLimiarJaccardGeral(forma_saida, listAlunos.get(i), abordagem_disciplinas, knn);
			
			
			if (valueJaccard >= limiarJaccard) {
				cont++;
//				System.out.println("entrou");
				vectorCoursesAlunoRepresentante = getDisciplinesFromGroupKnn(forma_saida, id_representante, abordagem_disciplinas, knn);
				List<List<String>> vectorCoursesAluno = getDisciplinesGeral(forma_saida, listAlunos.get(i), abordagem_disciplinas, knn);
				
//				System.out.println("vectorCoursesAlunoRepresentante: " + vectorCoursesAlunoRepresentante.size());
//				System.out.println("vectorCoursesAlunoRepresentante: " + vectorCoursesAlunoRepresentante.size());
//				System.out.println(" ");
//				System.out.println("vectorCoursesAluno: " + vectorCoursesAluno.size());
//				System.out.println("--");

				if (!id_representante.equals(listAlunos.get(i).toString())) {
					for (int k = 0; k < vectorCoursesAlunoRepresentante.size(); k++) {
						double nota_final = 0.0;
						double notaA = Double.parseDouble(vectorCoursesAlunoRepresentante.get(k).get(0));
						int ano_periodoA = Integer.parseInt(vectorCoursesAlunoRepresentante.get(k).get(1));
						String periodoA = vectorCoursesAlunoRepresentante.get(k).get(2);
						
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
			}
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
		System.out.println(" ");
		System.out.println("lista com info");
		System.out.println(listDistancesAprovados);
		System.out.println("tamanho da lista dos alunos em geral classificados como não pertences ao perfil");
		System.out.println(listDistancesReprovados.size());
		System.out.println(" ");
		System.out.println("outra lista com info");
		System.out.println(listDistancesReprovados);
		System.out.println("lista geral");
		System.out.println(listaDistanciasGeral);
		System.out.println(" ");
//		System.out.println(cont);
		return listDistancesAprovados;
	}
	
	public static void main(String args[]) {
		AnaliseDeEvasaoKnn ativ= new AnaliseDeEvasaoKnn("ciencia da computacao");
		
		System.out.println("KNN evasao");
		
		// FORMA O CONJUNTO DE ALUNOS QUE ESTARÃO NO CONJUNTO DO PERFIL SELECIONADO DE ACORDO COM A PORCENTAGEM REQUERIDA
		ativ.filterByEvasao("evasao", 1);
		
//		System.out.println(ativ.getDisciplinesDistintics("evasao").size());
//		System.out.println(ativ.getAlunos("evasao").size());
		
//		ativ.setDisciplinesRestrito("formado");
		
//		System.out.println(ativ.getDisciplinesRestrito("formado").size());
						
		// ENCONTRA O REPRESENTANTE DO CONJUNTO FORMADO DENTRO DAQUELE TIPO DE EVASAO REQUERIDO, ATRAVÉS
		// DE UM CALCULO DE MÉDIA DAS DISTÂNCIAS DE CADA ALUNO DENTRO DO CONJUNTO PARA O RESTANTE DOS ALUNOS DO CONJUNTO
		String representanteReal = ativ.getRepresentante("evasao", "flex");
		
//		ativ.calculateDistanceGroupToRepresentante("evasao", representanteReal, "restrito");
		
//		System.out.println(ativ.setInfoKnn("evasao", "restrito", 5));
		
//		System.out.println(ativ.getDisciplinesDistinticsToGroupKnn("evasao", "restrito", 5).size());
		
//		ativ.setRepresentanteArtificial("evasao", "restrito", 5);
		
		double limiarJaccard = ativ.getLimiarJaccardGroup("evasao", "flex", 5);
		
		// CALCULA A DISTANCIA MAXIMA DENTRO DO CONJUNTO, ATRAVÉS DE UM CALCULO PARA DESCOBRIR QUAL O VALOR DA DISTANCIA
		// DO ALUNO MAIS DISTANTE DO REPRESENTANTE DE DENTRO DO CONJUNTO
		double distMax = ativ.getDistanciaMediana("evasao", "representante"+5,"flex", 5);
		
//		System.out.println(" ");
		
		// VERIFICA QUANTIDADE DE ALUNOS DE DENTRO DO PERFIL PORÉM QUE NÃO ENTRARAM NO CONJUNTO E QUE FORAM CLASSIFICADOS
		// PELO ALGORITMO COMO PERTENCENTES AO PERFIL 
//		ativ.getResultadosExperimentosDistanceDoPerfil("formado", representante , distMax, limiarJaccard);
		
		// VERIFICA QUANTIDADE DE ALUNOS FORA DO PERFIL (RESTO DO MUNDO) QUE FORAM CLASSIFICADOS
		// PELO ALGORITMO COMO PERTENCENTES AO PERFIL
		ativ.getResultadosExperimentosDistanceGeral("evasao", "representante"+5, distMax, limiarJaccard, "flex", 5);
		
		System.out.println("primeiro vetor de disciplinas");
		System.out.println(ativ.getDisciplinesDistintics("evasao"));
		System.out.println(" ");
		System.out.println("disciplinas group knn");
		System.out.println(ativ.getDisciplinesDistinticsToGroupKnn("evasao", "flex", 5));
	}
}

