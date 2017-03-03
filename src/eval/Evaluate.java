package eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

public class Evaluate {

	public static void main(String[] args) throws IOException {
		ArrayList<Qrel> qrels = new ArrayList<Qrel>();
		ArrayList<Result> results = new ArrayList<Result>();
		Map<Integer, ArrayList<Qrel>> qrelMap = new TreeMap<Integer, ArrayList<Qrel>>();
		Map<Integer, ArrayList<Result>> resMap = new TreeMap<Integer, ArrayList<Result>>();
		File qrelFile = new File("qrels.adhoc.txt");
		File resFile = new File("BM25b0.75_0.res");
		BufferedReader qrelbr = new BufferedReader(new FileReader(qrelFile));
		BufferedReader resbr = new BufferedReader(new FileReader(resFile));
		String line;
		int check = 201;
		while ((line = qrelbr.readLine()) != null) {
			String tokens[] = line.split(" ");
			int topic = Integer.parseInt(tokens[0]);
			if (check != topic) {
				qrelMap.put(check, qrels);
				check = topic;
				qrels = new ArrayList<Qrel>();
			}
			if (tokens[3].contains("-")) {
				tokens[3] = "0";
			}
			qrels.add(new Qrel(tokens[0], tokens[2], tokens[3]));
		}
		qrelMap.put(check, qrels);
		qrelbr.close();
		
		check = 201;
		while ((line = resbr.readLine()) != null) {
			String tokens[] = line.split(" ");
			int topic = Integer.parseInt(tokens[0]);
			if (check != topic) {
				resMap.put(check, results);
				check = topic;
				results = new ArrayList<Result>();
			}
			results.add(new Result(tokens[2].replace("../clueweb12/", "").replace(".txt", ""), Integer.parseInt(tokens[3]), Double.parseDouble(tokens[4])));
		}
		resMap.put(check, results);
		resbr.close();		
		
		int[] K = {1, 5, 10, 20, 30, 40, 50};
		Map<Integer, Double> map = new TreeMap<Integer, Double>();
		for (int i: K) {
			double NDCGk = 0.0;
			for (int topic: resMap.keySet()) {
				NDCGk += NDCG.compute(resMap.get(topic), qrelMap.get(topic), i);
			}
			map.put(i, NDCGk/resMap.size());
		}
//		for (int topic: resMap.keySet()) {
////			ArrayList<Result> list = resMap.get(topic);
////			for (Result result: list) {
////				System.out.println("result " + result.getDocumentId() + " " + result.getDocumentRank() + " " + result.getScore());
////			}
////			ArrayList<Qrel> qlist = qrelMap.get(topic);
////			for (Qrel rel: qlist) {
////				if (topic!=rel.getTopic()) {
////					System.out.println(topic + " rel " + rel.getDocumentId() + " " + rel.getJudgment() + " " + rel.getTopic());
////				}
////			}
//			
//			for (int i: K) {
//				if (map.containsKey(i)) {
//					map.put(i, map.get(i) + NDCG.compute(resMap.get(topic), qrelMap.get(topic), i));
//				}
//				else {
//					map.put(i, NDCG.compute(resMap.get(topic), qrelMap.get(topic), i));
//				}
//			}
//		}
//		for (int k: map.keySet()) {
//			map.put(k,  map.get(k)/resMap.size());
//		}
		
		PrintWriter writer = new PrintWriter("bm25_ndcg.txt", "UTF-8");
		writer.println("BM25");
		writer.println("K\t|\tNDCG@K");
		for (int k: map.keySet()) {
			writer.print(k + "\t|\t" + String.format("%.3f", map.get(k)) + "\n");
		}
		writer.close();
	}

}
