import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Scanner;
import javax.xml.bind.DatatypeConverter;

public class cachesim {

	public static int pos;
	public static String type;
	public static String[] mm = new String[16777216];

	public static void main(String[] args){
		// System.out.println("ask for input");
		// Scanner cLine = new Scanner(System.in);
		// String file = cLine.next();
		// int csize = cLine.nextInt()*1024;
		// int as= cLine.nextInt();
		// type = cLine.next();
		// int bsize = cLine.nextInt();
		// cLine.close();
				String file = args[0];
				int csize = Integer.parseInt(args[1])*1024;
				int as= Integer.parseInt(args[2]);
				type = args[3];
				int bsize = Integer.parseInt(args[4]);
				for(int i = 0;i< mm.length;i++){
					mm[i]="00";
				}

		int S = csize/(bsize*as);
		int bolength = (int) (Math.log((double)bsize)/Math.log(2));
		int inlength = (int) (Math.log(S)/Math.log(2));
		int taglength = 24-inlength-bolength;

		//System.out.println(as);
		//System.out.println(csize);
		//System.out.println(bsize);
		//System.out.println(S);
		@SuppressWarnings("unchecked")
		LinkedList<entry>[] cache = new LinkedList[S];
		for(int i=0;i<S;i++){
			cache[i]= new LinkedList<entry>();
		}
		//read file
		Scanner info = null;
		try {
			info = new Scanner(new File(file));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		while(info.hasNext()){
			byte[] val;
			String sl = info.next();
			System.out.print(sl);
			String hexprint = info.next();
			System.out.print(" " + hexprint);
			String hex = hexprint.substring(2);
			int memloc = Integer.parseInt(hex,16);
			String address = Integer.toBinaryString(memloc);
			//pad with zeroes
			StringBuilder sb = new StringBuilder();
			for (int toPrepend=24-address.length(); toPrepend>0; toPrepend--) {
				sb.append('0');
			}

			sb.append(address);
			address = sb.toString();
			//designate what each part of the address means
			int set = 0;

			String tag = address.substring(0,taglength);
			if(inlength==0){
				set = 0;
			}
			else{
				String index = address.substring(taglength,taglength+inlength);
				set = Integer.parseInt(index,2);
			}
			String blockoffset = address.substring(taglength+inlength);
			int bo= Integer.parseInt(blockoffset,2);
			int asize = info.nextInt();
			if(sl.equals("store")){
				String hex2= info.next();
				String[] hexval = new String[(hex2.length()/2)];
				int i = 0;
				for(int k = 0;i<hex2.length()/2;k=k+2){
					hexval[i]=hex2.substring(k, k+2);
					i++;
				}
				store(hexval,memloc,tag,set,bo,asize,cache, as, address, bsize);
			}
			if(sl.equals("load")){
				load(memloc,tag,set,bo,asize,cache,address,bsize,as, "print");
			}
		}
	}

	public static void store(String[] val, int memloc, String tag, int set, int bo,int asize,LinkedList<entry>[] cache, int as, String address, int bsize){
		//check if it is in cache
		int hm = find(set, tag, cache);
		if(hm==-1){
			System.out.println(" miss");
			if(type.equals("wt")){
				writemem(memloc,val,asize);
			}
			if(type.equals("wb")){
				cache=load(memloc,tag,set,bo,asize,cache,address,bsize,as,"noprint");
				int newhm = find(set, tag, cache);
				for(int j = 0;j<asize;j++){
					cache[set].get(newhm).data[bo+j]=val[j];
				}
				cache[set].get(newhm).dirty=1;
			}
		}
		else{
			//if the block is already in the cache
			System.out.println(" hit");
			for(int j = 0;j<asize;j++){
				cache[set].get(hm).data[bo+j]=val[j];
				if(type.equals("wb")){
					cache[set].get(hm).dirty=1;
				}
				if(type.equals("wt)")){
					writemem(memloc,val,asize);
				}
			}
			//removes the block then re inserts it in the front to make the last the LRU
			entry MRU = cache[set].remove(hm);
			cache[set].addFirst(MRU);
		}
	}
	public static void writemem(int memloc, String[] val, int asize){
		for(int k = 0;k<asize;k++){
			mm[memloc+k]=val[k];
		}
	}

	public static LinkedList<entry>[] load(int memloc, String tag, int set, int bo,int asize,LinkedList<entry>[] cache, String address, int bsize, int as, String print){
		int hm = find(set, tag, cache);
		if(hm==-1){
			if(print.equals("print")){
				System.out.print(" miss");
			}
			if(cache[set].size()==as){
				entry LRU = cache[set].removeLast();
				//System.out.print(" tag evicted " + LRU.tag + " ");
				
				//write dirty block to memory if wb
				if(type.equals("wb")){
					if(LRU.dirty==1){
						for(int l = 0;l<LRU.data.length;l++){
							mm[LRU.memloc-LRU.bo+l]=LRU.data[l];
						}
					}
				}
			}
			//get the data from memory
			String[] loadval = new String[asize];
			String[] memval = new String[bsize];
			for(int l = 0;l<bsize;l++){
				memval[l]=mm[memloc-bo+l];
			}
			for(int k = 0; k<asize;k++){
				loadval[k] = memval[bo+k];
			}
			StringBuffer printval = new StringBuffer();
			for(int k = 0; k<loadval.length;k++){
				printval.append(loadval[k]);
			}
			if(print.equals("print")){
				System.out.println(" " + printval);
			}
			entry newBlock = new entry(memval,tag,memloc,bo);
			cache[set].addFirst(newBlock);
		}
		else{
			if(print.equals("print")){
				System.out.print(" hit");

			}
			String[] loadval = new String[asize];
			for(int j = 0;j<asize;j++){
				loadval[j]=cache[set].get(hm).data[bo+j];
			}
			entry MRU = cache[set].remove(hm);
			cache[set].addFirst(MRU);
			StringBuffer printval = new StringBuffer();
			for(int k = 0; k<loadval.length;k++){
				printval.append(loadval[k]);
			}
			System.out.println(" " + printval);
		}
		return cache;
	}

	public static int find(int set, String tag, LinkedList<entry>[] cache){
		if(cache[set].isEmpty()){
			return -1;
		}

		for(int i = 0;i<cache[set].size();i++){
			entry curr = cache[set].get(i);
			if(curr.valid==1){
				if(curr.tag.equals(tag)){
					return i;
				}
			}
		}
		return -1;
	}
}

