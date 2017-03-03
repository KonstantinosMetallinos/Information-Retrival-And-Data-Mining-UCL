package eval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.TreeMap;

import org.terrier.applications.batchquerying.TRECQuery;

import utils.SimpleScoring;
import utils.Utils;
import utils.Utils.QREL;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;



public class NDCG {

	private NDCG () {}

	/* *
	@param retrieved_list : list of documents, 
	the highest - ranking document first .
	@param qrel_list : a collection of labelled 
	document ids from qrel .
	@param k : cut - off for calculation of NDCG@k
	@return the NDCG for given data
	*/
	public static double compute (ArrayList <Result> retrieved_list, ArrayList <Qrel> qrel_list , int k) {
		
		return computeDCG(retrieved_list,qrel_list,k)/computeIDCG(qrel_list,k);    
		
	}
	
	
	static double computeIDCG (ArrayList <Qrel> qrel_list ,	int k) {
		Sorting sor = new Sorting();
		Collections.sort(qrel_list, sor);
		
		double sum = qrel_list.get(0).getJudgment();		
													
		for(int counter=1;counter<k;counter++){
			sum+= qrel_list.get(counter).getJudgment()/(Math.log(counter+1)/Math.log(2));		
		}					
				
		return sum;		
	}
	
	static double computeDCG (ArrayList <Result> retrieved_list, ArrayList <Qrel> qrel_list, int k) {
		
		double sum=0;
		String godvalue = retrieved_list.get(0).getDocument_id();
		godvalue = godvalue.replace("../irdm_data/clueweb12/", "");


		// testing
		for(int counter=0;counter<qrel_list.size();counter++){
			if(qrel_list.get(counter).getDocument_id().equals(godvalue)){
				if(qrel_list.get(counter).getJudgment()>0){sum += qrel_list.get(counter).getJudgment();}
			}
		}	
	
		
		if(k > 1){			
			for(int counter=1;counter<k;counter++){
				godvalue = retrieved_list.get(counter).getDocument_id();
				godvalue = godvalue.replace("../irdm_data/clueweb12/", "");
				for(int counter2=1;counter2<qrel_list.size();counter2++)	{
					if(qrel_list.get(counter2).getDocument_id().equals(godvalue)){
						if(qrel_list.get(counter2).getJudgment()>0){sum += qrel_list.get(counter2).getJudgment()/(Math.log(counter+1)/Math.log(2));}
					}
				}									
			}					
		}

		return sum;			

	}
	
	
	private static class Sorting implements Comparator<Qrel>{  // credits: http://beginnersbook.com/2013/12/java-arraylist-of-object-sort-example-comparable-and-comparator/

		@Override
		public int compare(Qrel arg0, Qrel arg1) {

			   int sort1 = arg0.getJudgment();
			   int sort2 = arg1.getJudgment();

			   /*For ascending order*/
			   return sort2-sort1;
			
		}
		
	}	

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
			
			results.add(new Result(tokens[2].replace("../irdm_data/clueweb12/", "").replace(".txt", ""), Integer.parseInt(tokens[3]), Double.parseDouble(tokens[4])));
		}
		resMap.put(check, results);
		resbr.close();		
		
		int[] K = {1, 5, 10, 20, 30, 40, 50};
		Map<Integer, Double> map = new TreeMap<Integer, Double>();
		
		for (int counterK: K) {
			
			double sum = 0.0;			
			for (int countertopic: resMap.keySet()) {sum += compute(resMap.get(countertopic), qrelMap.get(countertopic), counterK);}			
			map.put(counterK, sum/resMap.size());  
			
		}
		
		PrintWriter printout = new PrintWriter("bm25_ndcg.txt", "UTF-8");
		
		printout.println("bm25");
		printout.println("K \t | \t NDCG@K");
		for (int counter: map.keySet()) {printout.println(counter + " \t | \t " + String.format("%.3f", map.get(counter)) + "\n");}
		printout.close();
	}
	
	
}
