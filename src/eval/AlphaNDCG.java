package eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class AlphaNDCG {
	private AlphaNDCG () {}
	/* *
	@param k : cut-off for calculation
	of Alpha - NDCG@k
	@param alpha : alpha in Alpha - NDCG@k
	@param retrieved_list : list of
	documents , the highest - ranking
	document first .
	@param qrel_list : a collection 
	of labeled document ids from qrel .
	@return the Alpha - NDCG for the given data
	*/
	public static double compute (int k , double alpha, ArrayList < Result > retrieved_list, ArrayList < Qrel > qrel_list){
				
		return computeDCG(qrel_list,retrieved_list, k,alpha)/computeIDCG(qrel_list,retrieved_list, k,alpha);		
		
	}
	
	static double computeDCG (ArrayList < Qrel > qrel_list , ArrayList < Result > retrieved_list, int k,double a) { 
		
		double sum_nominator=0.0,sum_total=0.0;
				
		Set <Integer> temp = new TreeSet<Integer>(); 
		for(int counter=0;counter<qrel_list.size();counter++){
			temp.add(qrel_list.get(counter).getSubtopic_no()); // this is all the subtopics 
		}
		
		for(int j=0;j<k;j++){ 
			sum_nominator = 0.0;
			for(int i:temp){
				sum_nominator += computeJ(qrel_list,retrieved_list.get(i).getDocument_id(),i)*Math.pow(1-a, computeRij(qrel_list,retrieved_list,i,j));
			}
			sum_total += sum_nominator/( Math.log(2+j)/Math.log(2));  
		}
		
		return sum_total;
		
	}
	
	
	static int computeJ (ArrayList < Qrel > qrel_list , String dkID ,int i) { 
		
		
		if(qrel_list.get(i).getDocument_id()==dkID && qrel_list.get(i).getSubtopic_no()==i){
			if(qrel_list.get(i).getJudgment()>=1){
				return 1;
			}else{
				return 0;
			}
		}
		return 0;
	}
	
	static double computeRij (ArrayList < Qrel > qrel_list , ArrayList < Result > retrieved_list ,int i, int j) { 
		
		double sum = 0.0;
		String dkID;
		
		for(int jey=0;jey<j;jey++){ 	
			dkID = retrieved_list.get(jey).getDocument_id();
			sum += computeJ(qrel_list,dkID,i);			
		}
		return sum;
	}
	
	
	
	static double computeIDCG (ArrayList < Qrel > qrel_list, ArrayList < Result > retrieved_list,int k, double a) { 
		

		
		double sum_nominator=0.0,sum = 0.0;
		
		Set <Integer> temp = new TreeSet<Integer>(); 
		for(int counter=0;counter<qrel_list.size();counter++){
			temp.add(qrel_list.get(counter).getSubtopic_no()); // this is all the subtopics 
		}
		
		List<Double> nomin_list = new ArrayList<Double>();
		List<Double> denom_list = new ArrayList<Double>();
	
		
		
		for(int j=0;j<k;j++){ 
			sum_nominator = 0.0;
			for(int i:temp){
				sum_nominator += computeJ(qrel_list,retrieved_list.get(i).getDocument_id(),i)*Math.pow(1-a, computeRij(qrel_list,retrieved_list,i,j));
			}
			nomin_list.add(sum_nominator);
			denom_list.add(sum_nominator/( Math.log(2+j)/Math.log(2)));  
		}
		
		
		Collections.sort(nomin_list); 
		Collections.reverse(denom_list); 

		
		for(int counter=0;counter<qrel_list.size();counter++){sum += nomin_list.get(counter)/denom_list.get(counter);}
		 
		
		return sum;		
	}


	public static void main(String[] args) throws IOException {
		ArrayList<Qrel> qrels = new ArrayList<Qrel>();
		ArrayList<Result> resultsMMR = new ArrayList<Result>();
		ArrayList<Result> resultsPor = new ArrayList<Result>();
		Map<Integer, ArrayList<Qrel>> qrelMap = new TreeMap<Integer, ArrayList<Qrel>>();
		Map<Integer, ArrayList<Result>> resMMRMap = new TreeMap<Integer, ArrayList<Result>>();
		Map<Integer, ArrayList<Result>> resPorMap = new TreeMap<Integer, ArrayList<Result>>();
		File qrelFile = new File("qrels.ndeval.txt");
		File resMMRFile = new File("MMR_TF_IDF_0_25.txt");
		File resPorFile = new File("Port_TF_IDF_b4.txt"); 	
		BufferedReader qrelbr = new BufferedReader(new FileReader(qrelFile));
		BufferedReader resMMRbr = new BufferedReader(new FileReader(resMMRFile));
		BufferedReader resPorbr = new BufferedReader(new FileReader(resPorFile));
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
		while ((line = resMMRbr.readLine()) != null) {
			
			String tokens[] = line.split(" ");
			int topic = Integer.parseInt(tokens[0]);
			
			if (check != topic) {
				resMMRMap.put(check, resultsMMR);
				check = topic;
				resultsMMR = new ArrayList<Result>();
			}
			// because I need 4 columns I just repeat the docID on column 2, and column 3 I just leave it black (just put 1) 
			resultsMMR.add(new Result(tokens[1].replace("../irdm_data/clueweb12/", "").replace(".txt", ""), Integer.parseInt("1"), Double.parseDouble(tokens[2])));
		}
		resMMRMap.put(check, resultsMMR);
		resMMRbr.close();		
		
		check = 201;
		while ((line = resPorbr.readLine()) != null) {
			
			String tokens[] = line.split("\t");
			// IMPORTANT !!!!! IF YOU ARE RUNNING FOR ALL TOPICS AND NOT JUST TOPIC ONE REPLACE "1" WITH tokens[0]!!! 
			int topic = Integer.parseInt("1");  
			
			if (check != topic) {
				resPorMap.put(check, resultsPor);
				check = topic;
				resultsPor = new ArrayList<Result>();
			}
			// because I need 4 columns, similar as above, I place 1's on column 1 and 3 and column 2 has docID and 4 its score. 
			resultsPor.add(new Result(tokens[0].replace("C:\\Users\\Maly\\Documents\\IRDA_ICA1\\clueweb12\\", "").replace(".txt", ""), Integer.parseInt("1"), Double.parseDouble(tokens[1])));
		}
		resPorMap.put(check, resultsPor);
		resPorbr.close();		
		
		Map<Integer, Double> mapMMR = new TreeMap<Integer, Double>();
		Map<Integer, Double> mapPor = new TreeMap<Integer, Double>();
		
		double sumMMR=0.0,sumPor=0.0;
		
		int[] K = {1, 5, 10, 20, 30, 40, 50};
		Double[] A = {0.1,0.5,0.9};
		
		PrintWriter printoutMMR = new PrintWriter("mmr_ndcg.txt", "UTF-8");
		PrintWriter printoutPor = new PrintWriter("portfolio_ndcg.txt", "UTF-8");
		printoutMMR.println("aNDCG for MMR with TF-IDF using lambda = 0.25");
		printoutPor.println("aNDCG for Portfolio with TF-IDF using beta=4 ");
		printoutMMR.println("a \t | \t K \t | \t NDCG@K");
		printoutPor.println("a \t | \t K \t | \t NDCG@K");
		
		for(double counterA:A){			
			for(int counterK:K){

				sumMMR = 0.0; sumPor=0.0;	
				
				for (int countertopic: resMMRMap.keySet()) {sumMMR += compute(counterK, counterA,resMMRMap.get(countertopic), qrelMap.get(countertopic));}			
				mapMMR.put(counterK, sumMMR/resMMRMap.size());  
					
				for (int countertopic: resPorMap.keySet()) {sumPor += compute(counterK, counterA,resPorMap.get(countertopic), qrelMap.get(countertopic));}			
				mapPor.put(counterK, sumPor/resPorMap.size());  
				
			}
			

			for (int counter: mapMMR.keySet()) {printoutMMR.println(A+ " \t | \t " + counter + " \t | \t " + String.format("%.3f", mapMMR.get(counter)) + "\n");}
			for (int counter: mapPor.keySet()) {printoutPor.println(A+ " \t | \t " + counter + " \t | \t " + String.format("%.3f", mapPor.get(counter)) + "\n");}
		
			
		}
		
		printoutMMR.close();
		printoutPor.close();

	}
		
}
